package de.erethon.hecate.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.erethon.hecate.Hecate;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import io.papermc.paper.event.connection.configuration.PlayerConnectionInitialConfigureEvent;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.metadata.FixedMetadataValue;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Serves immutable merged packs and accepts signed GitHub release notifications. */
public final class ResourcePackServer implements AutoCloseable, Listener {

    private static final String PROMPT = "Erethon resource pack";
    private final Hecate plugin;
    private final HttpServer server;
    private final ExecutorService httpWorkers;
    private final ResourcePackCache cache;
    private final String publicBase;
    private final byte[] webhookSecret;
    private final Map<UUID, PackSession> sessions = new ConcurrentHashMap<>();

    private static final class PackSession {
        private final UUID packId;
        private volatile boolean loaded;

        private PackSession(UUID packId) {
            this.packId = packId;
        }
    }

    public ResourcePackServer(Hecate plugin, YamlConfiguration config) throws IOException {
        this.plugin = plugin;
        String bind = config.getString("resourcePack.bind", "127.0.0.1");
        String configuredUrl = config.getString("resourcePack.publicUrl", "").strip();
        boolean localDevelopment = configuredUrl.isEmpty()
                && ("127.0.0.1".equals(bind) || "localhost".equalsIgnoreCase(bind));
        int port = config.getInt("resourcePack.port", localDevelopment ? 0 : 8124);
        if (port < 0 || port > 65535 || (port == 0 && !localDevelopment)) {
            throw new IOException("resourcePack.port must be 1-65535 outside local development");
        }
        if (configuredUrl.isEmpty()) {
            if (!localDevelopment) {
                throw new IOException("resourcePack.publicUrl is required when binding outside localhost");
            }
        }
        if (!configuredUrl.isEmpty()) {
            URI parsed;
            try {
                parsed = URI.create(configuredUrl);
            } catch (IllegalArgumentException invalid) {
                throw new IOException("Invalid resourcePack.publicUrl", invalid);
            }
            if ((!"https".equalsIgnoreCase(parsed.getScheme()) && !"http".equalsIgnoreCase(parsed.getScheme()))
                    || parsed.getHost() == null || parsed.getQuery() != null || parsed.getFragment() != null) {
                throw new IOException("resourcePack.publicUrl must be an HTTP(S) base URL without query or fragment");
            }
        }
        String secret = config.getString("resourcePack.webhookSecret", "");
        webhookSecret = secret.isBlank() ? null : secret.getBytes(StandardCharsets.UTF_8);
        if (webhookSecret == null) plugin.getLogger().warning("GitHub resource pack webhook is disabled: resourcePack.webhookSecret is empty");

        server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        server.createContext("/packs/", this::servePack);
        server.createContext("/github/resourcepack", this::receiveWebhook);
        httpWorkers = Executors.newFixedThreadPool(2, task -> {
            Thread thread = new Thread(task, "Hecate resource pack HTTP");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(httpWorkers);
        publicBase = localDevelopment
                ? "http://127.0.0.1:" + server.getAddress().getPort()
                : configuredUrl.replaceAll("/+$", "");
        cache = new ResourcePackCache(plugin, this::packUpdated, localDevelopment);
        server.start();
        Bukkit.getScheduler().runTask(plugin, cache::start);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Resource pack server listening on " + bind + ":" + server.getAddress().getPort());
    }

    private URI packUri(ResourcePackCache.Pack pack) {
        return URI.create(publicBase + "/packs/" + pack.hash() + ".zip");
    }

    private void servePack(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String name = exchange.getRequestURI().getPath().substring("/packs/".length());
            if (!name.matches("[0-9a-f]{40}\\.zip")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var file = plugin.getDataFolder().toPath().resolve("resource-pack-cache")
                    .resolve("merged-" + name);
            if (!Files.isRegularFile(file)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Content-Length", Long.toString(Files.size(file)));
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, Files.size(file));
                try (InputStream input = Files.newInputStream(file)) {
                    input.transferTo(exchange.getResponseBody());
                }
            }
        }
    }

    private void receiveWebhook(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            if (webhookSecret == null) {
                exchange.sendResponseHeaders(503, -1);
                return;
            }
            byte[] body = exchange.getRequestBody().readNBytes(1_000_001);
            if (body.length > 1_000_000) {
                exchange.sendResponseHeaders(413, -1);
                return;
            }
            String supplied = exchange.getRequestHeaders().getFirst("X-Hub-Signature-256");
            if (!validSignature(body, supplied)) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }
            String event = exchange.getRequestHeaders().getFirst("X-GitHub-Event");
            try {
                JsonObject payload = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
                String repository = payload.getAsJsonObject("repository").get("full_name").getAsString();
                if (!"DRE2N/Resourcepack".equalsIgnoreCase(repository)) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                if ("release".equals(event) || "push".equals(event)) cache.releaseUpdated();
                exchange.sendResponseHeaders(202, -1);
            } catch (RuntimeException invalid) {
                exchange.sendResponseHeaders(400, -1);
            }
        }
    }

    private boolean validSignature(byte[] body, String supplied) {
        if (supplied == null || !supplied.matches("sha256=[0-9a-f]{64}")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret, "HmacSHA256"));
            byte[] expected = mac.doFinal(body);
            return MessageDigest.isEqual(expected, HexFormat.of().parseHex(supplied.substring(7)));
        } catch (Exception e) {
            return false;
        }
    }

    private void packUpdated(ResourcePackCache.Pack pack) {
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getOnlinePlayers().forEach(this::send));
    }

    public boolean send(Player player) {
        ResourcePackCache.Pack pack = cache.current();
        if (pack == null) return false;
        PackSession session = new PackSession(pack.id());
        PackSession previous = sessions.put(player.getUniqueId(), session);
        player.removeMetadata(SkillVfx.LOADED_METADATA, plugin);
        if (previous != null && !previous.packId.equals(pack.id())) {
            player.removeResourcePack(previous.packId);
        }
        player.addResourcePack(pack.id(), packUri(pack).toString(), pack.sha1(), PROMPT, false);
        return true;
    }

    @EventHandler
    public void onConfigure(PlayerConnectionInitialConfigureEvent event) {
        ResourcePackCache.Pack pack = cache.current();
        if (pack == null) return;
        UUID playerId = event.getConnection().getProfile().getId();
        if (playerId == null) return;
        PackSession session = new PackSession(pack.id());
        sessions.put(playerId, session);
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(pack.id(), packUri(pack), pack.hash());
        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(info).replace(true).required(false).prompt(Component.text(PROMPT))
                .callback((id, status, audience) -> {
                    if (!pack.id().equals(id) || status.intermediate() || sessions.get(playerId) != session) return;
                    session.loaded = status == ResourcePackStatus.SUCCESSFULLY_LOADED;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && sessions.get(playerId) == session) updateLoaded(player, session.loaded);
                    });
                }).build();
        event.getConnection().getAudience().sendResourcePacks(request);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PackSession session = sessions.get(player.getUniqueId());
        ResourcePackCache.Pack current = cache.current();
        if (session == null || (current != null && !session.packId.equals(current.id()))) send(player);
        else updateLoaded(player, session.loaded);
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        PackSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || !session.packId.equals(event.getID())) return;
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            session.loaded = true;
            updateLoaded(event.getPlayer(), true);
        } else if (event.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD
                || event.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_RELOAD
                || event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED
                || event.getStatus() == PlayerResourcePackStatusEvent.Status.DISCARDED
                || event.getStatus() == PlayerResourcePackStatusEvent.Status.INVALID_URL) {
            session.loaded = false;
            updateLoaded(event.getPlayer(), false);
            plugin.getLogger().warning("Resource pack " + event.getStatus() + " for " + event.getPlayer().getName());
        }
    }

    private void updateLoaded(Player player, boolean loaded) {
        if (loaded) {
            player.setMetadata(SkillVfx.LOADED_METADATA, new FixedMetadataValue(plugin, true));
            plugin.getStatusDisplayManager().refreshPackText(player);
        } else {
            player.removeMetadata(SkillVfx.LOADED_METADATA, plugin);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
        event.getPlayer().removeMetadata(SkillVfx.LOADED_METADATA, plugin);
    }

    @Override
    public void close() {
        cache.close();
        server.stop(0);
        httpWorkers.shutdownNow();
        sessions.clear();
    }
}

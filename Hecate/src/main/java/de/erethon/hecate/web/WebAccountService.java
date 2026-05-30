package de.erethon.hecate.web;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WebAccountService {

    public static final String SESSION_COOKIE = "hecate_session";

    private static final Duration SETUP_TTL = Duration.ofMinutes(10);
    private static final Duration SESSION_TTL = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DatabaseManager databaseManager;
    private final byte[] sessionSecret;
    private final Map<UUID, PendingSetup> pendingSetups = new ConcurrentHashMap<>();

    public WebAccountService(DatabaseManager databaseManager, String configuredSecret) {
        this.databaseManager = databaseManager;
        this.sessionSecret = loadSessionSecret(configuredSecret).getBytes(StandardCharsets.UTF_8);
    }

    private String loadSessionSecret(String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return configuredSecret;
        }
        File webConfigFile = new File(Hecate.getInstance().getDataFolder(), "web.yml");
        YamlConfiguration webConfig = YamlConfiguration.loadConfiguration(webConfigFile);
        String stored = webConfig.getString("sessionSecret");
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String generated = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        webConfig.set("sessionSecret", generated);
        try {
            File parent = webConfigFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            webConfig.save(webConfigFile);
            Hecate.log("Generated stable web session secret in " + webConfigFile.getPath() + ".");
        } catch (IOException e) {
            Hecate.log("Failed to save stable web session secret to " + webConfigFile.getPath() + ". Web sessions will be invalid after plugin restart.");
            e.printStackTrace();
        }
        return generated;
    }

    public CompletableFuture<SetupStartResult> startSetup(String playerName, String browserIp) {
        return databaseManager.queryAsync(handle -> handle.createQuery("""
                        SELECT player_id, COALESCE(web_password_hash, '') AS web_password_hash
                        FROM Players
                        WHERE lower(last_known_name) = lower(:name)
                        ORDER BY last_online DESC NULLS LAST
                        LIMIT 1
                        """)
                .bind("name", playerName)
                .map((rs, ctx) -> new PlayerAccountRow(
                        rs.getObject("player_id", UUID.class),
                        rs.getString("web_password_hash")
                ))
                .findOne()).thenApply(row -> {
            if (row.isEmpty()) {
                return new SetupStartResult(false, "No player with that name was found.", null);
            }
            UUID playerId = row.get().playerId();
            String token = newToken();
            pendingSetups.put(playerId, new PendingSetup(playerId, browserIp, token, Instant.now().plus(SETUP_TTL), false));
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(net.kyori.adventure.text.Component.text("Web account setup requested. Run /web confirm to continue."));
            }
            return new SetupStartResult(true, "Run /web confirm in game, then return here to set a password.", token);
        });
    }

    public ConfirmResult confirmSetup(Player player, String playerIp) {
        PendingSetup setup = pendingSetups.get(player.getUniqueId());
        if (setup == null || setup.isExpired()) {
            return new ConfirmResult(false, "No active web setup request found.");
        }
        if (!setup.browserIp().equals(playerIp)) {
            return new ConfirmResult(false, "Web setup IP does not match your Minecraft connection IP.");
        }
        pendingSetups.put(player.getUniqueId(), setup.markConfirmed());
        return new ConfirmResult(true, "Web setup confirmed. Return to the browser to set your password.");
    }

    public CompletableFuture<LoginResult> finishSetup(String token, char[] password) {
        Optional<PendingSetup> setup = pendingSetups.values().stream()
                .filter(candidate -> candidate.token().equals(token))
                .findFirst();
        if (setup.isEmpty() || setup.get().isExpired() || !setup.get().confirmed()) {
            return CompletableFuture.completedFuture(new LoginResult(false, "Setup was not confirmed or has expired.", null, null));
        }
        String hash = WebPasswordHasher.hash(password);
        UUID playerId = setup.get().playerId();
        return databaseManager.queryAsync(handle -> {
            handle.createUpdate("""
                        UPDATE Players
                        SET web_password_hash = :hash,
                            web_enabled_at = NOW(),
                            web_session_version = COALESCE(web_session_version, 0) + 1
                        WHERE player_id = :playerId
                        """)
                .bind("hash", hash)
                .bind("playerId", playerId)
                .execute();
            return handle.createQuery("SELECT COALESCE(web_session_version, 0) FROM Players WHERE player_id = :playerId")
                    .bind("playerId", playerId)
                    .mapTo(Integer.class)
                    .one();
        }).thenApply(sessionVersion -> {
            pendingSetups.remove(playerId);
            return new LoginResult(true, "Web account enabled.", playerId, createSessionToken(playerId, sessionVersion));
        });
    }

    public CompletableFuture<LoginResult> login(String playerName, char[] password) {
        return databaseManager.queryAsync(handle -> handle.createQuery("""
                        SELECT player_id, web_password_hash, COALESCE(web_session_version, 0) AS web_session_version
                        FROM Players
                        WHERE lower(last_known_name) = lower(:name)
                        ORDER BY last_online DESC NULLS LAST
                        LIMIT 1
                        """)
                .bind("name", playerName)
                .map((rs, ctx) -> new LoginRow(
                        rs.getObject("player_id", UUID.class),
                        rs.getString("web_password_hash"),
                        rs.getInt("web_session_version")
                ))
                .findOne()).thenCompose(row -> {
            if (row.isEmpty() || !WebPasswordHasher.verify(password, row.get().passwordHash())) {
                return CompletableFuture.completedFuture(new LoginResult(false, "Invalid Minecraft name or password.", null, null));
            }
            LoginRow login = row.get();
            return databaseManager.executeAsync(handle -> handle.createUpdate("UPDATE Players SET web_last_login_at = NOW() WHERE player_id = :playerId")
                    .bind("playerId", login.playerId())
                    .execute()).thenApply(v -> new LoginResult(true, "Logged in.", login.playerId(), createSessionToken(login.playerId(), login.sessionVersion())));
        });
    }

    public CompletableFuture<Optional<UUID>> authenticate(String sessionToken) {
        SessionClaims claims = parseSessionToken(sessionToken);
        if (claims == null || claims.expiresAt().isBefore(Instant.now())) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return getSessionVersion(claims.playerId())
                .thenApply(version -> version == claims.sessionVersion() ? Optional.of(claims.playerId()) : Optional.empty());
    }

    public CompletableFuture<Void> logout(UUID playerId) {
        return databaseManager.executeAsync(handle -> handle.createUpdate("""
                        UPDATE Players
                        SET web_session_version = COALESCE(web_session_version, 0) + 1
                        WHERE player_id = :playerId
                        """)
                .bind("playerId", playerId)
                .execute());
    }

    private CompletableFuture<Integer> getSessionVersion(UUID playerId) {
        return databaseManager.queryAsync(handle -> handle.createQuery("SELECT COALESCE(web_session_version, 0) FROM Players WHERE player_id = :playerId")
                .bind("playerId", playerId)
                .mapTo(Integer.class)
                .findOne()
                .orElse(0));
    }

    private String createSessionToken(UUID playerId, int sessionVersion) {
        long expiresAt = Instant.now().plus(SESSION_TTL).getEpochSecond();
        String payload = playerId + ":" + sessionVersion + ":" + expiresAt;
        String signature = sign(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    private SessionClaims parseSessionToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(sessionToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 4) {
                return null;
            }
            String payload = parts[0] + ":" + parts[1] + ":" + parts[2];
            if (!constantTimeEquals(sign(payload), parts[3])) {
                return null;
            }
            return new SessionClaims(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Instant.ofEpochSecond(Long.parseLong(parts[2])));
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sessionSecret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign web session", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return Arrays.equals(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record PendingSetup(UUID playerId, String browserIp, String token, Instant expiresAt, boolean confirmed) {
        boolean isExpired() {
            return expiresAt.isBefore(Instant.now());
        }

        PendingSetup markConfirmed() {
            return new PendingSetup(playerId, browserIp, token, expiresAt, true);
        }
    }

    private record PlayerAccountRow(UUID playerId, String passwordHash) {
    }

    private record LoginRow(UUID playerId, String passwordHash, int sessionVersion) {
    }

    private record SessionClaims(UUID playerId, int sessionVersion, Instant expiresAt) {
    }

    public record SetupStartResult(boolean success, String message, String token) {
    }

    public record ConfirmResult(boolean success, String message) {
    }

    public record LoginResult(boolean success, String message, UUID playerId, String sessionToken) {
    }
}

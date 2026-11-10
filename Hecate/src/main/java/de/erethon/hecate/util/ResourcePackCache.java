package de.erethon.hecate.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.erethon.hecate.Hecate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Persistent release cache and deterministic merge of the release, Hecate VFX, and local fragments. */
final class ResourcePackCache implements AutoCloseable {

    private static final URI RELEASE_API = URI.create("https://api.github.com/repos/DRE2N/Resourcepack/releases/tags/latest");
    private static final long CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);
    private static final long MIN_STARTUP_CHECK_MS = TimeUnit.MINUTES.toMillis(15);
    private static final long MAX_DOWNLOAD_BYTES = 256L * 1024 * 1024;
    private static final long MAX_ENTRY_BYTES = 64L * 1024 * 1024;
    private static final long MAX_MERGED_BYTES = 512L * 1024 * 1024;
    private static final String DEFAULT_FONT = "assets/minecraft/font/default.json";
    private static final String OVERHEAD_FONT = "assets/hecate/font/combat_overhead.json";

    record Pack(Path file, byte[] sha1, UUID id, String hash) { }

    private final Hecate plugin;
    private final Path cacheDir;
    private final Path releaseFile;
    private final Path propertiesFile;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "Hecate resource pack cache");
        thread.setDaemon(true);
        return thread;
    });
    private final Consumer<Pack> onUpdate;
    private final AtomicBoolean webhookCheckPending = new AtomicBoolean();
    private final Properties properties = new Properties();
    private volatile Pack current;

    ResourcePackCache(Hecate plugin, Consumer<Pack> onUpdate, boolean localFallback) throws IOException {
        this.plugin = plugin;
        this.onUpdate = onUpdate;
        cacheDir = plugin.getDataFolder().toPath().resolve("resource-pack-cache");
        releaseFile = cacheDir.resolve("Erethon-Resourcepack.zip");
        propertiesFile = cacheDir.resolve("state.properties");
        Files.createDirectories(cacheDir);
        if (Files.isRegularFile(propertiesFile)) {
            try (InputStream input = Files.newInputStream(propertiesFile)) {
                properties.load(input);
            }
        }
        String hash = properties.getProperty("mergedHash", "");
        if (hash.matches("[0-9a-f]{40}")) {
            Path file = cacheDir.resolve("merged-" + hash + ".zip");
            if (Files.isRegularFile(file)) current = pack(file, hash);
        }
        if (current == null && localFallback) prepareLocalFallback();
    }

    void start() {
        long lastCheck;
        try {
            lastCheck = Long.parseLong(properties.getProperty("lastAttempt", "0"));
        } catch (NumberFormatException ignored) {
            lastCheck = 0;
        }
        long delay = Math.max(0, MIN_STARTUP_CHECK_MS - (System.currentTimeMillis() - lastCheck));
        worker.execute(this::rebuildFromCache);
        worker.schedule(this::refresh, delay, TimeUnit.MILLISECONDS);
        worker.scheduleWithFixedDelay(this::refresh, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    Pack current() {
        return current;
    }

    void releaseUpdated() {
        // A push can precede the workflow uploading the release asset.
        if (webhookCheckPending.compareAndSet(false, true)) {
            worker.schedule(() -> {
                webhookCheckPending.set(false);
                refresh();
            }, 90, TimeUnit.SECONDS);
        }
    }

    private void refresh() {
        try {
            properties.setProperty("lastAttempt", Long.toString(System.currentTimeMillis()));
            saveProperties();
            HttpRequest.Builder request = HttpRequest.newBuilder(RELEASE_API)
                    .timeout(Duration.ofSeconds(20)).header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Hecate-ResourcePack").GET();
            String etag = properties.getProperty("etag");
            if (etag != null && Files.isRegularFile(releaseFile)) request.header("If-None-Match", etag);
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray assets = release.getAsJsonArray("assets");
                JsonObject asset = null;
                for (var element : assets) {
                    JsonObject candidate = element.getAsJsonObject();
                    if ("Erethon-Resourcepack.zip".equals(candidate.get("name").getAsString())) {
                        asset = candidate;
                        break;
                    }
                }
                if (asset == null) throw new IOException("latest release has no Erethon-Resourcepack.zip asset");
                String revision = asset.get("id").getAsString() + ":" + asset.get("updated_at").getAsString();
                if (!revision.equals(properties.getProperty("releaseRevision")) || !Files.isRegularFile(releaseFile)) {
                    download(URI.create(asset.get("browser_download_url").getAsString()));
                    properties.setProperty("releaseRevision", revision);
                    plugin.getLogger().info("Updated cached Erethon release pack to " + revision);
                }
                response.headers().firstValue("ETag").ifPresent(value -> properties.setProperty("etag", value));
            } else if (response.statusCode() != 304) {
                throw new IOException("GitHub release check returned HTTP " + response.statusCode());
            }
            properties.setProperty("lastCheck", Long.toString(System.currentTimeMillis()));
            rebuildFromCache();
            saveProperties();
        } catch (Exception e) {
            plugin.getLogger().warning("Resource pack update failed; keeping cached pack: " + e.getMessage());
        }
    }

    private void download(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(2))
                .header("User-Agent", "Hecate-ResourcePack").GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("Release pack download returned HTTP " + response.statusCode());
        }
        Path temp = Files.createTempFile(cacheDir, "release-", ".zip");
        try {
            try (InputStream input = response.body(); OutputStream output = Files.newOutputStream(temp)) {
                copyLimited(input, output, MAX_DOWNLOAD_BYTES);
            }
            try (ZipFile zip = new ZipFile(temp.toFile())) {
                if (zip.getEntry("pack.mcmeta") == null) throw new IOException("Release ZIP has no pack.mcmeta");
            }
            move(temp, releaseFile);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void rebuildFromCache() {
        if (!Files.isRegularFile(releaseFile)) return;
        try {
            Path bundled = Files.createTempFile(cacheDir, "hecate-vfx-", ".zip");
            try {
                copyBundledPack(bundled);
                List<Path> sources = new ArrayList<>();
                sources.add(releaseFile);
                sources.add(bundled);
                sources.addAll(fragments());
                Path merged = Files.createTempFile(cacheDir, "merged-", ".zip");
                try {
                    merge(sources, merged);
                    byte[] sha1 = sha1(merged);
                    String hash = HexFormat.of().formatHex(sha1);
                    Path destination = cacheDir.resolve("merged-" + hash + ".zip");
                    if (!Files.exists(destination)) move(merged, destination);
                    Pack previous = current;
                    if (previous == null || !previous.hash().equals(hash)) {
                        Pack next = pack(destination, hash);
                        current = next;
                        properties.setProperty("mergedHash", hash);
                        saveProperties();
                        onUpdate.accept(next);
                        plugin.getLogger().info("Merged Erethon resource pack ready: " + hash);
                    }
                } finally {
                    Files.deleteIfExists(merged);
                }
            } finally {
                Files.deleteIfExists(bundled);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not merge resource packs; keeping previous pack: " + e.getMessage());
        }
    }

    private void prepareLocalFallback() throws IOException {
        Path bundled = Files.createTempFile(cacheDir, "hecate-vfx-", ".zip");
        Path merged = Files.createTempFile(cacheDir, "local-vfx-", ".zip");
        try {
            copyBundledPack(bundled);
            List<Path> sources = new ArrayList<>();
            sources.add(bundled);
            sources.addAll(fragments());
            merge(sources, merged);
            String hash = HexFormat.of().formatHex(sha1(merged));
            Path destination = cacheDir.resolve("merged-" + hash + ".zip");
            if (!Files.exists(destination)) move(merged, destination);
            current = pack(destination, hash);
            plugin.getLogger().info("Local VFX pack ready while the published pack is fetched");
        } finally {
            Files.deleteIfExists(bundled);
            Files.deleteIfExists(merged);
        }
    }

    private void copyBundledPack(Path destination) throws IOException {
        try (InputStream input = plugin.getResource("dev-vfx-pack.zip")) {
            if (input == null) throw new IOException("Missing bundled dev-vfx-pack.zip");
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<Path> fragments() throws IOException {
        Path pluginDirectory = plugin.getDataFolder().toPath();
        Path directory = pluginDirectory.resolve("pack-fragments");
        List<Path> sources = new ArrayList<>();
        if (Files.isDirectory(directory)) {
            try (var paths = Files.list(directory)) {
                paths.filter(path -> path.getFileName().toString().endsWith(".zip"))
                        .sorted().forEachOrdered(sources::add);
            }
        }
        // Daedalus generates the models that its live mobs reference at startup.
        Path mobModels = pluginDirectory.resolveSibling("Daedalus").resolve("output/Daedalus.zip");
        if (Files.isRegularFile(mobModels)) {
            sources.add(mobModels);
        }
        return sources;
    }

    private static void merge(List<Path> sources, Path destination) throws IOException {
        Map<String, Integer> owners = new HashMap<>();
        for (int i = 0; i < sources.size(); i++) {
            try (ZipFile zip = new ZipFile(sources.get(i).toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    if (name.startsWith("/") || name.contains("\\") || name.contains("..")
                            || name.chars().anyMatch(Character::isISOControl)) {
                        throw new IOException("Invalid pack entry: " + name);
                    }
                    if (entry.getSize() > MAX_ENTRY_BYTES) throw new IOException("Oversized pack entry: " + name);
                    if (!"pack.mcmeta".equals(name) || i == 0) owners.put(name, i);
                    if (owners.size() > 100_000) throw new IOException("Too many pack entries");
                }
            }
        }
        if (!owners.containsKey("pack.mcmeta")) throw new IOException("Release ZIP has no pack.mcmeta");
        boolean includeOverheadGlyphs = owners.containsKey(OVERHEAD_FONT);
        long total = 0;
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(destination))) {
            for (int i = 0; i < sources.size(); i++) {
                try (ZipFile zip = new ZipFile(sources.get(i).toFile())) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.isDirectory() || owners.get(entry.getName()) != i) continue;
                        ZipEntry target = new ZipEntry(entry.getName());
                        target.setTime(0);
                        output.putNextEntry(target);
                        try (InputStream input = zip.getInputStream(entry)) {
                            long limit = Math.min(MAX_ENTRY_BYTES, MAX_MERGED_BYTES - total);
                            if (includeOverheadGlyphs && DEFAULT_FONT.equals(entry.getName())) {
                                JsonObject font = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                                total += writeOverheadFontReference(font, output, limit);
                            } else {
                                total += copyLimited(input, output, limit);
                            }
                        }
                        output.closeEntry();
                    }
                }
            }
            if (includeOverheadGlyphs && !owners.containsKey(DEFAULT_FONT)) {
                ZipEntry entry = new ZipEntry(DEFAULT_FONT);
                entry.setTime(0);
                output.putNextEntry(entry);
                writeOverheadFontReference(new JsonObject(), output, MAX_MERGED_BYTES - total);
                output.closeEntry();
            }
        }
    }

    private static int writeOverheadFontReference(JsonObject font, OutputStream output, long limit) throws IOException {
        JsonObject reference = new JsonObject();
        reference.addProperty("type", "reference");
        reference.addProperty("id", "hecate:combat_overhead");
        JsonArray providers = new JsonArray();
        providers.add(reference);
        if (font.has("providers")) providers.addAll(font.getAsJsonArray("providers"));
        font.add("providers", providers);
        byte[] bytes = font.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > limit) throw new IOException("Resource pack exceeds size limit");
        output.write(bytes);
        return bytes.length;
    }

    private static long copyLimited(InputStream input, OutputStream output, long limit) throws IOException {
        byte[] buffer = new byte[8192];
        long copied = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            copied += count;
            if (copied > limit) throw new IOException("Resource pack exceeds size limit");
            output.write(buffer, 0, count);
        }
        return copied;
    }

    private static byte[] sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Pack pack(Path file, String hash) {
        byte[] sha1 = HexFormat.of().parseHex(hash);
        UUID id = UUID.nameUUIDFromBytes(("erethon-pack:" + hash).getBytes(StandardCharsets.UTF_8));
        return new Pack(file, sha1, id, hash);
    }

    private void saveProperties() throws IOException {
        Path temp = Files.createTempFile(cacheDir, "state-", ".properties");
        try {
            try (OutputStream output = Files.newOutputStream(temp)) {
                properties.store(output, "Hecate resource pack cache");
            }
            move(temp, propertiesFile);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void close() {
        worker.shutdownNow();
        client.close();
    }
}

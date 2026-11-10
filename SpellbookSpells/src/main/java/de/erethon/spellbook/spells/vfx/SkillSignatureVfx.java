package de.erethon.spellbook.spells.vfx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.erethon.spellbook.Spellbook;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven, per-skill display effects. The same JSON feeds Hecate's
 * painterly pack generator; casts cannot silently request a different asset.
 */
public final class SkillSignatureVfx {

    private static final String PREFIX = "de.erethon.spellbook.spells.";
    // A signature is the skill's primary visual, not a small accent over legacy VFX.
    private static final float SCENE_SCALE = 2.0f;
    private static final int ART_FRAMES = 8;
    private static final Map<String, Signature> SIGNATURES = load();

    private SkillSignatureVfx() {
    }

    public static int count() {
        return SIGNATURES.size();
    }

    /** Reuses the skill's painted art for a delayed impact or a gameplay cue. */
    public static void accent(Location location, Class<?> spellClass, String layer,
                              float scale, int ticks, boolean ground) {
        accent(location, spellClass, layer, scale, ticks, ground, null);
    }

    /** A moving afterimage can carry its direction without changing stationary impact art. */
    public static void accent(Location location, Class<?> spellClass, String layer,
                              float scale, int ticks, boolean ground, Vector direction) {
        String name = spellClass.getName();
        if (!name.startsWith(PREFIX) || location.getWorld() == null) return;
        Signature signature = SIGNATURES.get(name.substring(PREFIX.length()));
        if (signature == null) return;
        String suffix = switch (layer) {
            case "main" -> "";
            case "lead" -> "_lead";
            case "wake" -> "_wake";
            default -> throw new IllegalArgumentException("Unknown signature layer " + layer);
        };
        Location point = ground ? settle(location.clone()) : location.clone();
        ItemDisplay display = ground
                ? SkillVfx.groundPlane(point, signature.effectId() + suffix + "_f0", 0.01f)
                : direction == null
                        ? SkillVfx.billboard(point, signature.effectId() + suffix + "_f0", 0.01f)
                        : SkillVfx.projectile(point, direction, signature.effectId() + suffix + "_f0", 0.01f);
        int lifetime = Math.max(2, ticks);
        new BukkitRunnable() {
            private int age;

            @Override
            public void run() {
                if (!display.isValid() || age >= lifetime) {
                    display.remove();
                    cancel();
                    return;
                }
                int frame = Math.min(ART_FRAMES - 1, age * ART_FRAMES / lifetime);
                display.setItemStack(SkillVfx.item(signature.effectId() + suffix + "_f" + frame));
                float progress = age / (float) lifetime;
                float size = scale * (0.65f + 0.55f * progress);
                Quaternionf rotation = ground ? new Quaternionf().rotateX((float) Math.PI / 2f)
                        : direction == null ? new Quaternionf() : SkillVfx.projectileRotation(direction);
                display.setTransformation(new Transformation(new Vector3f(), rotation,
                        new Vector3f(size), new Quaternionf()));
                age++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    public static void cast(LivingEntity caster, LivingEntity target, Class<?> spellClass) {
        if (!SkillVfx.availableTo(caster)) return;
        String className = spellClass.getName();
        if (!className.startsWith(PREFIX)) return;
        Signature signature = SIGNATURES.get(className.substring(PREFIX.length()));
        if (signature == null) return;
        Profile profile = signature.profile();
        Vector forward = caster.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 0.0001) forward = new Vector(0, 0, 1);
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        Location origin = anchor(caster, target, forward, profile.anchor());
        if (profile.ground()) origin = settle(origin);
        Location initial = position(origin, right, forward, profile.frames().getFirst());
        ItemDisplay display = sceneLayer(initial, signature.effectId(), 0.05f, profile, forward);
        ItemDisplay lead = sceneLayer(initial, signature.effectId() + "_lead", 0.01f, profile, forward);
        ItemDisplay wake = sceneLayer(initial, signature.effectId() + "_wake", 0.01f, profile, forward);
        List<ItemDisplay> fragments = new ArrayList<>(signature.assemble() ? 4 : 0);
        if (signature.assemble()) {
            for (int quadrant = 0; quadrant < 4; quadrant++) {
                String asset = signature.effectId() + "_q" + quadrant;
                fragments.add(sceneLayer(initial, asset, 0.05f, profile, forward));
            }
        }
        if (profile.frames().getLast().tick() < 1) {
            display.remove();
            lead.remove();
            wake.remove();
            fragments.forEach(ItemDisplay::remove);
            return;
        }
        Location base = origin;
        Vector castRight = right;
        Vector castForward = forward;
        new BukkitRunnable() {
            private int age;

            @Override
            public void run() {
                if (!display.isValid() || age > profile.frames().getLast().tick()) {
                    display.remove();
                    lead.remove();
                    wake.remove();
                    fragments.forEach(ItemDisplay::remove);
                    cancel();
                    return;
                }
                Frame frame = interpolate(profile.frames(), age);
                Location next = position(base, castRight, castForward, frame);
                display.teleport(next);
                int artFrame = Math.min(ART_FRAMES - 1,
                        age * ART_FRAMES / Math.max(1, profile.frames().getLast().tick()));
                display.setItemStack(SkillVfx.item(signature.effectId() + "_f" + artFrame));
                lead.setItemStack(SkillVfx.item(signature.effectId() + "_lead_f" + artFrame));
                wake.setItemStack(SkillVfx.item(signature.effectId() + "_wake_f" + artFrame));
                float size = age < 8 && signature.assemble()
                        ? 0.01f : Math.max(0.01f, signature.scale() * frame.size());
                Quaternionf rotation = sceneRotation(profile, castForward, frame.roll());
                display.setTransformation(new Transformation(new Vector3f(), rotation,
                        new Vector3f(size), new Quaternionf()));
                int windupEnd = profile.frames().get(1).tick();
                int lastTick = profile.frames().getLast().tick();
                float windup = Math.min(1f, age / (float) windupEnd);
                Location leadPosition = initial.clone().add(next.toVector()
                        .subtract(initial.toVector()).multiply(windup * 0.3));
                leadPosition.add(castRight.clone().multiply(-signature.scale() * 0.22 * (1f - windup)));
                float leadSize = age >= windupEnd ? 0.01f
                        : signature.scale() * (0.28f + 0.46f * windup) * (1f - windup);
                transformLayer(lead, leadPosition, Math.max(0.01f, leadSize),
                        profile, castForward, frame.roll() - 32f + windup * 65f);

                float aftermath = Math.max(0f, (age - windupEnd) / (float) (lastTick - windupEnd));
                Location wakePosition = next.clone().add(castForward.clone().multiply(-0.36));
                wakePosition.add(castRight.clone().multiply(signature.scale() * 0.18 * aftermath));
                float wakeSize = age < windupEnd ? 0.01f
                        : signature.scale() * frame.size()
                        * (0.32f + 0.48f * (float) Math.sin(Math.PI * aftermath));
                transformLayer(wake, wakePosition, Math.max(0.01f, wakeSize),
                        profile, castForward, frame.roll() + 28f - aftermath * 48f);
                for (int quadrant = 0; quadrant < fragments.size(); quadrant++) {
                    ItemDisplay fragment = fragments.get(quadrant);
                    if (age >= 8) {
                        fragment.remove();
                        continue;
                    }
                    fragment.setItemStack(SkillVfx.item(signature.effectId() + "_q" + quadrant
                            + "_f" + artFrame));
                    double offset = signature.scale() * 0.45 * (1.0 - age / 8.0);
                    double side = (quadrant % 2 == 0 ? -offset : offset);
                    double other = (quadrant < 2 ? -offset : offset);
                    Location fragmentPosition = next.clone().add(castRight.clone().multiply(side));
                    if (profile.ground()) fragmentPosition.add(castForward.clone().multiply(other));
                    else fragmentPosition.add(0, other, 0);
                    fragment.teleport(fragmentPosition);
                    float fragmentSize = Math.max(0.01f, signature.scale() * frame.size());
                    fragment.setTransformation(new Transformation(new Vector3f(), rotation,
                            new Vector3f(fragmentSize), new Quaternionf()));
                }
                age++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    private static ItemDisplay sceneLayer(Location location, String asset, float scale,
                                          Profile profile, Vector forward) {
        if (profile.ground()) return SkillVfx.groundPlane(location, asset, scale);
        if (profile.directional()) return SkillVfx.projectile(location, forward, asset, scale);
        return SkillVfx.billboard(location, asset, scale);
    }

    private static Quaternionf sceneRotation(Profile profile, Vector forward, float roll) {
        if (profile.ground()) return new Quaternionf().rotateX((float) Math.PI / 2f)
                .rotateZ((float) Math.toRadians(roll));
        if (profile.directional()) return SkillVfx.projectileRotation(forward)
                .rotateX((float) Math.toRadians(roll));
        return new Quaternionf().rotateZ((float) Math.toRadians(roll));
    }

    private static void transformLayer(ItemDisplay display, Location location, float size,
                                       Profile profile, Vector forward, float roll) {
        display.teleport(location);
        Quaternionf rotation = sceneRotation(profile, forward, roll);
        display.setTransformation(new Transformation(new Vector3f(), rotation,
                new Vector3f(size), new Quaternionf()));
    }

    private static Location anchor(LivingEntity caster, LivingEntity target, Vector forward,
                                   String kind) {
        Location casterLocation = caster.getLocation();
        return switch (kind) {
            case "target", "target_ground" -> target == null
                    ? casterLocation.add(forward.clone().multiply(4.0)) : target.getLocation();
            case "behind" -> casterLocation.add(forward.clone().multiply(-0.9));
            case "caster", "forward", "ground" -> casterLocation;
            default -> throw new IllegalArgumentException("Unknown VFX anchor " + kind);
        };
    }

    private static Location position(Location anchor, Vector right, Vector forward, Frame frame) {
        return anchor.clone().add(right.clone().multiply(frame.side()))
                .add(forward.clone().multiply(frame.forward()))
                .add(0, frame.up(), 0);
    }

    private static Location settle(Location location) {
        RayTraceResult hit = location.getWorld().rayTraceBlocks(location.clone().add(0, 1.8, 0),
                new Vector(0, -1, 0), 5.5, FluidCollisionMode.NEVER, true);
        if (hit != null && hit.getHitBlockFace() == BlockFace.UP
                && Math.abs(hit.getHitPosition().getY() - location.getY()) <= 2.2) {
            location.setY(hit.getHitPosition().getY());
        }
        return location;
    }

    private static Frame interpolate(List<Frame> frames, int age) {
        for (int i = 1; i < frames.size(); i++) {
            Frame end = frames.get(i);
            if (age > end.tick()) continue;
            Frame start = frames.get(i - 1);
            float fraction = (age - start.tick()) / (float) (end.tick() - start.tick());
            return new Frame(age,
                    mix(start.size(), end.size(), fraction),
                    mix(start.side(), end.side(), fraction),
                    mix(start.up(), end.up(), fraction),
                    mix(start.forward(), end.forward(), fraction),
                    mix(start.roll(), end.roll(), fraction));
        }
        return frames.getLast();
    }

    private static float mix(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }

    private static Map<String, Signature> load() {
        InputStream stream = SkillSignatureVfx.class.getClassLoader()
                .getResourceAsStream("skill-vfx-signatures.json");
        if (stream == null) throw new IllegalStateException("Missing skill-vfx-signatures.json");
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, Profile> profiles = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("motions").entrySet()) {
                JsonObject definition = entry.getValue().getAsJsonObject();
                List<Frame> frames = new ArrayList<>();
                for (JsonElement element : definition.getAsJsonArray("frames")) {
                    JsonArray values = element.getAsJsonArray();
                    frames.add(new Frame(values.get(0).getAsInt(), values.get(1).getAsFloat(),
                            values.get(2).getAsFloat(), values.get(3).getAsFloat(),
                            values.get(4).getAsFloat(), values.get(5).getAsFloat()));
                }
                if (frames.size() < 2) throw new IllegalStateException("Too few frames: " + entry.getKey());
                boolean ground = definition.has("ground") && definition.get("ground").getAsBoolean();
                boolean directional = definition.has("directional") && definition.get("directional").getAsBoolean();
                if (ground && directional) throw new IllegalStateException("Ground VFX cannot be directional: " + entry.getKey());
                profiles.put(entry.getKey(), new Profile(definition.get("anchor").getAsString(),
                        ground, directional,
                        definition.has("assemble") && definition.get("assemble").getAsBoolean(),
                        List.copyOf(frames)));
            }
            Map<String, Signature> signatures = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("skills").entrySet()) {
                JsonObject definition = entry.getValue().getAsJsonObject();
                Profile profile = profiles.get(definition.get("motion").getAsString());
                if (profile == null) throw new IllegalStateException("Missing VFX motion: " + entry.getKey());
                String effectId = "sig_" + entry.getKey().toLowerCase().replace('.', '_');
                boolean assemble = definition.has("assemble")
                        ? definition.get("assemble").getAsBoolean() : profile.assemble();
                signatures.put(entry.getKey(), new Signature(effectId,
                        definition.get("scale").getAsFloat() * SCENE_SCALE, profile, assemble));
            }
            if (signatures.size() != 54) throw new IllegalStateException("Expected 54 skill signatures");
            return Map.copyOf(signatures);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid skill VFX signatures", exception);
        }
    }

    private record Frame(int tick, float size, float side, float up, float forward, float roll) {
    }

    private record Profile(String anchor, boolean ground, boolean directional,
                           boolean assemble, List<Frame> frames) {
    }

    private record Signature(String effectId, float scale, Profile profile, boolean assemble) {
    }
}

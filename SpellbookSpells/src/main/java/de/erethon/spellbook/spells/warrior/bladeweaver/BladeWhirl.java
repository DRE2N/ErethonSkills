package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CircleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Blade Whirl - Spin attack that damages all nearby enemies.
 * Consumes Razor Mark stacks on hit enemies for bonus damage.
 * In demon form: Creates a persistent whirlwind with orbiting swords that continues damaging enemies.
 */
public class BladeWhirl extends BladeweaverBaseSpell {

    private final double whirlRadius = data.getDouble("whirlRadius", 4.0);
    private final double whirlHeight = data.getDouble("whirlHeight", 2.5);
    private final double bonusDamagePerStack = data.getDouble("bonusDamagePerStack", 0.15);
    private final int demonFormDuration = data.getInt("demonFormDuration", 60); // 3 seconds
    private final int demonFormTickInterval = data.getInt("demonFormTickInterval", 10); // Damage every 0.5s

    private final Set<LivingEntity> hitThisTick = new HashSet<>();
    private int demonWhirlTimer = 0;
    private CircleEffect persistentEffect;

    private final List<ItemDisplay> orbitingSwords = new ArrayList<>();
    private static final int SWORD_COUNT = 4;

    public BladeWhirl(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 20;
    }

    @Override
    public boolean onCast() {
        if (isInDemonForm()) {
            keepAliveTicks = demonFormDuration;
        }

        createOrbitingSwords();
        playWhirlEffect();
        playWhirlSound();

        dealWhirlDamage(true);

        if (isInDemonForm()) {
            demonWhirlTimer = demonFormDuration;
            startPersistentWhirlwind();
        }

        return super.onCast();
    }

    private void createOrbitingSwords() {
        Location center = caster.getLocation().add(0, 1, 0);
        int swordCount = isInDemonForm() ? SWORD_COUNT + 2 : SWORD_COUNT;

        for (int i = 0; i < swordCount; i++) {
            final int index = i;
            ItemDisplay sword = center.getWorld().spawn(center, ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.GOLDEN_SWORD));
                display.setBillboard(Display.Billboard.FIXED);
                display.setGlowing(true);
                display.setPersistent(false);
                display.setGlowColorOverride(getThemeColor());
                display.setInterpolationDuration(1);
                double angle = (Math.PI * 2 * index / swordCount);
                updateSwordPosition(display, angle, 0);
            });
            orbitingSwords.add(sword);
        }
    }

    private void updateSwordPosition(ItemDisplay sword, double angle, float heightOffset) {
        float x = (float) (Math.cos(angle) * whirlRadius * 0.8);
        float z = (float) (Math.sin(angle) * whirlRadius * 0.8);
        Quaternionf rotation = new Quaternionf();
        rotation.rotateY((float) angle + (float) Math.PI / 2);
        rotation.rotateZ((float) Math.PI / 4);

        sword.setTransformation(new Transformation(
            new Vector3f(x, heightOffset, z),
            rotation,
            new Vector3f(1.2f, 1.2f, 1.2f),
            new Quaternionf()
        ));
    }

    @Override
    protected void onTick() {
        super.onTick();
        updateOrbitingSwords();
        if (isInDemonForm() && demonWhirlTimer > 0) {
            demonWhirlTimer--;
            if (demonWhirlTimer % demonFormTickInterval == 0) {
                hitThisTick.clear();
                dealWhirlDamage(false);
            }
            spawnDemonWhirlParticles();
        }

        if (!isInDemonForm() && currentTicks >= keepAliveTicks - 5) {
            disperseSwords();
        }
    }

    private void updateOrbitingSwords() {
        Location center = caster.getLocation().add(0, 1, 0);
        float spinSpeed = isInDemonForm() ? 0.4f : 0.6f;
        float time = currentTicks * spinSpeed;

        int swordCount = orbitingSwords.size();
        for (int i = 0; i < swordCount; i++) {
            ItemDisplay sword = orbitingSwords.get(i);
            if (sword == null || !sword.isValid()) continue;

            double angle = (Math.PI * 2 * i / swordCount) + time;
            float heightOffset = (float) Math.sin(time * 2 + i * 0.5) * 0.3f; // Bobbing effect

            float x = (float) (Math.cos(angle) * whirlRadius * 0.75);
            float z = (float) (Math.sin(angle) * whirlRadius * 0.75);
            Location swordLoc = center.clone().add(x, heightOffset, z);
            sword.teleport(swordLoc);

            Quaternionf rotation = new Quaternionf();
            rotation.rotateY((float) angle + (float) Math.PI / 2);
            rotation.rotateX((float) Math.PI / 6);
            rotation.rotateZ(time * 2);

            sword.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                rotation,
                new Vector3f(1.2f, 1.2f, 1.2f),
                new Quaternionf()
            ));

            if (currentTicks % 2 == 0) {
                Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 0.8f);
                swordLoc.getWorld().spawnParticle(Particle.DUST, swordLoc, 2, 0.1, 0.1, 0.1, 0, dust);
            }
        }
    }

    private void disperseSwords() {
        for (int i = 0; i < orbitingSwords.size(); i++) {
            ItemDisplay sword = orbitingSwords.get(i);
            if (sword == null || !sword.isValid()) continue;

            Location loc = sword.getLocation();
            double angle = (Math.PI * 2 * i / orbitingSwords.size());

            loc.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.2, 0.2, 0.2, 0,
                new Particle.DustOptions(getThemeColor(), 1.0f));
            loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1);
        }
    }

    private void playWhirlEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        Color color = getThemeColor();
        for (int ring = 0; ring < 3; ring++) {
            double radius = (ring + 1) * (whirlRadius / 3);
            int particles = (int) (radius * 8);

            for (int i = 0; i < particles; i++) {
                double angle = (Math.PI * 2 * i / particles);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location particleLoc = loc.clone().add(x, 0, z);
                caster.getWorld().spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(color, 1.2f));
                caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 8, whirlRadius / 2, 0.5, whirlRadius / 2, 0);
    }

    private void playWhirlSound() {
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.5f);
    }

    private void dealWhirlDamage(boolean consumeMarks) {
        for (LivingEntity entity : caster.getWorld().getNearbyLivingEntities(caster.getLocation(), whirlRadius, whirlHeight)) {
            if (entity.equals(caster) || !Spellbook.canAttack(caster, entity) || hitThisTick.contains(entity)) {
                continue;
            }
            hitThisTick.add(entity);
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
            if (consumeMarks) {
                int stacks = consumeRazorMarkStacks(entity);
                if (stacks > 0) {
                    double bonusMultiplier = 1 + (stacks * bonusDamagePerStack);
                    damage *= bonusMultiplier;
                    playMarkConsumptionEffect(entity, stacks);
                }
            }
            entity.damage(damage, caster, PDamageType.PHYSICAL);
            playHitEffect(entity);
            grantBonusHealthForHit();
        }
    }

    private void playHitEffect(LivingEntity target) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
    }


    private void startPersistentWhirlwind() {
        persistentEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        persistentEffect.setEntity(caster);
        persistentEffect.radius = (float) whirlRadius;
        persistentEffect.particle = Particle.FLAME;
        persistentEffect.particleCount = 2;
        persistentEffect.enableRotation = true;
        persistentEffect.angularVelocityY = Math.PI / 4;
        persistentEffect.type = EffectType.REPEATING;
        persistentEffect.duration = demonFormDuration * 50; // Convert to milliseconds approximately
        persistentEffect.start();
    }

    private void spawnDemonWhirlParticles() {
        Location loc = caster.getLocation().add(0, 0.5, 0);
        double time = currentTicks * 0.3;
        for (int i = 0; i < 4; i++) {
            double angle = time + (i * Math.PI / 2);
            double x = Math.cos(angle) * whirlRadius * 0.8;
            double z = Math.sin(angle) * whirlRadius * 0.8;

            Location particleLoc = loc.clone().add(x, 0, z);
            caster.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.2, 0.1, 0.01);
            caster.getWorld().spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0,
                new Particle.DustOptions(BLADEWEAVER_DEMON, 1.0f));
        }
    }

    @Override
    protected void cleanup() {
        if (persistentEffect != null) {
            persistentEffect.cancel();
        }
        for (ItemDisplay sword : orbitingSwords) {
            if (sword != null && sword.isValid()) {
                Location loc = sword.getLocation();
                loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(getThemeColor(), 1.0f));
                sword.remove();
            }
        }
        orbitingSwords.clear();
        super.cleanup();
    }
}

package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CircleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Weave - Ultimate transformation ability.
 * Requires full energy to cast. Transforms the Bladeweaver into demon form,
 * empowering all abilities and granting stat bonuses.
 * All other abilities gain enhanced demon form versions while active.
 */
public class Weave extends BladeweaverBaseSpell {

    private final int transformDuration = data.getInt("transformDuration", 300); // 15 seconds
    private final double damageBonus = data.getDouble("damageBonus", 0.25);
    private final double speedBonus = data.getDouble("speedBonus", 0.15);
    private final double resistanceBonus = data.getDouble("resistanceBonus", 0.1);
    private final int energyRequired = data.getInt("energyRequired", 100);

    private final AttributeModifier damageModifier = new AttributeModifier(
        NamespacedKey.fromString("spellbook:bladeweaver_weave_damage"),
        damageBonus,
        AttributeModifier.Operation.ADD_SCALAR
    );

    private final AttributeModifier speedModifier = new AttributeModifier(
        NamespacedKey.fromString("spellbook:bladeweaver_weave_speed"),
        speedBonus,
        AttributeModifier.Operation.ADD_SCALAR
    );

    private final AttributeModifier resistanceModifier = new AttributeModifier(
        NamespacedKey.fromString("spellbook:bladeweaver_weave_resistance"),
        resistanceBonus,
        AttributeModifier.Operation.ADD_SCALAR
    );

    private CircleEffect auraEffect;
    private boolean transformed = false;

    private final List<ItemDisplay> auraSwords = new ArrayList<>();
    private static final int AURA_SWORD_COUNT = 3;

    public Weave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = transformDuration;
        energyBuildUpFromUse = 0; // Ultimate doesn't build energy
    }

    @Override
    protected boolean onPrecast() {
        if (isInDemonForm()) {
            caster.sendParsedActionBar("<red>Already in Demon Form!");
            return false;
        }

        if (caster.getEnergy() < energyRequired) {
            caster.sendParsedActionBar("<red>Not enough energy! (" + (int)caster.getEnergy() + "/" + energyRequired + ")");
            return false;
        }

        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        caster.setEnergy(0);
        enterDemonForm();

        return super.onCast();
    }

    private void enterDemonForm() {
        transformed = true;
        caster.getTags().add(DEMON_FORM_TAG);

        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(damageModifier);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(speedModifier);
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(resistanceModifier);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(resistanceModifier);

        resetAllCooldowns();
        playTransformationEffect();
        createAuraSwords();
        startDemonAura();
    }

    private void resetAllCooldowns() {
        caster.getUsedSpells().clear();
    }

    private void createAuraSwords() {
        Location center = caster.getLocation().add(0, 2, 0);

        for (int i = 0; i < AURA_SWORD_COUNT; i++) {
            final int index = i;
            ItemDisplay sword = center.getWorld().spawn(center, ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.GOLDEN_SWORD));
                display.setBillboard(Display.Billboard.FIXED);
                display.setGlowing(true);
                display.setPersistent(false);
                display.setGlowColorOverride(BLADEWEAVER_DEMON);
                display.setInterpolationDuration(2);

                double angle = (Math.PI * 2 * index / AURA_SWORD_COUNT);
                float x = (float) (Math.cos(angle) * 1.5);
                float z = (float) (Math.sin(angle) * 1.5);

                Quaternionf rotation = new Quaternionf();
                rotation.rotateY((float) angle);
                rotation.rotateZ((float) Math.PI / 6); // Tilted outward

                display.setTransformation(new Transformation(
                    new Vector3f(x, 0.5f, z),
                    rotation,
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Quaternionf()
                ));
            });
            auraSwords.add(sword);
        }
    }

    private void playTransformationEffect() {
        Location loc = caster.getLocation();
        caster.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1);
        for (int ring = 0; ring < 4; ring++) {
            final int ringNum = ring;
            for (int j = 0; j < 8; j++) {
                double angle = (Math.PI * 2 * j / 8) + (ring * Math.PI / 4);
                double radius = 1.5 + ring * 0.3;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = ring * 0.6;

                Location particleLoc = loc.clone().add(x, y, z);
                caster.getWorld().spawnParticle(Particle.FLAME, particleLoc, 3, 0.1, 0.1, 0.1, 0.05);
                caster.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(BLADEWEAVER_DEMON, 1.5f));
            }
        }

        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            double x = Math.cos(angle) * 2;
            double z = Math.sin(angle) * 2;
            caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(x, 1, z), 1);
        }

        caster.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.5f);
        caster.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        caster.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.7f);
        caster.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
    }

    private void startDemonAura() {
        auraEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        auraEffect.setEntity(caster);
        auraEffect.radius = 1.5f;
        auraEffect.particle = Particle.FLAME;
        auraEffect.particleCount = 3;
        auraEffect.enableRotation = true;
        auraEffect.angularVelocityY = Math.PI / 8;
        auraEffect.type = EffectType.REPEATING;
        auraEffect.duration = transformDuration * 50;
        auraEffect.start();
    }

    @Override
    protected void onTick() {
        super.onTick();

        if (!transformed) return;
        updateAuraSwords();
        if (currentTicks % 5 == 0) {
            spawnAmbientParticles();
        }

        if (currentTicks >= transformDuration - 60 && currentTicks % 20 == 0) {
            playWarningEffect();
        }
    }

    private void updateAuraSwords() {
        Location center = caster.getLocation().add(0, 2, 0);
        float time = currentTicks * 0.05f;

        for (int i = 0; i < auraSwords.size(); i++) {
            ItemDisplay sword = auraSwords.get(i);
            if (sword == null || !sword.isValid()) continue;

            double angle = (Math.PI * 2 * i / AURA_SWORD_COUNT) + time;
            float radius = 1.2f + (float) Math.sin(time * 2 + i) * 0.2f;
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            float y = 0.3f + (float) Math.sin(time * 3 + i * 2) * 0.2f;

            Location swordLoc = center.clone().add(x, y, z);
            sword.teleport(swordLoc);

            Quaternionf rotation = new Quaternionf();
            rotation.rotateY((float) angle + (float) Math.PI / 2);
            rotation.rotateZ((float) Math.PI / 4);
            rotation.rotateX(time * 0.5f);

            sword.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                rotation,
                new Vector3f(0.9f, 0.9f, 0.9f),
                new Quaternionf()
            ));
        }
    }

    private void spawnAmbientParticles() {
        Location loc = caster.getLocation().add(0, 1, 0);

        caster.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0.3, 0.5, 0.3, 0.01);
        double angle = currentTicks * 0.1;
        double x = Math.cos(angle) * 0.8;
        double z = Math.sin(angle) * 0.8;
        Location groundLoc = caster.getLocation().add(x, 0.1, z);
        caster.getWorld().spawnParticle(Particle.DUST, groundLoc, 2, 0.1, 0.05, 0.1, 0,
            new Particle.DustOptions(BLADEWEAVER_DEMON, 0.8f));
    }

    private void playWarningEffect() {
        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 2.5, 0), 10, 0.3, 0.1, 0.3, 0,
            new Particle.DustOptions(BLADEWEAVER_DEMON, 1.0f));
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);

        for (ItemDisplay sword : auraSwords) {
            if (sword != null && sword.isValid()) {
                Location loc = sword.getLocation();
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    @Override
    protected void onTickFinish() {
        exitDemonForm();
        super.onTickFinish();
    }

    private void exitDemonForm() {
        if (!transformed) return;

        transformed = false;
        caster.getTags().remove(DEMON_FORM_TAG);

        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).removeModifier(damageModifier);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(speedModifier);
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).removeModifier(resistanceModifier);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(resistanceModifier);

        if (auraEffect != null) {
            auraEffect.cancel();
        }

        removeAuraSwords();
        playExitEffect();
    }

    private void removeAuraSwords() {
        for (ItemDisplay sword : auraSwords) {
            if (sword != null && sword.isValid()) {
                Location loc = sword.getLocation();
                loc.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(BLADEWEAVER_PRIMARY, 1.0f));
                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1);
                sword.remove();
            }
        }
        auraSwords.clear();
    }

    private void playExitEffect() {
        Location loc = caster.getLocation();

        Particle.DustOptions fadingDust = new Particle.DustOptions(BLADEWEAVER_PRIMARY, 1.2f);
        caster.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 30, 0.5, 1, 0.5, 0, fadingDust);
        caster.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.05);

        caster.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, 0.6f, 1.5f);
        caster.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.0f);
    }

    @Override
    protected void cleanup() {
        exitDemonForm();
        super.cleanup();
    }
}


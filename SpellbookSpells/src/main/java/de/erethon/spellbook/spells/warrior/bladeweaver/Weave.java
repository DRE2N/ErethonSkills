package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.spellbook.utils.SpellbookFeedback;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;


/**
 * Weave - Ultimate transformation ability.
 * Requires full energy to cast. Transforms the Bladeweaver into demon form,
 * empowering all abilities and granting stat bonuses.
 * All other abilities gain enhanced demon form versions while active.
 */
public class Weave extends BladeweaverBaseSpell {

    private final int transformDuration = data.getInt("transformDuration", 300); // 15 seconds
    private final double damageBonus = Math.clamp(data.getDouble("damageBonus", 0.25), 0, 0.35);
    private final double speedBonus = Math.clamp(data.getDouble("speedBonus", 0.12), 0, 0.2);
    private final double resistanceBonus = Math.clamp(data.getDouble("resistanceBonus", 0.15), 0, 0.2);
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

    private boolean transformed = false;

    private TerrainRing terrainSigil;

    public Weave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = transformDuration;
        energyBuildUpFromUse = 0; // Ultimate doesn't build energy
    }

    @Override
    protected boolean onPrecast() {
        if (isInDemonForm()) {
            SpellbookFeedback.send(caster, "<red>Already in Demon Form!");
            return false;
        }

        if (caster.getEnergy() < energyRequired) {
            SpellbookFeedback.send(caster, "<red>Not enough energy! (" + (int)caster.getEnergy() + "/" + energyRequired + ")");
            return false;
        }

        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        caster.setEnergy(0);
        enterDemonForm();
        return true;
    }

    private void enterDemonForm() {
        transformed = true;
        caster.getTags().add(DEMON_FORM_TAG);

        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(damageModifier);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(speedModifier);
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(resistanceModifier);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(resistanceModifier);

        playTransformationEffect();
        createRazorSigil();
    }

    private void createRazorSigil() {
        if (SkillVfx.availableTo(caster)) {
            terrainSigil = new TerrainRing(caster.getLocation(), 1.65,
                    "terrain_band_bladeweaver");
        }
    }

    private void playTransformationEffect() {
        Location loc = caster.getLocation();
        bladeGroundFlash(loc.clone().add(0, 0.06, 0), "", 5.5f, 40);
        bladeFlash(loc.clone().add(0, 1.25, 0), "", 4.1f, 20);
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

    @Override
    protected void onTick() {
        super.onTick();

        if (!transformed) return;
        updateRazorSigil();
        if (currentTicks % 24 == 0) SkillSignatureVfx.accent(caster.getLocation().add(0, 1.5, 0),
                getClass(), "main", 2.8f, 22, false);
        if (currentTicks % 5 == 0) {
            spawnAmbientParticles();
        }

        if (currentTicks >= transformDuration - 60 && currentTicks % 20 == 0) {
            playWarningEffect();
        }
    }

    private void updateRazorSigil() {
        if (terrainSigil != null) {
            if (currentTicks % 2 == 0) terrainSigil.move(caster.getLocation());
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

        if (terrainSigil != null) {
            terrainSigil.close();
            terrainSigil = null;
        }
        playExitEffect();
    }

    private void playExitEffect() {
        Location loc = caster.getLocation();
        bladeGroundFlash(loc.clone().add(0, 0.06, 0), "", 3.2f, 16);

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


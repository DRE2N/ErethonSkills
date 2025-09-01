package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HuntersFocus extends AssassinBaseSpell {

    // Toggle ability. While active, you drain Focus per second. In return, your Charged Shot charges 50% faster,
    // and you gain "True Sight," allowing you to see invisible enemies within a 30-block radius.

    private final double minFocusDrain = data.getDouble("minFocusDrain", 10.0);
    private final double maxFocusDrain = data.getDouble("maxFocusDrain", 20.0);
    private final double trueSightRadius = data.getDouble("trueSightRadius", 30.0);
    private final String huntersFocusTag = "hunters_focus_active";

    public HuntersFocus(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = -1;
    }

    @Override
    protected boolean onPrecast() {
        if (caster.getTags().contains(huntersFocusTag)) {
            caster.removeScoreboardTag(huntersFocusTag);
            createDeactivationVFX();
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 0.8f);
            onTickFinish();
            return false;
        }

        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        caster.getTags().add(huntersFocusTag);
        createActivationVFX();
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.5f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 2.0f);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();

        if (!caster.getScoreboardTags().contains(huntersFocusTag)) {
            onTickFinish();
            return;
        }

        if (currentTicks % 20 == 0) {
            double drainAmount = Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, minFocusDrain, maxFocusDrain, "focusDrain");

            if (caster.getEnergy() < drainAmount) {
                caster.removeScoreboardTag(huntersFocusTag);
                createDeactivationVFX();
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 0.8f);
                onTickFinish();
                return;
            }

            caster.setEnergy((int) (caster.getEnergy() - drainAmount));
        }

        if (currentTicks % 10 == 0) {
            createTrueSightEffect();
            createActiveVFX();
        }

        if (currentTicks % 5 == 0) {
            revealInvisibleEnemies();
        }
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        caster.getTags().remove(huntersFocusTag);
    }

    private void createActivationVFX() {
        Location eyeLoc = caster.getEyeLocation();

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect activationSphere = new SphereEffect(effectManager);
            activationSphere.setLocation(eyeLoc);
            activationSphere.radius = 2.0f;
            activationSphere.particle = Particle.DUST;
            activationSphere.color = Color.ORANGE;
            activationSphere.particles = 30;
            activationSphere.duration = 20;
            activationSphere.start();
        }

        eyeLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, eyeLoc, 15, 0.5, 0.5, 0.5, 0.2);
        eyeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, eyeLoc, 8, 0.3, 0.3, 0.3, 0.1);
    }

    private void createDeactivationVFX() {
        Location eyeLoc = caster.getEyeLocation();
        eyeLoc.getWorld().spawnParticle(Particle.SMOKE, eyeLoc, 10, 0.3, 0.3, 0.3, 0.1);
        eyeLoc.getWorld().spawnParticle(Particle.DUST, eyeLoc, 8, 0.3, 0.3, 0.3, 0,
            new Particle.DustOptions(Color.GRAY, 1.0f));
    }

    private void createActiveVFX() {
        Location eyeLoc = caster.getEyeLocation();
        eyeLoc.getWorld().spawnParticle(Particle.DUST, eyeLoc, 3, 0.2, 0.2, 0.2, 0,
            new Particle.DustOptions(Color.ORANGE, 1.2f));
    }

    private void createTrueSightEffect() {
        Location center = caster.getLocation();

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect trueSightSphere = new SphereEffect(effectManager);
            trueSightSphere.setLocation(center);
            trueSightSphere.radius = (float) trueSightRadius;
            trueSightSphere.particle = Particle.DUST;
            trueSightSphere.color = Color.YELLOW;
            trueSightSphere.particles = 15;
            trueSightSphere.duration = 15;
            trueSightSphere.start();
        }
    }

    private void revealInvisibleEnemies() {
        for (LivingEntity entity : caster.getLocation().getNearbyLivingEntities(trueSightRadius)) {
            if (entity != caster && Spellbook.canAttack(caster, entity)) {
                if (entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(Color.RED, 1.5f));
                    entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.1);

                    entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
                }
            }
        }
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, minFocusDrain, maxFocusDrain, "focusDrain"), VALUE_COLOR));
        placeholderNames.add("focusDrain");
    }
}

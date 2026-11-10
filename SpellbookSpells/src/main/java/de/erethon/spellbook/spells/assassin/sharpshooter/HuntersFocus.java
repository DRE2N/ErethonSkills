package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class HuntersFocus extends SharpshooterBaseSpell {

    // Toggle ability. While active, you drain Focus per second. In return, your Charged Shot charges 50% faster,
    // and you gain "True Sight," allowing you to see invisible enemies within a 30-block radius.

    private final double minFocusDrain = data.getDouble("minFocusDrain", 10.0);
    private final double maxFocusDrain = data.getDouble("maxFocusDrain", 20.0);
    private final double trueSightRadius = data.getDouble("trueSightRadius", 30.0);
    private final String huntersFocusTag = "hunters_focus_active";
    private ItemDisplay sightDisplay;

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

            caster.setEnergy(Math.max(0, (int) (caster.getEnergy() - drainAmount)));
        }

        if (currentTicks % 10 == 0) {
            createActiveVFX();
        }

        updateSight();

        if (currentTicks % 5 == 0) {
            revealInvisibleEnemies();
        }
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        caster.getTags().remove(huntersFocusTag);
        if (sightDisplay != null && sightDisplay.isValid()) sightDisplay.remove();
        sightDisplay = null;
    }

    private void createActivationVFX() {
        sightDisplay = trackVfx(SkillVfx.billboard(caster.getLocation().add(0, 2.25, 0),
                "sig_assassin_sharpshooter_huntersfocus_wake_f4", 1.6f));
        SkillSignatureVfx.accent(caster.getEyeLocation(), getClass(), "main", 2.2f, 18, false);
    }

    private void createDeactivationVFX() {
        if (sightDisplay != null && sightDisplay.isValid()) sightDisplay.remove();
        sightDisplay = null;
        SkillSignatureVfx.accent(caster.getEyeLocation(), getClass(), "wake", 1.7f, 14, false);
    }

    private void createActiveVFX() {
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "lead", 1.4f, 12, true);
    }

    private void updateSight() {
        if (sightDisplay != null && sightDisplay.isValid())
            sightDisplay.teleport(caster.getLocation().add(0, 2.25, 0));
    }
    private void revealInvisibleEnemies() {
        for (LivingEntity entity : caster.getLocation().getNearbyLivingEntities(trueSightRadius)) {
            if (entity != caster && Spellbook.canAttack(caster, entity)) {
                if (entity.isInvisible() || entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    if (currentTicks % 20 == 0) createRevealRune(entity);

                    entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
                }
            }
        }
    }

    private void createRevealRune(LivingEntity entity) {
        Location center = entity.getLocation().add(0, entity.getHeight() + 0.45, 0);
        SkillSignatureVfx.accent(center, getClass(), "lead", 2.3f, 18, false);
    }
    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, minFocusDrain, maxFocusDrain, "focusDrain"), VALUE_COLOR));
        placeholderNames.add("focusDrain");
    }
}

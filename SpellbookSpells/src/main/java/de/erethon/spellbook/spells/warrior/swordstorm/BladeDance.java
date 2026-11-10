package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BladeDance extends SwordstormBaseSpell {

    // For several seconds, the Swordstorm enters a focused trance. Each basic attack during Blade Dance unleashes a secondary shockwave of energy that damages
    // all other enemies in a small radius around the primary target, Scaling with phys damage The model size is reduced and speed and jump height is increased, allowing the Swordstorm to move quickly between targets.
    // The Swordstorms has reduced resistance during the ability. Cannot use other abilities during Blade Dance.

    private final double secondaryDamageRadiusMin = data.getDouble("secondaryDamageRadiusMin", 3.0);
    private final double secondaryDamageRadiusMax = data.getDouble("secondaryDamageRadiusMax", 6.0);
    private final double minSecondaryDamageMultiplier = data.getDouble("secondaryDamageMultiplierMin", 0.5);
    private final double maxSecondaryDamageMultiplier = data.getDouble("secondaryDamageMultiplierMax", 1.5);

    private final AttributeModifier resistanceModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:warrior_blade_dance"), data.getDouble("resistanceMultiplier", 0.66) - 1.0, AttributeModifier.Operation.ADD_SCALAR);
    private final AttributeModifier sizeModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:warrior_blade_dance_size"), data.getDouble("sizeMultiplier", -0.66 ), AttributeModifier.Operation.ADD_SCALAR);
    private final AttributeModifier speedModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:warrior_blade_dance_speed"), data.getDouble("speedMultiplier", 1.05), AttributeModifier.Operation.ADD_SCALAR);
    private final AttributeModifier jumpModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:warrior_blade_dance_jump"), data.getDouble("jumpMultiplier", 1.2), AttributeModifier.Operation.ADD_SCALAR);

    private double secondaryDamageRadius;
    private TerrainRing tranceRing;

    public BladeDance(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && !caster.getTags().contains("swordstorm.bladedance");
    }

    @Override
    public boolean onCast() {
        caster.getTags().add("swordstorm.bladedance");
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(resistanceModifier);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(resistanceModifier);
        caster.getAttribute(Attribute.SCALE).addTransientModifier(sizeModifier);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(speedModifier);
        caster.getAttribute(Attribute.JUMP_STRENGTH).addTransientModifier(jumpModifier);
        caster.getAttribute(Attribute.SAFE_FALL_DISTANCE).addTransientModifier(jumpModifier);

        createTranceStructure();
        caster.getLocation().getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.RECORDS, 0.2f, 0.5f);
        secondaryDamageRadius = Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, secondaryDamageRadiusMin, secondaryDamageRadiusMax, "secondaryDamageRadius");
        return super.onCast();
    }

    @Override
    protected void onTick() {
        updateTranceStructure();
        super.onTick();
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        playRadialBladeWave(target);
        caster.getLocation().getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.RECORDS, 0.2f, 0.5f);
        for (LivingEntity livingEntity : target.getWorld().getNearbyLivingEntities(target.getLocation(), secondaryDamageRadius, secondaryDamageRadius)) {
            if (!Spellbook.canAttack(caster, livingEntity) || livingEntity == target || livingEntity == caster) {
                continue;
            }
            double damageMultiplier = Spellbook.getRangedValue(data, caster, livingEntity, Attribute.ADVANTAGE_PHYSICAL, minSecondaryDamageMultiplier, maxSecondaryDamageMultiplier, "secondaryDamageMultiplier");
            double secondaryDamage = damage * damageMultiplier;
            livingEntity.damage(secondaryDamage, caster, type);
        }
        return super.onAttack(target, damage, type);
    }

    private void createTranceStructure() {
        tranceRing = new TerrainRing(caster.getLocation(), 1.75,
                "terrain_band_swordstorm", 32, 0.28f);
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "main", 2.8f, 20, true);
    }

    private void updateTranceStructure() {
        if (tranceRing != null && currentTicks % 4 == 0)
            tranceRing.move(caster.getLocation());
    }

    private void playRadialBladeWave(LivingEntity target) {
        SkillSignatureVfx.accent(target.getLocation(), getClass(), "wake",
                (float) (secondaryDamageRadius * 1.7), 10, true);
    }
    @Override
    protected void cleanup() {
        playTranceCollapse();
        for (Player player : caster.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(caster.getLocation()) <= 32 * 32) {
                player.stopSound(Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.RECORDS);
            }
        }
        caster.getTags().remove("swordstorm.bladedance");
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).removeModifier(resistanceModifier);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(resistanceModifier);
        caster.getAttribute(Attribute.SCALE).removeModifier(sizeModifier);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(speedModifier);
        caster.getAttribute(Attribute.JUMP_STRENGTH).removeModifier(jumpModifier);
        caster.getAttribute(Attribute.SAFE_FALL_DISTANCE).removeModifier(jumpModifier);
        super.cleanup();
    }

    private void playTranceCollapse() {
        if (tranceRing != null) tranceRing.close();
        tranceRing = null;
        if (caster != null && caster.isValid()) SkillSignatureVfx.accent(
                caster.getLocation(), getClass(), "wake", 2.8f, 12, true);
    }
    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, secondaryDamageRadiusMin, secondaryDamageRadiusMax, "secondaryDamageRadius"), VALUE_COLOR));
        placeholderNames.add("secondaryDamageRadius");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, minSecondaryDamageMultiplier, maxSecondaryDamageMultiplier, "secondaryDamageMultiplier")));
        placeholderNames.add("secondaryDamageMultiplier");
        super.addSpellPlaceholders();
    }
}

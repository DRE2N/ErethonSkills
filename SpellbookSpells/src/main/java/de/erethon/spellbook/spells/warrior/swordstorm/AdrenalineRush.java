package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.units.qual.C;

import java.util.HashSet;
import java.util.Set;

public class AdrenalineRush extends WarriorBaseSpell {

    // Instantly gain a large burst of Fury and Power for a moderate duration.
    // If below 30% health when activated: Also gain Stability and Resistance for a short duration.
    // The caster gains lifesteal for the next 5 seconds.
    // Clears all negative effects on the caster.

    private final double healthThreshold = data.getDouble("healthThreshold", 0.3);
    private final double lifestealAmount = data.getDouble("lifestealAmount", 0.1);
    private final int furyMinDuration = data.getInt("furyMinDuration", 3);
    private final int furyMaxDuration = data.getInt("furyMaxDuration", 5);
    private final int powerMinDuration = data.getInt("powerMinDuration", 3);
    private final int powerMaxDuration = data.getInt("powerMaxDuration", 5);
    private final int stabilityMinDuration = data.getInt("stabilityMinDuration", 10);
    private final int stabilityMaxDuration = data.getInt("stabilityMaxDuration", 15);
    private final int resistanceMinDuration = data.getInt("resistanceMinDuration", 10);
    private final int resistanceMaxDuration = data.getInt("resistanceMaxDuration", 20);

    private final EffectData furyEffect = Spellbook.getEffectData("Fury");
    private final EffectData powerEffect = Spellbook.getEffectData("Power");
    private final EffectData stabilityEffect = Spellbook.getEffectData("Stability");
    private final EffectData resistanceEffect = Spellbook.getEffectData("Resistance");

    public AdrenalineRush(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && !caster.getTags().contains("swordstorm.bladedance");
    }

    @Override
    public boolean onCast() {
        caster.addEffect(caster, furyEffect, (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, furyMinDuration, furyMaxDuration, "furyDuration") * 20, 1);
        caster.addEffect(caster, powerEffect, (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, powerMinDuration, powerMaxDuration, "powerDuration") * 20, 1);
        if (caster.getHealth() / caster.getAttribute(Attribute.MAX_HEALTH).getValue() < healthThreshold) {
            caster.addEffect(caster, stabilityEffect, (int) Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_PHYSICAL, stabilityMinDuration, stabilityMaxDuration, "stabilityDuration") * 20, 1);
            caster.addEffect(caster, resistanceEffect, (int) Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_PHYSICAL, resistanceMinDuration, resistanceMaxDuration, "resistanceDuration") * 20, 1);
        }
        Set<EffectData> toRemove = new HashSet<>();
        for (SpellEffect effect : caster.getEffects()) {
            if (!effect.data.isPositive()) {
                toRemove.add(effect.data);
            }
        }
        toRemove.forEach(caster::removeEffect);
        World world = caster.getWorld();
        world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, caster.getLocation(), 20, 2, 0.5, 2);
        world.spawnParticle(Particle.SPLASH, caster.getLocation(), 5, 1, 0.5, 1);
        world.playSound(caster.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, SoundCategory.RECORDS, 0.7f, 1.5f);
        return super.onCast();
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        caster.heal(damage * lifestealAmount);
        World world = caster.getWorld();
        world.spawnParticle(Particle.HEART, caster.getLocation(), 3, 0.5, 0.5, 0.5);
        world.spawnParticle(Particle.WITCH, caster.getLocation(), 5, 1, 0.5, 1);
        return super.onAttack(target, damage, type);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, furyMinDuration, furyMaxDuration, "furyDuration"), VALUE_COLOR));
        placeholderNames.add("furyDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, powerMinDuration, powerMaxDuration, "powerDuration"), VALUE_COLOR));
        placeholderNames.add("powerDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_PHYSICAL, stabilityMinDuration, stabilityMaxDuration, "stabilityDuration"), VALUE_COLOR));
        placeholderNames.add("stabilityDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_PHYSICAL, resistanceMinDuration, resistanceMaxDuration, "resistanceDuration"), VALUE_COLOR));
        placeholderNames.add("resistanceDuration");
    }
}

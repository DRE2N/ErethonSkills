package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class LightOfPurification extends PaladinBaseSpell {

    // The Guardian channels a circle of light, removing negative effects from allies and applying a resistance effect.
    // The Guardian gains devotion for each effect removed.

    private final int range = data.getInt("range", 5);
    private final int effectsToRemove = data.getInt("effectsToRemove", 3);
    private final int devotionPerEffect = data.getInt("devotionPerEffect", 5);
    private final int resistanceDurationMin = data.getInt("resistanceDurationMin", 12) * 20;
    private final int resistanceDurationMax = data.getInt("resistanceDurationMax", 30) * 20;
    private final int purificationDuration = data.getInt("purificationDuration", 80);

    private final EffectData resistanceEffect = Spellbook.getEffectData("Resistance");

    public LightOfPurification(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        int devotionGained = 0;

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.RECORDS, 1.0f, 0.5f);

        AoE purificationZone = createCircularAoE(caster.getLocation(), range, 1, purificationDuration)
                .onTick(aoe -> {
                    if (currentTicks % 20 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.END_ROD,
                                aoe.getCenter().add(0, 0.5, 0),
                                8, range * 0.7, 0.5, range * 0.7, 0.05);
                    }
                    if (currentTicks % 10 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.INSTANT_EFFECT,
                                aoe.getCenter().add(0, 0.1, 0),
                                3, range * 0.8, 0, range * 0.8, 0);
                    }
                })
                .addBlockChange(Material.BLUE_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.WHITE_CONCRETE, Material.BLUE_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS, Material.WHITE_STAINED_GLASS)
                .sendBlockChanges();

        for (LivingEntity target : caster.getLocation().getNearbyLivingEntities(range)) {
            if (Spellbook.canAttack(caster, target)) continue;
            if (target == caster) continue;

            int effectsToRemoveCount = this.effectsToRemove;
            Set<EffectData> toRemove = new HashSet<>();
            for (SpellEffect effect : target.getEffects()) {
                if (effect.data == null) continue;
                if (effect.data.isPositive()) continue;
                if (effectsToRemoveCount <= 0) break;
                toRemove.add(effect.data);
                effectsToRemoveCount--;
            }

            for (EffectData effect : toRemove) {
                target.removeEffect(effect);
                devotionGained += devotionPerEffect;
            }

            int resistanceDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.RESISTANCE_MAGICAL, resistanceDurationMin, resistanceDurationMax, "resistanceDuration");
            target.addEffect(caster, resistanceEffect, resistanceDuration, 1);

            target.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    target.getLocation().add(0, 1, 0),
                    5, 0.5, 1, 0.5, 0.02);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, SoundCategory.RECORDS, 1.0f, 1.2f);
        }

        caster.setEnergy(caster.getEnergy() + devotionGained);
        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, resistanceDurationMin, resistanceDurationMax, "resistanceDuration") / 20, VALUE_COLOR));
        placeholderNames.add("resistanceDuration");
    }
}

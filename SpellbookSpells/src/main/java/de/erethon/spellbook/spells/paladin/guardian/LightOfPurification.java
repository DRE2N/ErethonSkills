package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
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
    private final int resistanceDurationMin = data.getInt("resistanceDurationMin", 120);
    private final int resistanceDurationMax = data.getInt("resistanceDurationMax", 300);

    private final EffectData resistanceEffect = Spellbook.getEffectData("Resistance");

    public LightOfPurification(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data); // 25
    }

    @Override
    public boolean onCast() {
        CircleEffect circleEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circleEffect.radius = range;
        circleEffect.particle = Particle.DUST;
        circleEffect.particleCount = 32;
        circleEffect.setLocation(caster.getLocation().clone().add(0, 1, 0));
        circleEffect.duration = 20;
        circleEffect.start();
        int devotionGained = 0;
        for (LivingEntity target : caster.getLocation().getNearbyLivingEntities(range)) {
            if (Spellbook.canAttack(caster, target)) continue;
            int effectsToRemove = this.effectsToRemove;
            Set<EffectData> toRemove = new HashSet<>();
            for (SpellEffect effect : target.getEffects()) {
                if (effect.data == null) continue;
                if (effect.data.isPositive()) continue;
                if (effectsToRemove <= 0) break;
                toRemove.add(effect.data);
                effectsToRemove--;
            }
            for (EffectData effect : toRemove) {
                target.removeEffect(effect);
                devotionGained += devotionPerEffect;
            }
            int resistanceDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.RESISTANCE_MAGICAL, resistanceDurationMin, resistanceDurationMax, "resistanceDuration");
            target.addEffect(caster, resistanceEffect, resistanceDuration, 1);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation(), 3, 0.5, 0.5, 0.5);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, SoundCategory.RECORDS, 1.0f, 1.0f);
        }
        caster.setEnergy(caster.getEnergy() + devotionGained);
        return super.onCast();
    }
}

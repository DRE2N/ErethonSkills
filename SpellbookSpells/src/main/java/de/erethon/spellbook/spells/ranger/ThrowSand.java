package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;

public class ThrowSand extends RangerBaseSpell {

    EffectData blindness = Spellbook.getEffectData("Blindness");

    public ThrowSand(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    protected boolean onCast() {
        target.addEffect(caster, blindness, data.getInt("duration", 200), 1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, SoundCategory.RECORDS, 1, 1);
        LineEffect effect = new LineEffect(Spellbook.getInstance().getEffectManager());
        effect.iterations = 1;
        effect.duration = 10 * 50;
        effect.particle = Particle.REDSTONE;
        effect.particleSize = 0.5f;
        effect.color = Color.YELLOW;
        effect.start();
        return super.onCast();
    }
}

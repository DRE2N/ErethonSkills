package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class ThrowSand extends RangerBaseSpell {

    private final int effectDuration = data.getInt("effectDuration", 200);
    private final EffectData blindness = Spellbook.getEffectData("Blindness");

    public ThrowSand(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        target.addEffect(caster, blindness, effectDuration, 1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, SoundCategory.RECORDS, 1, 1);
        LineEffect effect = new LineEffect(Spellbook.getInstance().getEffectManager());
        effect.iterations = 1;
        effect.duration = 10 * 50;
        effect.particle = Particle.DUST;
        effect.particleSize = 0.5f;
        effect.color = Color.YELLOW;
        effect.start();
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(effectDuration, VALUE_COLOR));
        placeholderNames.add("effect duration");
        return super.getPlaceholders(c);
    }
}

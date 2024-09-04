package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class CaptivatingBlow extends PaladinBaseSpell {

    private final int duration = data.getInt("baseDuration", 10);
    private final int stacks = data.getInt("stacks", 1);

    private final EffectData slowness = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");

    public CaptivatingBlow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(2);
    }

    @Override
    public boolean onCast() {
        target.addEffect(caster, slowness, (int) (duration + Spellbook.getScaledValue(data, target, Attribute.ADV_PHYSICAL)), stacks);
        target.playSound(Sound.sound(org.bukkit.Sound.BLOCK_CHAIN_BREAK, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_CHAIN_BREAK, Sound.Source.RECORD, 0.8f, 1));
        CircleEffect circle = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circle.particle = Particle.DUST;
        circle.radius = 1.2f;
        circle.color = Color.GRAY;
        circle.particleCount = 8;
        circle.duration = 20 * 50;
        circle.iterations = -1;
        circle.setEntity(target);
        circle.start();
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text((int) (duration + Spellbook.getScaledValue(data, caster, Attribute.ADV_PHYSICAL)), ATTR_PHYSICAL_COLOR));
        placeholderNames.add("effect duration");
        spellAddedPlaceholders.add(Component.text(stacks, VALUE_COLOR));
        placeholderNames.add("stacks");
        return super.getPlaceholders(c);
    }
}

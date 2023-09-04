package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class CaptivatingBlow extends PaladinBaseSpell {

    private final EffectData slowness = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");

    public CaptivatingBlow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(2);
    }

    @Override
    protected boolean onCast() {
        target.addEffect(caster, slowness, (int) (data.getInt("baseDuration", 20) + Math.round(Spellbook.getScaledValue(data, target, Attribute.ADV_PHYSICAL))), data.getInt("stacks", 1));
        target.playSound(Sound.sound(org.bukkit.Sound.BLOCK_CHAIN_BREAK, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_CHAIN_BREAK, Sound.Source.RECORD, 0.8f, 1));
        CircleEffect circle = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circle.particle = Particle.REDSTONE;
        circle.radius = 1.2f;
        circle.color = Color.GRAY;
        circle.particleCount = 8;
        circle.duration = 20 * 50;
        circle.iterations = -1;
        circle.setEntity(target);
        circle.start();
        return super.onCast();
    }
}

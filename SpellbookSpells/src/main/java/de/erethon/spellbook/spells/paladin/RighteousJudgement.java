package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class RighteousJudgement extends PaladinBaseSpell {

    private final EffectData weakness = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Weakness");

    public RighteousJudgement(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(2);
    }

    @Override
    protected boolean onCast() {
        target.addEffect(caster, weakness, (int) (data.getInt("baseDuration", 20) + Math.round(Spellbook.getScaledValue(data, target, Attribute.ADV_MAGIC))), data.getInt("stacks", 1));
        target.playSound(Sound.sound(org.bukkit.Sound.ENTITY_WITHER_SPAWN, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_WITHER_SPAWN, Sound.Source.RECORD, 0.8f, 1));
        CircleEffect circle = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circle.particle = Particle.REVERSE_PORTAL;
        circle.radius = 1.5f;
        circle.particleCount = 16;
        circle.duration = 40 * 50;
        circle.iterations = -1;
        circle.setEntity(target);
        circle.start();
        return super.onCast();
    }
}

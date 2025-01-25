package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class RighteousJudgement extends PaladinBaseSpell {

    private final int baseDuration = data.getInt("baseDuration", 20);
    private final int stacks = data.getInt("stacks", 1);

    private final EffectData weakness = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Weakness");

    public RighteousJudgement(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(2);
    }

    @Override
    public boolean onCast() {
        target.addEffect(caster, weakness, (int) (baseDuration + Spellbook.getScaledValue(data, caster, Attribute.ADVANTAGE_MAGICAL)), stacks);
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

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(baseDuration + Spellbook.getScaledValue(data, caster, Attribute.ADVANTAGE_MAGICAL), ATTR_PHYSICAL_COLOR));
        placeholderNames.add("effect duration");
        spellAddedPlaceholders.add(Component.text(stacks, VALUE_COLOR));
        placeholderNames.add("stacks");
        return super.getPlaceholders(c);
    }
}

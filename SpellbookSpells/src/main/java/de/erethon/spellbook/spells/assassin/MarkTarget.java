package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class MarkTarget extends AssassinBaseSpell {

    private final int duration = data.getInt("duration", 10);
    private final double critDmgBonus = data.getDouble("critDmgBonus", 1.0);

    public MarkTarget(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        tickInterval = 5;
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    protected void onTick() {
        caster.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, target.getLocation().add(0, 2, 0), 5);
    }

    @Override
    public double onAttack(LivingEntity entity, double damage, PDamageType type) {
        if (entity == target) {
            damage += caster.getAttribute(Attribute.STAT_CRIT_DMG).getValue() + critDmgBonus;
            caster.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 5);
            caster.playSound(Sound.sound(Key.key("entity.generic.attack.crit"), Sound.Source.RECORD, 1.0f, 1.0f));
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        spellAddedPlaceholders.add(Component.text(caster.getAttribute(Attribute.STAT_CRIT_DMG).getValue() + critDmgBonus, VALUE_COLOR));
        return super.getPlaceholders(caster);
    }
}


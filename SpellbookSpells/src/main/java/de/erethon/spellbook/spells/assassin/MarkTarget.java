package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class MarkTarget extends AssassinBaseSpell {

    public MarkTarget(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 10) * 20;
        tickInterval = 5;
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    protected void onTick() {
        caster.getWorld().spawnParticle(Particle.SPELL, target.getLocation().add(0, 2, 0), 5);
    }

    @Override
    public double onAttack(LivingEntity entity, double damage, DamageType type) {
        if (entity == target) {
            damage += caster.getAttribute(Attribute.STAT_CRIT_DMG).getValue() + data.getDouble("critDmgBonus", 1.0);
            caster.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 5);
            caster.playSound(Sound.sound(Key.key("entity.generic.attack.crit"), Sound.Source.RECORD, 1.0f, 1.0f));
        }
        return super.onAttack(target, damage, type);
    }
}


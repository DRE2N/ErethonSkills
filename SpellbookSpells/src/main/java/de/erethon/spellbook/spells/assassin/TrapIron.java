package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class TrapIron extends AssassinBaseTrap {

    public TrapIron(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    public boolean triggeredFirstTime = false;

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        keepAliveTicks = data.getInt("duration", 10);
        tickInterval = 20;
        return super.onCast();
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }

    @Override
    public void onTick() {
        super.onTick();
        Set<LivingEntity> targets = new HashSet<>();
        for (LivingEntity entity : getEntities()) {
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }
            targets.add(entity);
            entity.damage(Spellbook.getScaledValue(data, caster, entity, Attribute.ADV_PHYSICAL, damageMultiplier), caster, DamageType.PHYSICAL);
            triggerTraits(entity, 1);
        }
        if (!targets.isEmpty() && !triggeredFirstTime) {
            triggeredFirstTime = true;
            triggerTraits(targets);
            triggerTraits(2);
        }
    }
}

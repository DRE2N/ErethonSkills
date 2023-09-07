package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.entity.LivingEntity;

public class TrapExplosion extends AssassinBaseTrap {

    public TrapExplosion(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        keepAliveTicks = data.getInt("duration", 10) * 10;
        return super.onCast();
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }

    @Override
    protected void onEnter(LivingEntity entity) {
        if (entity == caster || !Spellbook.canAttack(caster, entity)) {
            return;
        }
        target.createExplosion((float) ((float) data.getDouble("power", 3.0) * damageMultiplier), false, false);
        triggerTraits(entity, 1);
        triggerTraits(2);
        keepAliveTicks = 1;
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }
}


package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.entity.LivingEntity;

public class TrapIron extends AoEBaseSpell {

    public TrapIron(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

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
        for (LivingEntity entity : getEntities()) {
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }
            entity.damage(data.getInt("damagePerTick", 1), caster, DamageType.PHYSICAL);
        }
    }
}

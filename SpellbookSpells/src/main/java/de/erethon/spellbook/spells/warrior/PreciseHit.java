package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class PreciseHit extends WarriorBaseSpell {

    protected int duration = data.getInt("duration", 400);
    public int BonusDamage = data.getInt("bonusDamage", 10);

    public PreciseHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration;
    }
    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        keepAliveTicks = 0;
        interrupt();
        return damage + BonusDamage;
    }
    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }
}







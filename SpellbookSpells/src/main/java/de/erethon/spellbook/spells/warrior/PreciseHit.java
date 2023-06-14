package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class PreciseHit extends SpellbookSpell {

    private int duration = 400;
    private int BonusDamage = 10;

    public PreciseHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration;
    }
    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        interrupt();
        return damage + BonusDamage;
    }
    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }
}






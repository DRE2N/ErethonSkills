package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.traits.assassin.saboteur.TrapTrackingTrait;
import org.bukkit.entity.LivingEntity;

public class AssassinBaseTrap extends AoEBaseSpell {

    public double damageMultiplier = 1.0;

    protected TrapTrackingTrait trapTrackingTrait;

    public AssassinBaseTrap(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        for (SpellTrait trait : caster.getActiveTraits()) {
            if (trait instanceof TrapTrackingTrait) {
                trapTrackingTrait = (TrapTrackingTrait) trait;
                break;
            }
        }
        if (trapTrackingTrait == null) {
            return false;
        }
        trapTrackingTrait.addTrap(this);
        return super.onCast();
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        if (trapTrackingTrait != null) {
            trapTrackingTrait.removeTrap(this);
        }
    }
}

package de.erethon.spellbook.spells.priest.passive;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;

/**
 * @author Fyreum
 */
public class PriestPassiveManaGain extends PassiveSpell {

    public PriestPassiveManaGain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 10;
    }

    @Override
    protected boolean onCast() {
        caster.addEnergy(2);
        return true;
    }

    @Override
    protected void onTick() {
        onCast();
    }
}

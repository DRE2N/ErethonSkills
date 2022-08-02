package de.erethon.spellbook.spells.assassin.passive;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class AssassinPassiveEnergyGain extends PassiveSpell {
    public AssassinPassiveEnergyGain(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        caster.addEnergy(5);
        return true;
    }

    @EventHandler
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().equals(caster.getEntity())) {
            onCast();
        }
    }

}

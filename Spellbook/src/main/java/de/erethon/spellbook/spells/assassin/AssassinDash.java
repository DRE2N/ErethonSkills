package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.Location;

public class AssassinDash extends AssassinBaseSpell {

    public AssassinDash(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        Location location = caster.getEntity().getLocation();
        location.setYaw(-10);
        caster.getEntity().setVelocity(location.getDirection().multiply(data.getDouble("dashMultiplier", 2.0)));
        return true;
    }

}


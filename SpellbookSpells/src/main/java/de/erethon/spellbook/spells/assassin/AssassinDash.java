package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class AssassinDash extends AssassinBaseSpell {

    public AssassinDash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        Location location = caster.getLocation();
        location.setYaw(-10);
        caster.setVelocity(location.getDirection().multiply(data.getDouble("dashMultiplier", 2.0)));
        return true;
    }

}


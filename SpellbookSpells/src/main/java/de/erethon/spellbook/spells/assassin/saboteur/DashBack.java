package de.erethon.spellbook.spells.assassin.saboteur;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class DashBack extends AssassinBaseSpell {


    public DashBack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Location location = caster.getLocation();
        location.setPitch(-10);
        caster.setVelocity(location.getDirection().multiply(data.getDouble("dashMultiplier", 2.0) * -1));
        return super.onCast();
    }

}


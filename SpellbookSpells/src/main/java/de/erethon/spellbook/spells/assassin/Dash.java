package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class Dash extends AssassinBaseSpell {

    public Dash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        Location location = caster.getLocation();
        location.setPitch(-10);
        caster.setVelocity(location.getDirection().multiply(data.getDouble("dashMultiplier", 2.0)));
        triggerTraits(0);
        return super.onCast();
    }

}


package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class Dash extends SpellbookSpell {

    public Dash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        Location location = caster.getLocation();
        location.setPitch(-10);
        caster.setVelocity(location.getDirection().multiply(data.getDouble("dashMultiplier", 2.0)));
        triggerTraits(0);
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }
}


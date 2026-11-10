package de.erethon.spellbook.traits.assassin.shadow;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

/** Holds the visible, destructible return anchor across the two Echo presses. */
public class ShadowEchoReturnTrait extends SpellTrait {

    private Location returnLocation;
    private ArmorStand anchor;

    public ShadowEchoReturnTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    public void setAnchor(Location location, ArmorStand stand) {
        returnLocation = location.clone();
        anchor = stand;
    }

    public boolean isWaitingForReturn() {
        return anchor != null && anchor.isValid() && !anchor.isDead();
    }

    public Location getReturnLocation() {
        return returnLocation == null ? null : returnLocation.clone();
    }

    public ArmorStand getAnchor() {
        return anchor;
    }

    public void finishReturn() {
        if (anchor != null && anchor.isValid()) anchor.remove();
        anchor = null;
        returnLocation = null;
    }
}

package de.erethon.spellbook.traits.assassin.sharpshooter;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class KineticLauncherTrait extends SpellTrait {

    private Location targetLocation;
    private boolean hasLaunched = false;
    private boolean hasArrived = false;

    public KineticLauncherTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    public void setTargetLocation(Location targetLocation) {
        this.targetLocation = targetLocation;
    }

    public Location getTargetLocation() {
        return targetLocation;
    }

    public boolean hasLaunched() {
        return hasLaunched;
    }

    public void setHasLaunched(boolean hasLaunched) {
        this.hasLaunched = hasLaunched;
    }

    public boolean hasArrived() {
        return hasArrived;
    }

    public void setHasArrived(boolean hasArrived) {
        this.hasArrived = hasArrived;
    }
}

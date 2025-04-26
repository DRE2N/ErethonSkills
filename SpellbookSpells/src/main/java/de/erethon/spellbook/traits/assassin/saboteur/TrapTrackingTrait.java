package de.erethon.spellbook.traits.assassin.saboteur;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.AssassinBaseTrap;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class TrapTrackingTrait extends SpellTrait {

    private Set<AssassinBaseTrap> traps = new HashSet<>();


    public TrapTrackingTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    public void addTrap(AssassinBaseTrap trap) {
        traps.add(trap);
    }

    public void removeTrap(AssassinBaseTrap trap) {
        traps.remove(trap);
    }

    public Set<AssassinBaseTrap> getTraps() {
        return traps;
    }

    public AssassinBaseTrap getClosestTrap(Location location) {
        AssassinBaseTrap closestTrap = null;
        double closestDistance = Double.MAX_VALUE;

        for (AssassinBaseTrap trap : traps) {
            double distance = trap.target.distance(location);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestTrap = trap;
            }
        }
        return closestTrap;
    }

}

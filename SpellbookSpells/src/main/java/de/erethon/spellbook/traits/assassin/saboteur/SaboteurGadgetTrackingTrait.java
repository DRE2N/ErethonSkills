package de.erethon.spellbook.traits.assassin.saboteur;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class SaboteurGadgetTrackingTrait extends SpellTrait {

    private final Set<SaboteurGadget> trackedGadgets = new HashSet<>();

    public SaboteurGadgetTrackingTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTick() {
        super.onTick();
        for (SaboteurGadget gadget : trackedGadgets) {
            if (gadget.isExpired()) {
                gadget.remove();
                trackedGadgets.remove(gadget);
            }
        }
    }

    public void addGadget(SaboteurGadget gadget) {
        trackedGadgets.add(gadget);
        gadget.display().setPersistent(false); // Just to be sure
    }

    public void removeGadget(SaboteurGadget gadget) {
        trackedGadgets.remove(gadget);
    }

    public Set<SaboteurGadget> getTrackedGadgets() {
        return trackedGadgets;
    }

    public Set<SaboteurGadget> getMines() {
        Set<SaboteurGadget> mines = new HashSet<>();
        for (SaboteurGadget gadget : trackedGadgets) {
            if (gadget.type() == SaboteurGadgetType.MINE) {
                mines.add(gadget);
            }
        }
        return mines;
    }

    public Set<SaboteurGadget> getTraps() {
        Set<SaboteurGadget> traps = new HashSet<>();
        for (SaboteurGadget gadget : trackedGadgets) {
            if (gadget.type() == SaboteurGadgetType.TRAP) {
                traps.add(gadget);
            }
        }
        return traps;
    }
}

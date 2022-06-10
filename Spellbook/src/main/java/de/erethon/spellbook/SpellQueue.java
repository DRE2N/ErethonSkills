package de.erethon.spellbook;

import de.erethon.spellbook.spells.ActiveSpell;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class SpellQueue extends BukkitRunnable {

    Spellbook spellbook;
    private final List<ActiveSpell> queue = new ArrayList<>();

    public SpellQueue(Spellbook spellbook) {
        this.spellbook = spellbook;
    }

    @Override
    public void run() {
        int i = 0;
        for (ActiveSpell skill : queue) {
            skill.cast();
            queue.remove(skill);
            i++;
            if (i >= 5) {
                break;
            }
        }
    }

    public void addToQueue(ActiveSpell spell) {
        queue.add(spell);
    }

}

package de.erethon.spellbook;

import de.erethon.spellbook.spells.ActiveSpell;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class SpellQueue extends BukkitRunnable {

    Spellbook spellbook;
    private final List<ActiveSpell> queue = new ArrayList<>();
    private final List<ActiveSpell> activeSpells = new ArrayList<>();

    public SpellQueue(Spellbook spellbook) {
        this.spellbook = spellbook;
    }

    @Override
    public void run() {
        int i = 0;
        for (ActiveSpell spell : activeSpells) {
            spell.tick();
            if (spell.shouldRemove()) {
                activeSpells.remove(spell);
            }
        }
        for (ActiveSpell spell : queue) {
            spell.ready();
            queue.remove(spell);
            activeSpells.add(spell);
            i++;
            if (i >= 5) {
                break;
            }
        }
    }

    public ActiveSpell addToQueue(ActiveSpell spell) {
        queue.add(spell);
        return spell;
    }

}

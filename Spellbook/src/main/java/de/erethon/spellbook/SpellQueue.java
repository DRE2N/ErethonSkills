package de.erethon.spellbook;

import org.bukkit.Server;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SpellQueue extends BukkitRunnable {

    Spellbook spellbook;
    private final List<ActiveSpell> queue = new ArrayList<>();
    private final List<ActiveSpell> activeSpells = new ArrayList<>();

    private final Server server;

    public SpellQueue(Spellbook spellbook) {
        this.spellbook = spellbook;
        this.server = spellbook.getImplementingPlugin().getServer();
    }

    @Override
    public void run() {
        int maxSpellsPerTickQueue = updateMaxSpellsPerTick();
        int i = 0;
        Iterator<ActiveSpell> activeSpellIterator = activeSpells.iterator();
        while (activeSpellIterator.hasNext()) {
            ActiveSpell activeSpell = activeSpellIterator.next();
            activeSpell.tick();
            if (activeSpell.shouldRemove()) {
                activeSpellIterator.remove();
            }
        }
        Iterator<ActiveSpell> queueIterator = queue.iterator();
        while (queueIterator.hasNext()) {
            ActiveSpell spell = queueIterator.next();
            spell.ready();
            queueIterator.remove();
            activeSpells.add(spell);
            i++;
            if (i >= maxSpellsPerTickQueue) {
                spellbook.getImplementingPlugin().getLogger().warning("Queue overflow! Queue size " + queue.size() + "/" + maxSpellsPerTickQueue + " | Tick time: " + server.getAverageTickTime());
                break;
            }
        }
    }

    private int updateMaxSpellsPerTick() {
        double tickTime = server.getAverageTickTime();
        if (tickTime > 50.0) {
            return 2;
        }
        if (tickTime > 40.0) {
            return 4;
        }
        if (tickTime > 30.0) {
            return 8;
        }
        if (tickTime > 20.0) {
            return 16;
        }
        return 32;
    }

    public ActiveSpell addToQueue(ActiveSpell spell) {
        queue.add(spell);
        return spell;
    }

}

package de.erethon.spellbook;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SpellQueue implements Listener {

    Spellbook spellbook;
    private final List<SpellbookSpell> queue = new ArrayList<>();
    private final List<SpellbookSpell> activeSpells = new ArrayList<>();

    private final Server server;

    public SpellQueue(Spellbook spellbook) {
        this.spellbook = spellbook;
        this.server = spellbook.getImplementingPlugin().getServer();
    }

    @EventHandler
    public void onTick(ServerTickEndEvent event) {
        if (event.getTimeRemaining() < 0) { // Don't execute spells if the tick is already over 50ms
            spellbook.getImplementingPlugin().getLogger().warning("Skipped SpellQueue because the tick is already over 50ms.");
            return;
        }
        run();
    }

    public void run() {
        int maxSpellsPerTickQueue = updateMaxSpellsPerTick();
        int i = 0;
        Iterator<SpellbookSpell> activeSpellIterator = activeSpells.iterator();
        while (activeSpellIterator.hasNext()) {
            SpellbookSpell spell = activeSpellIterator.next();
            if (spell.shouldRemove()) {
                activeSpellIterator.remove();
            } else {
                spell.tick();
            }
        }
        Iterator<SpellbookSpell> queueIterator = queue.iterator();
        while (queueIterator.hasNext()) {
            SpellbookSpell spell = queueIterator.next();
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

    public SpellbookSpell addToQueue(SpellbookSpell spell) {
        queue.add(spell);
        return spell;
    }

}

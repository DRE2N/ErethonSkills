package de.erethon.spellbook.spells;

import de.erethon.spellbook.SpellQueue;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class SpellData extends YamlConfiguration {
    Spellbook spellbook;
    SpellQueue queue;
    private int cooldown;
    private String id;
    private String name;
    private List<String> description = new ArrayList<>();

    public SpellData(Spellbook spellbook, String id) {
        this.spellbook = spellbook;
        this.id = id;
        queue = spellbook.getQueue();
        spellbook.getImplementingPlugin().getLogger().info("Created new SpellData with " + spellbook.getClass().getName() + " and queue ID " + queue.getTaskId());
    }
    /* Casting process:
    Player clicks button -> ActiveSpell is created and queued -> queue calls ActiveSpell#ready -> precast -> cast -> afterCast
    The precast method is called before the cast method. If it returns false, the cast method is not called and the spell
    is not cast. In the same way, the afterCast method will only be called if the cast method returned true.
    If the cast fails for any reason, a SpellError is set which the ActiveSpell will display to the player.


    A queue is used so that spells are cast in a consistent order and to limit the amount of spells that can be cast per tick globally.
    If we can ensure that all damage is handled using the queue, we could even run the queue in a separate thread. T
    he queue adds a slight delay to the cast process, even when its empty, but that is not noticeable for players and
    could be easily hidden by adding animations, if needed.

    The current implementation only works for spells that are actively cast by a SpellCaster. Passive spells likely
    would be implemented using some sort of "trigger" that listens to events and then runs the usual queue/precast/cast/afterCast process.

    Spells should be implemented by extending this class and overriding the precast, cast and afterCast methods.
     This class will only exist once. Casting will create an instance of ActiveSpell, which should be used to handle all casting-related logic.

    TODO:
    The design of ActiveSpell needs to be reevaluated. It should be possible to create an ActiveSpell that expires after
    a certain amount of time, or when the player stops casting, as well as spells that follow a target.
    The current implementation only really works well with instant spells.
     */

    /**
     * This should be used to check for prerequisites for the spell, such as mana, target, location, etc.
     * @param caster the caster of the spell
     * @param activeSpell the active spell instance
     * @return true if the spell can be cast, false otherwise
     */
    public abstract boolean precast(SpellCaster caster, ActiveSpell activeSpell);

    /**
     * This should be used to implement the spell itself.
     * @param caster the caster of the spell
     * @param activeSpell the active spell instance
     * @return true if the spell was successfully cast, false otherwise
     */
    public abstract boolean cast(SpellCaster caster, ActiveSpell activeSpell);

    /**
     * This should be used do execute code after the spell was cast, like removing mana.
     * @param caster the caster of the spell
     * @param activeSpell the active spell instance
     */
    public abstract void afterCast(SpellCaster caster, ActiveSpell activeSpell);

    public abstract void tick(SpellCaster caster, ActiveSpell activeSpell);

    public ActiveSpell queue(SpellCaster caster) {
         ActiveSpell activeSpell = new ActiveSpell(caster, this);
         queue.addToQueue(activeSpell);
         return activeSpell;
    }

    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public List<String> getDescription() {
        return description;
    }

    public int getCooldown() {
        return cooldown;
    }

    @Override
    public void save(@NotNull File file) throws IOException {
        super.save(file);
    }

    @Override
    public void load(@NotNull File file) throws FileNotFoundException, IOException, InvalidConfigurationException {
        super.load(file);
        cooldown = getInt("cooldown", 0);

    }
}

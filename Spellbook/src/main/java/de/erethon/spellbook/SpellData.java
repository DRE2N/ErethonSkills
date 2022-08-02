package de.erethon.spellbook;

import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SpellData extends YamlConfiguration {
    Spellbook spellbook;
    SpellQueue queue;
    private int cooldown;
    private String id;
    private String name;
    private List<String> description = new ArrayList<>();

    private Class<? extends ActiveSpell> spellClass;

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


    A queue is used so that spells are cast in a consistent order and to limit the amount of spells that can be cast per tick globally.
    If we can ensure that all damage is handled using the queue, we could even run the queue in a separate thread. T
    he queue adds a slight delay to the cast process, even when its empty, but that is not noticeable for players and
    could be easily hidden by adding animations, if needed.

    The current implementation only works for spells that are actively cast by a SpellCaster. Passive spells likely
    would be implemented using some sort of "trigger" that listens to events and then runs the usual queue/precast/cast/afterCast process.

    Spells should be implemented by extending this class and overriding the precast, cast and afterCast methods.
     This class will only exist once. Casting will create an instance of ActiveSpell, which should be used to handle all casting-related logic.

     */


    public ActiveSpell queue(SpellCaster caster) {
        ActiveSpell activeSpell;
        try {
            activeSpell = spellClass.getDeclaredConstructor(SpellCaster.class, SpellData.class).newInstance(caster, this);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            spellbook.getImplementingPlugin().getLogger().warning("Could not create ActiveSpell for spell " + id + " with class " + spellClass.getName());
            throw new RuntimeException(e);
        }
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
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        String className = getString("class");
        spellbook.getImplementingPlugin().getLogger().info(this.getClass().getPackageName() + ".spells." + className);
        try {
            spellClass = (Class<? extends ActiveSpell>) Class.forName(this.getClass().getPackageName() + ".spells." + className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        cooldown = getInt("cooldown", 0);

    }
}

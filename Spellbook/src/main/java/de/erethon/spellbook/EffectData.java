package de.erethon.spellbook;

import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class EffectData extends YamlConfiguration {

    enum StackMode {
        INTENSIFY,
        PROLONG,
    }

    Spellbook spellbook;
    SpellQueue queue;
    private final String id;

    private int maxDuration;
    private int maxStacks;

    private StackMode stackMode;

    private Class<? extends SpellEffect> effectClass;

    public EffectData(Spellbook spellbook, String id) {
        this.spellbook = spellbook;
        this.id = id;
        queue = spellbook.getQueue();
        spellbook.getImplementingPlugin().getLogger().info("Created new SpellData with " + spellbook.getClass().getName());
    }

    public String getIcon() {
        return getString("icon", "<red>E");
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public int getMaxStacks() {
        return maxStacks;
    }

    public StackMode getStackMode() {
        return stackMode;
    }

    public SpellEffect getActiveEffect(SpellCaster target, int duration, int stack) {
        SpellEffect spellEffect;
        try {
            spellEffect = effectClass.getDeclaredConstructor(EffectData.class, SpellCaster.class, Integer.class, Integer.class).newInstance(this, target, duration, stack);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            spellbook.getImplementingPlugin().getLogger().warning("Could not create class for effect " + id + ": " + effectClass.getName());
            throw new RuntimeException(e);
        }
        return spellEffect;
    }

    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        String className = getString("class");
        try {
            effectClass = (Class<? extends SpellEffect>) Class.forName(this.getClass().getPackageName() + ".effects." + className);
        } catch (ClassNotFoundException e1) {
            spellbook.getImplementingPlugin().getLogger().warning("Could not find class for effect " + id + ": " + className);
            throw new RuntimeException(e1);
        }
    }
}

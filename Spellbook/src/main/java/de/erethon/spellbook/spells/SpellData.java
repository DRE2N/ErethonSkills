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
    }

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

    }
}

package de.erethon.spellbook.spells;

import de.erethon.spellbook.SpellQueue;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Spell extends YamlConfiguration {
    Spellbook spellbook;
    SpellQueue queue;
    private int cooldown;
    private String name;
    private List<String> description = new ArrayList<>();
    private

    public Spell(Spellbook spellbook) {
        this.spellbook = spellbook;
        queue = spellbook.getQueue();
    }

    public void queue(SpellCaster caster) {
         queue.addToQueue(new ActiveSpell(caster, this));
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

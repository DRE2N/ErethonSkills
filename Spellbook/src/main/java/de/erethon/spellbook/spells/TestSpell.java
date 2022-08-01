package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TestSpell extends SpellData {

    private double dashMultiplier = 1;

    public TestSpell(Spellbook spellbook, String id) {
        super(spellbook, id);
    }

    @Override
    public boolean precast(SpellCaster caster, ActiveSpell activeSpell) {
        return true;
    }

    @Override
    public boolean cast(SpellCaster caster, ActiveSpell activeSpell) {
        Player player = (Player) caster.getEntity();
        player.sendMessage("TestSpell. Hui!");
        player.setVelocity(player.getLocation().getDirection().multiply(dashMultiplier));
        return true;
    }

    @Override
    public void afterCast(SpellCaster caster, ActiveSpell activeSpell) {
        caster.setCooldown(this);
    }

    @Override
    public void tick(SpellCaster caster, ActiveSpell activeSpell) {
    }


    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        getDouble("dashMultiplier", dashMultiplier);
    }
}

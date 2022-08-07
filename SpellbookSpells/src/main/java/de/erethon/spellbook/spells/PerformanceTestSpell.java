package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class PerformanceTestSpell extends SpellbookSpell {

    private int iterations = 0;

    public PerformanceTestSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        iterations = spellData.getInt("iterations", 100);
    }

    @Override
    protected boolean onCast() {
        keepAliveTicks = 10;
        return true;
    }


    @Override
    protected void onTickFinish() {
        SpellbookAPI api = Bukkit.getServer().getSpellbookAPI();
        SpellData spellbookSpell = api.getLibrary().getSpellByID("TestSpell");
        for (int i = 0; i < iterations; i++) {
            spellbookSpell.queue(caster);
        }
    }
}

package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.entity.Player;

public class TestSpell extends SpellData {

    public TestSpell(Spellbook spellbook, String id) {
        super(spellbook, id);
    }

    @Override
    public boolean precast(SpellCaster caster, ActiveSpell activeSpell) {
        return true;
    }

    @Override
    public boolean cast(SpellCaster caster, ActiveSpell activeSpell) {
        return true;
    }

    @Override
    public void afterCast(SpellCaster caster, ActiveSpell activeSpell) {
        Player player = (Player) caster.getEntity();
        player.sendMessage("TestSpell. Hui!");
        player.setVelocity(player.getLocation().getDirection().multiply(5));
    }

    @Override
    public void tick(SpellCaster caster, ActiveSpell activeSpell) {
    }
}

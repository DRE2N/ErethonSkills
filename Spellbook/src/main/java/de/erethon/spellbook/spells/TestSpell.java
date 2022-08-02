package de.erethon.spellbook.spells;

import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.entity.Player;

public class TestSpell extends SpellbookSpell {

    public TestSpell(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return true;
    }

    @Override
    protected boolean onCast() {
        Player player = (Player) caster.getEntity();
        player.sendMessage("TestSpell. Hui!");
        player.setVelocity(player.getLocation().getDirection().multiply(data.getDouble("dashMultiplier", 5.0)));
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.setCooldown(data);
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected void onTickFinish() {
    }
}

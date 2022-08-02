package de.erethon.spellbook.spells;

import de.erethon.spellbook.ActiveSpell;
import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.entity.Player;

public class TestSpell extends ActiveSpell {

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
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected void onTickFinish() {
    }
}

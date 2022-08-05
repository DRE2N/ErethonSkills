package de.erethon.spellbook.spells.assassin.passive;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.event.EventHandler;

/**
 * @author Fyreum
 */
public class AssassinDoubleJump extends PassiveSpell {

    public AssassinDoubleJump(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        caster.sendMessage("Jump!");
    }
}

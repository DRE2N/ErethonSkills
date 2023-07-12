package de.erethon.spellbook.spells;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class TestSpell extends SpellbookSpell implements Listener {

    public TestSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 2000;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());

    }

    @Override
    protected boolean onPrecast() {
        return true;
    }

    @Override
    protected boolean onCast() {
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.setCooldown(data);
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        event.getPlayer().showTitle(Title.title(net.kyori.adventure.text.Component.empty(), Component.text("JUMP", NamedTextColor.GREEN)));
    }


}

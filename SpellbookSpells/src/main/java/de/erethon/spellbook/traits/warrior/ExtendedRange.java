package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ExtendedRange extends SpellTrait implements Listener {

    private final int range = data.getInt("range", 5);

    public ExtendedRange(TraitData data, LivingEntity caster) {
        super(data, caster);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) return;
        if (!event.getPlayer().equals(caster)) return;
        Entity entity = caster.getTargetEntity(range);
        if (entity == null) return;
        if (entity instanceof LivingEntity living) {
            caster.attack(living);
        }
    }

}

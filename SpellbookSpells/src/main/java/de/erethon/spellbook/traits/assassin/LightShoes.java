package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class LightShoes extends SpellTrait implements Listener {

    private final int duration = data.getInt("duration", 120);
    private long lastKill = 0;

    public LightShoes(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null || event.getEntity().getKiller() != caster) return;
        caster.setInvisible(true);
        lastKill = System.currentTimeMillis();
    }

    @Override
    protected void onTick() {
        if (System.currentTimeMillis() - lastKill > duration * 50L) {
            caster.setInvisible(false);
        }
    }

    @Override
    protected void onAdd() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
    }
}

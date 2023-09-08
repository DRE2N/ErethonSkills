package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;

public class BrutalPassion extends SpellTrait implements Listener {

    private final int cooldownReductionPerKill = data.getInt("cooldownReductionPerKill", 5);

    public BrutalPassion(TraitData data, LivingEntity caster) {
        super(data, caster);
    }
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null || event.getEntity().getKiller() != caster) return;
        for (Map.Entry<SpellData, Long> entry : caster.getUsedSpells().entrySet()) {
            if (entry.getValue() + cooldownReductionPerKill * 1000 > System.currentTimeMillis()) {
                caster.getUsedSpells().put(entry.getKey(), entry.getValue() - cooldownReductionPerKill * 1000);
            }
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

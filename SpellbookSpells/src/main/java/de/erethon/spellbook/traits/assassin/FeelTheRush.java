package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class FeelTheRush extends SpellTrait implements Listener {

    private final int duration = data.getInt("duration", 120);
    private final int stacks = data.getInt("stacks", 1);
    private final EffectData effectData = Spellbook.getEffectData("Power");

    public FeelTheRush(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null || event.getEntity().getKiller() != caster) return;
        event.getEntity().getKiller().addEffect(caster, effectData, duration, stacks);
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

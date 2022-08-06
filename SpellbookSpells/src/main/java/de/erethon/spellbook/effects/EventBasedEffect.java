package de.erethon.spellbook.effects;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class EventBasedEffect extends SpellEffect implements Listener {

    public EventBasedEffect(EffectData data, LivingEntity target, int duration) {
        super(data, target, duration);
    }

    @Override
    public void onApply() {
        Bukkit.getServer().getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    public void onRemove() {
        HandlerList.unregisterAll(this);
    }
}

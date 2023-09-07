package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashSet;
import java.util.Set;

public class ClearHead extends SpellTrait implements Listener {

    public ClearHead(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null || event.getEntity().getKiller() != caster) return;
        Set<EffectData> toRemove = new HashSet<>();
        for (SpellEffect effect : caster.getEffects()) {
            if (!effect.data.isPositive()) {
                toRemove.add(effect.data);
            }
        }
        for (EffectData effect : toRemove) {
            caster.removeEffect(effect);
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

package de.erethon.spellbook.traits.paladin;

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

public class PathOfPurification extends SpellTrait implements Listener {

    private final double range = data.getDouble("range", 5);
    private final int duration = data.getInt("duration", 100);
    private final int stacks = data.getInt("stacks", 5);
    private final EffectData effectData = Spellbook.getEffectData("Power");

    public PathOfPurification(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != caster) return;
        caster.addEffect(caster, effectData, duration, stacks);
        for (LivingEntity living : event.getEntity().getLocation().getNearbyLivingEntities(range)) {
            if (living == caster) continue;
            if (Spellbook.canAttack(caster, living)) continue;
            living.addEffect(caster, effectData, duration, stacks);
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

package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class BlindnessEffect extends EventBasedEffect {

    double missChancePerStack = data.getDouble("missChancePerStack", 0.1);

    public BlindnessEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @EventHandler
    public void onDamageEvent(EntityDamageByEntityEvent event) {
        if (event.getDamager() != target) {
            return;
        }
        if (Math.random() < missChancePerStack * stacks) {
            event.setCancelled(true);
        }

    }

}

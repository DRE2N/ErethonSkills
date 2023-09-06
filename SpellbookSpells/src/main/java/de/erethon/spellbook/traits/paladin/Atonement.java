package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashSet;
import java.util.Set;

public class Atonement extends SpellTrait implements Listener {

    private final double range = data.getDouble("range", 5);

    public Atonement(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity().getLocation().distanceSquared(caster.getLocation()) > range * range) return;
        if (!Spellbook.canAttack(caster, event.getEntity())) return;
        Set<EffectData> toRemove = new HashSet<>();
        for (SpellEffect effect : caster.getEffects()) {
            if (effect.data.isPositive()) continue;
            toRemove.add(effect.data);
        }
        for (EffectData effect : toRemove) {
            caster.removeEffect(effect);
        }
    }


}

package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class ClimbingExpertise extends SpellTrait implements Listener {

    private final double damageReduction = data.getDouble("damageReductionPercent", 0.2);

    public ClimbingExpertise(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    public void onFall(EntityDamageEvent event) {
        if (event.getEntity() != caster || event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        event.setDamage(event.getDamage() * (1 - damageReduction));
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

package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class HeavyWarrior extends SpellTrait implements Listener {

    private final double damageReduction = data.getDouble("damageReduction", 0.9);
    private final int radius = data.getInt("radius", 2);
    private final double damage = data.getDouble("damage", 10);


    public HeavyWarrior(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    private void onDamage(EntityDamageEvent event) {
        if (event.getEntity() != caster) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        event.setDamage(event.getDamage() * damageReduction);
        caster.getLocation().getNearbyLivingEntities(radius).forEach(entity -> {
            if (entity == caster) return;
            if (!Spellbook.canAttack(caster, entity)) return;
            entity.damage(damage, caster);
        });
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_AXE_STRIP, SoundCategory.RECORDS,1f, 0.5f);
    }
}

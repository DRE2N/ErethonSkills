package de.erethon.spellbook.traits.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Explorer extends SpellTrait implements Listener {

    private final double damagePerBlockDistance = data.getDouble("damagePerBlockDistance", 0.5);
    private final double maxDamage = data.getDouble("maxDamage", 50);
    private Location lastDamageLocation = null;

    public Explorer(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (lastDamageLocation != null) {
            double distance = lastDamageLocation.distance(target.getLocation());
            double bonusDamage = distance * damagePerBlockDistance;
            if (bonusDamage > maxDamage) {
                bonusDamage = maxDamage;
            }
            damage += bonusDamage;
        }
        lastDamageLocation = target.getLocation();
        return super.onAttack(target, damage, type);
    }
    // We don't players to stack up damage by teleporting
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getPlayer() != caster) return;
        lastDamageLocation = null;
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

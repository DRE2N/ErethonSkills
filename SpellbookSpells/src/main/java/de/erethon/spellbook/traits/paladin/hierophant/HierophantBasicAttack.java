package de.erethon.spellbook.traits.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.combat.BossControl;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Confirmed glaive hits earn Wrath for a chosen spell, rather than passive hoarding damage. */
public class HierophantBasicAttack extends SpellTrait implements Listener {

    private final int wrathPerHit = data.getInt("wrathPerHit", 8);
    private int hits;

    public HierophantBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        caster.setMaxEnergy(100);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
        super.onRemove();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConfirmedGlaiveHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        if (event.getDamager() != caster || !(event.getEntity() instanceof LivingEntity target)
                || !Spellbook.canAttack(caster, target)
                || event.getDamageType() != PDamageType.PHYSICAL
                || source == null || source.type() != CombatSource.Type.BASIC_ATTACK
                || event.getFinalDamage() <= 0) return;
        caster.addEnergy(wrathPerHit);
        BossControl.apply(caster, target, 10);
        if (++hits < 3) return;
        hits = 0;
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT,
                0.65f, 1.4f);
        target.getWorld().spawnParticle(Particle.END_ROD, target.getEyeLocation(),
                8, 0.25, 0.35, 0.25, 0.02);
    }
}
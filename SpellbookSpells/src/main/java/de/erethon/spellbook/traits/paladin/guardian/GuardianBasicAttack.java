package de.erethon.spellbook.traits.paladin.guardian;

import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.HealingContext;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Confirmed weapon contacts build Devotion; the third returns a bounded self heal. */
public class GuardianBasicAttack extends SpellTrait implements Listener {

    private final int devotionPerAttack = data.getInt("devotionPerAttack", 5);
    private int hits;

    public GuardianBasicAttack(TraitData data, LivingEntity caster) {
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
    public void onConfirmedWeaponHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        if (event.getDamager() != caster || !(event.getEntity() instanceof LivingEntity target)
                || !Spellbook.canAttack(caster, target)
                || event.getDamageType() != PDamageType.PHYSICAL
                || source == null || source.type() != CombatSource.Type.BASIC_ATTACK
                || event.getFinalDamage() <= 0) return;
        caster.addEnergy(devotionPerAttack);
        BossControl.apply(caster, target, 5);
        if (++hits < 3) return;
        hits = 0;
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double healing = Math.min(caster.getMaxHealth() * 0.1, 5 + healingPower * 0.12);
        caster.heal(HealingContext.isPlayerCombat(caster, target) ? healing * 0.5 : healing);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                0.7f, 1.2f);
        caster.getWorld().spawnParticle(Particle.END_ROD, caster.getEyeLocation(),
                5, 0.3, 0.3, 0.3, 0.01);
    }
}
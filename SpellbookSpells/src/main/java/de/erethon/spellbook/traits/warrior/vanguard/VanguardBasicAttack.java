package de.erethon.spellbook.traits.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import de.erethon.spellbook.spells.warrior.vanguard.ShieldBreaker;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

/** Normal modern charge timing: a full axe hit earns impact, Momentum, and boss stagger. */
public class VanguardBasicAttack extends SpellTrait implements Listener {

    private final int momentumPerAttack = data.getInt("momentumPerAttack", 10);
    private final int momentumForChargedAttack = data.getInt("momentumForChargedAttack", 20);
    private final double perfectHitDamageMultiplier = data.getDouble("perfectHitDamageMultiplier", 1.2);
    private UUID pendingTarget;
    private int pendingTick = -1;
    private float pendingCharge;

    public VanguardBasicAttack(TraitData data, LivingEntity caster) {
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

    private boolean weaponHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        return event.getDamager() == caster && event.getEntity() instanceof LivingEntity target
                && Spellbook.canAttack(caster, target)
                && event.getDamageType() == PDamageType.PHYSICAL
                && source != null && source.type() == CombatSource.Type.BASIC_ATTACK;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void captureCharge(PrePlayerAttackEntityEvent event) {
        if (event.getPlayer() != caster || !event.willAttack()) return;
        pendingTarget = event.getAttacked().getUniqueId();
        pendingTick = Bukkit.getCurrentTick();
        pendingCharge = event.getPlayer().getAttackCooldown();
    }

    private boolean isCapturedAttack(EntityDamageByEntityEvent event) {
        return pendingTick == Bukkit.getCurrentTick() && pendingTarget != null
                && pendingTarget.equals(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void strengthenFullCharge(EntityDamageByEntityEvent event) {
        if (!weaponHit(event) || !isCapturedAttack(event) || event.getDamage() <= 0
                || pendingCharge < 0.99f) return;
        event.setDamage(event.getDamage() * perfectHitDamageMultiplier);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void confirmHit(EntityDamageByEntityEvent event) {
        if (!weaponHit(event) || !isCapturedAttack(event) || event.getFinalDamage() <= 0) return;
        float charge = pendingCharge;
        if (charge >= 0.99f) {
            caster.addEnergy(momentumForChargedAttack);
            BossControl.apply(caster, (LivingEntity) event.getEntity(), 30);
            var point = event.getEntity().getLocation().add(0, 1, 0);
            point.getWorld().playSound(point, Sound.BLOCK_ANVIL_LAND, 0.75f, 1.25f);
            point.getWorld().spawnParticle(Particle.DUST, point, 8, 0.25, 0.3, 0.25, 0,
                    new Particle.DustOptions(Color.fromRGB(160, 60, 60), 1.0f));
            if (SkillVfx.availableTo(caster)) SkillSignatureVfx.accent(
                    point, ShieldBreaker.class, "main", 0.9f, 8, false);
        } else if (charge >= 0.9f) {
            caster.addEnergy(momentumPerAttack);
        }
    }
}

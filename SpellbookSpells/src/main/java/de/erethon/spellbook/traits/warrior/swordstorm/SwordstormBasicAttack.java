package de.erethon.spellbook.traits.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import de.erethon.spellbook.spells.warrior.swordstorm.BladeDance;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Three confirmed rapid sword hits build to one visible, stronger weapon strike. */
public class SwordstormBasicAttack extends SpellTrait implements Listener {

    private static final String RAPID_MELEE_TAG = "papyrus:rapid_melee";
    private final int ragePerAttack = data.getInt("ragePerAttack", 10);
    private final int consecutiveAttacksForBonus = data.getInt("consecutiveAttacksForBonus", 3);
    private final int maxTicksBetweenAttacks = data.getInt("maxTicksBetweenAttacks", 20);
    private final double thirdHitMultiplier = data.getDouble("thirdHitMultiplier", 1.15);
    private final double fallingHitMultiplier = data.getDouble("fallingHitMultiplier", 1.1);
    private int consecutiveHits;
    private int lastHitTick;
    private int lastJumpControlTick = Integer.MIN_VALUE / 2;

    public SwordstormBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        caster.setMaxEnergy(100);
        caster.getTags().add(RAPID_MELEE_TAG);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
        caster.getTags().remove(RAPID_MELEE_TAG);
        super.onRemove();
    }

    private boolean swordHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        return event.getDamager() == caster && event.getEntity() instanceof LivingEntity target
                && Spellbook.canAttack(caster, target)
                && event.getDamageType() == PDamageType.PHYSICAL
                && source != null && source.type() == CombatSource.Type.BASIC_ATTACK;
    }

    private boolean readyForThird() {
        return consecutiveHits == consecutiveAttacksForBonus - 1
                && Bukkit.getCurrentTick() - lastHitTick <= maxTicksBetweenAttacks;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void resetOnMiss(PlayerInteractEvent event) {
        if (event.getPlayer() != caster || event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.LEFT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK)) return;
        // Hecate resolves a ray-traced contact synchronously at NORMAL priority.
        if (lastHitTick != Bukkit.getCurrentTick()) consecutiveHits = 0;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void prepareHit(EntityDamageByEntityEvent event) {
        if (!swordHit(event) || event.getDamage() <= 0) return;
        double multiplier = readyForThird() ? thirdHitMultiplier : 1.0;
        if (caster instanceof Player player && !player.isOnGround()
                && player.getVelocity().getY() < -0.08) multiplier *= fallingHitMultiplier;
        event.setDamage(event.getDamage() * multiplier);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void confirmHit(EntityDamageByEntityEvent event) {
        if (!swordHit(event) || event.getFinalDamage() <= 0) return;
        int tick = Bukkit.getCurrentTick();
        if (tick - lastHitTick > maxTicksBetweenAttacks) consecutiveHits = 0;
        consecutiveHits++;
        lastHitTick = tick;
        caster.addEnergy(ragePerAttack);
        if (caster instanceof Player player && !player.isOnGround()
                && player.getVelocity().getY() < -0.08
                && tick - lastJumpControlTick >= 40
                && BossControl.apply(caster, (LivingEntity) event.getEntity(), 25)) {
            lastJumpControlTick = tick;
        }
        if (consecutiveHits == consecutiveAttacksForBonus - 1) {
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.45f, 1.65f);
            if (SkillVfx.availableTo(caster)) {
                SkillSignatureVfx.accent(caster.getLocation().add(0, 1.15, 0),
                        BladeDance.class, "wake", 0.65f, 6, false);
            } else {
                caster.getWorld().spawnParticle(Particle.DUST, caster.getEyeLocation(), 4, 0.25, 0.25, 0.25, 0,
                        new Particle.DustOptions(Color.fromRGB(165, 70, 185), 0.9f));
            }
        }
        if (consecutiveHits >= consecutiveAttacksForBonus) {
            consecutiveHits = 0;
            caster.addEnergy(10);
            BossControl.apply(caster, (LivingEntity) event.getEntity(), 20);
            caster.getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT,
                    0.9f, 1.0f);
            event.getEntity().getWorld().spawnParticle(Particle.CRIT,
                    event.getEntity().getLocation().add(0, 1, 0), 9, 0.3, 0.3, 0.3, 0.08);
            if (SkillVfx.availableTo(caster)) SkillSignatureVfx.accent(
                    event.getEntity().getLocation().add(0, 1.1, 0),
                    BladeDance.class, "main", 1.0f, 8, false);
        } else {
            BossControl.apply(caster, (LivingEntity) event.getEntity(), 5);
        }
    }
}

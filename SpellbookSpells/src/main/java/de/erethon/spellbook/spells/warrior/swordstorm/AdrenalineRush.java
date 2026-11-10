package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Weapon-first recovery: immediate help, then earned healing from landed sword hits. */
public class AdrenalineRush extends SwordstormBaseSpell implements Listener {

    private final int activeTicks = data.getInt("activeTicks", 100);
    private final double castHealBase = data.getDouble("castHealBase", 12);
    private final double hitHealBase = data.getDouble("hitHealBase", 2);
    private final double playerCombatMultiplier = data.getDouble("playerCombatMultiplier", 0.6);
    private final double playerDamageMultiplier = data.getDouble("playerDamageMultiplier", 1.2);
    private final double entityDamageMultiplier = data.getDouble("entityDamageMultiplier", 1.4);
    private double remainingHitHealing;

    public AdrenalineRush(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = activeTicks;
        tickInterval = 1;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void strengthenSwordHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        if (event.getDamager() != caster || !(event.getEntity() instanceof LivingEntity target)
                || source == null || source.type() != CombatSource.Type.BASIC_ATTACK
                || event.getDamage() <= 0 || !Spellbook.canAttack(caster, target)) return;
        double multiplier = HealingContext.isPlayerCombat(caster, target)
                ? playerDamageMultiplier : entityDamageMultiplier;
        event.setDamage(event.getDamage() * multiplier);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double castHeal = Math.min(caster.getMaxHealth() * 0.2,
                castHealBase + 0.3 * healingPower);
        caster.heal(HealingContext.isPlayerCombat(caster)
                ? castHeal * playerCombatMultiplier : castHeal);
        remainingHitHealing = Math.min(caster.getMaxHealth() * 0.25,
                12 + 0.4 * healingPower);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 0.8f, 1.3f);
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "wake", 2.5f, 18, true);
        SkillSignatureVfx.accent(caster.getLocation().add(0, 1.1, 0), getClass(),
                "main", 1.2f, 10, false);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConfirmedSwordHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        if (event.getDamager() != caster || !(event.getEntity() instanceof LivingEntity target)
                || source == null || source.type() != CombatSource.Type.BASIC_ATTACK
                || event.getFinalDamage() <= 0 || remainingHitHealing <= 0
                || !Spellbook.canAttack(caster, target)) return;
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double amount = Math.min(remainingHitHealing, hitHealBase + 0.1 * healingPower);
        remainingHitHealing -= amount;
        if (HealingContext.isPlayerCombat(caster) || HealingContext.isPlayerCombat(target)) {
            amount *= playerCombatMultiplier;
        }
        caster.heal(amount);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.45f, 1.4f);
        SkillSignatureVfx.accent(target.getLocation().add(0, 1.1, 0), getClass(),
                "wake", 0.9f, 7, false);
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (currentTicks % 16 == 0) SkillSignatureVfx.accent(caster.getLocation(),
                getClass(), "lead", 1.5f, 12, true);
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        super.cleanup();
    }
}

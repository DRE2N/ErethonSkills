package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;
import java.util.stream.Collectors;

public class ShadowCloak extends ShadowBaseSpell implements Listener {

    // Cloaks the caster in shadows, making them invisible and increasing their movement speed.
    // Attacking will break the cloak and deal massive bonus damage.
    // Removes negative effects.
    // If the target is marked, apply blindness.
    // Blindness scales with advantage_magical.

    private final int blindnessMinDuration = data.getInt("blindnessDurationMin", 10) * 20;
    private final int blindnessMaxDuration = data.getInt("blindnessDurationMax", 20) * 20;
    private final double speedDurationMin = data.getDouble("speedDurationMin", 5) * 20;
    private final double speedDurationMax = data.getDouble("speedDurationMax", 8) * 20;
    private final double bonusDamageMin = data.getDouble("bonusDamageMin", 20);
    private final double bonusDamageMax = data.getDouble("bonusDamageMax", 150);
    private final double healthRegenMin = data.getDouble("healthRegenMin", 5);
    private final double healthRegenMax = data.getDouble("healthRegenMax", 25);

    private final EffectData blindness = Spellbook.getEffectData("Blindness");
    private final EffectData speedBoostData = Spellbook.getEffectData("Speed");

    private boolean alreadyAttacked = false;
    private AoE shadowAura = null;
    private int visualTicks = 8;

    public ShadowCloak(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        tickInterval = 1;
    }

    @Override
    public boolean onCast() {
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.RECORDS, 0.8f, 1.2f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, SoundCategory.RECORDS, 0.6f, 0.8f);

        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 25, 1, 1, 1, 0.1, new Particle.DustOptions(Color.BLACK, 1.5f));
        caster.getWorld().spawnParticle(Particle.SMOKE, caster.getLocation().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.1);

        caster.setInvisible(true);
        int speedDuration = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, speedDurationMin, speedDurationMax, "speedDuration");
        caster.addEffect(caster, speedBoostData, speedDuration, 1);

        Set<EffectData> toRemove = caster.getEffects().stream()
            .filter(effect -> !effect.data.isPositive())
            .map(effect -> effect.data)
            .collect(Collectors.toSet());
        toRemove.forEach(caster::removeEffect);

        caster.getTags().add("shadow_cloak");

        shadowAura = createCircularAoE(caster.getLocation(), 1.5, 1, keepAliveTicks)
            .followEntity(caster)
            .addBlockChange(Material.GRAY_CONCRETE_POWDER)
            .sendBlockChanges();
        // Hide equipment from other players, otherwise we have floating weapons
        for (Player player : caster.getTrackedBy()) {
            player.sendEquipmentChange(caster, EquipmentSlot.HAND, null);
            player.sendEquipmentChange(caster, EquipmentSlot.OFF_HAND, null);
            player.sendEquipmentChange(caster, EquipmentSlot.HEAD, null);
            player.sendEquipmentChange(caster, EquipmentSlot.CHEST, null);
            player.sendEquipmentChange(caster, EquipmentSlot.LEGS, null);
            player.sendEquipmentChange(caster, EquipmentSlot.FEET, null);
        }
        for (LivingEntity entity : caster.getLocation().getNearbyLivingEntities(16)) {
            if (Spellbook.canAttack(caster, entity) && entity != caster && entity instanceof Mob mob) {
                if (mob.getTarget() == caster) {
                    mob.setTarget(null);
                }
            }
        }
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @Override
    protected void onTick() {
        visualTicks--;
        if (caster.getTicksLived() % 20 == 0) {
            double healthRegen = Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healthRegenMin, healthRegenMax, "healthRegen");
            caster.heal(healthRegen, EntityRegainHealthEvent.RegainReason.CUSTOM);
        }
        if (visualTicks <= 0) {
            visualTicks = 8;
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(Color.fromRGB(64, 64, 64), 0.8f));
            caster.getWorld().spawnParticle(Particle.WHITE_ASH, caster.getLocation().add(0, 0.5, 0), 2, 0.3, 0.1, 0.3, 0.01);
        }
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (!caster.getTags().contains("shadow_cloak")) {
            return super.onAttack(target, damage, type);
        }

        endCloak();
        if (alreadyAttacked) {
            return super.onAttack(target, damage, type);
        }

        Location attackLoc = target.getLocation().add(0, 1, 0);
        attackLoc.getWorld().spawnParticle(Particle.DUST, attackLoc, 20, 0.8, 0.8, 0.8, 0.1, new Particle.DustOptions(Color.BLACK, 1.5f));
        attackLoc.getWorld().spawnParticle(Particle.CRIT, attackLoc, 15, 0.5, 0.5, 0.5, 0.3);
        attackLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, attackLoc, 3, 0.3, 0.3, 0.3, 0.1);

        attackLoc.getWorld().playSound(attackLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.RECORDS, 1.2f, 0.8f);
        attackLoc.getWorld().playSound(attackLoc, Sound.BLOCK_SOUL_SAND_BREAK, SoundCategory.RECORDS, 0.8f, 1.5f);

        if (target instanceof Player player) {
            player.playSound(caster, Sound.ITEM_SHIELD_BREAK, 1, 1);
        }
        if (caster instanceof Player casterPlayer) {
            casterPlayer.playSound(caster, Sound.ITEM_SHIELD_BREAK, 1, 1);
        }

        target.addEffect(caster, blindness,
            (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, blindnessMinDuration, blindnessMaxDuration, "blindnessDuration"), 1);

        alreadyAttacked = true;
        double bonusDamage = Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bonusDamageMin, bonusDamageMax, "bonusDamage");
        return super.onAttack(target, damage + bonusDamage, type);
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        caster.setInvisible(false);
        caster.getTags().remove("shadow_cloak");
        if (shadowAura != null) {
            shadowAura.revertBlockChanges();
            shadowAura.end();
        }
        // Restore equipment visibility
        for (Player player : caster.getTrackedBy()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (caster.getEquipment() != null) {
                    caster.getEquipment().getItem(slot);
                    if (caster.getEquipment().getItem(slot).getType() != Material.AIR) {
                        player.sendEquipmentChange(caster, slot, caster.getEquipment().getItem(slot));
                    }
                }
            }
        }
    }

    @EventHandler
    private void onTarget(EntityTargetEvent event) {
        if (event.getTarget() == null || event.getTarget() != caster) {
            return;
        }
        if (!caster.getTags().contains("shadow_cloak")) {
            return;
        }
        if (event.getReason() == EntityTargetEvent.TargetReason.TARGET_INVALID || event.getReason() == EntityTargetEvent.TargetReason.FORGOT_TARGET) {
            return;
        }
        event.setCancelled(true);
    }

    private void endCloak() {
        caster.getTags().remove("shadow_cloak");

        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.1, new Particle.DustOptions(Color.fromRGB(96, 96, 96), 1.2f));
        caster.getWorld().spawnParticle(Particle.SMOKE, caster.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.05);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.RECORDS, 0.6f, 1.8f);

        if (shadowAura != null) {
            shadowAura.revertBlockChanges();
            shadowAura.end();
        }

        currentTicks = keepAliveTicks;
        onTickFinish();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text((int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, blindnessMinDuration, blindnessMaxDuration, "blindnessDuration") / 20, VALUE_COLOR));
        placeholderNames.add("blindnessDuration");
        spellAddedPlaceholders.add(Component.text((int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, speedDurationMin, speedDurationMax, "speedDuration") / 20, VALUE_COLOR));
        placeholderNames.add("speedDuration");
        spellAddedPlaceholders.add(Component.text((int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bonusDamageMin, bonusDamageMax, "bonusDamage"), ATTR_PHYSICAL_COLOR));
        placeholderNames.add("bonusDamage");
        spellAddedPlaceholders.add(Component.text((int) Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healthRegenMin, healthRegenMax, "healthRegen"), VALUE_COLOR));
        placeholderNames.add("healthRegen");
    }
}

package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShoutOfResolve extends PaladinBaseSpell implements Listener {

    // The Guardian creates a radiant aura of unbreakable resolve, becoming immune to all negative effects.
    // Allies within the aura gain significant damage reduction and reflect damage back to attackers.
    // The reflected damage heals both the Guardian and their allies based on devotion.

    private final double range = data.getDouble("range", 6);
    private final double guardianDamageMultiplierMin = data.getDouble("guardianDamageMultiplierMin", 0.3);
    private final double guardianDamageMultiplierMax = data.getDouble("guardianDamageMultiplierMax", 0.7);
    private final double allyDamageMultiplierMin = data.getDouble("allyDamageMultiplierMin", 0.2);
    private final double allyDamageMultiplierMax = data.getDouble("allyDamageMultiplierMax", 0.5);
    private final double guardianDamageReflectMin = data.getDouble("guardianDamageReflectMin", 0.4);
    private final double guardianDamageReflectMax = data.getDouble("guardianDamageReflectMax", 0.8);
    private final double allyDamageReflectMin = data.getDouble("allyDamageReflectMin", 0.2);
    private final double allyDamageReflectMax = data.getDouble("allyDamageReflectMax", 0.4);
    private final double allyHealDamageMultiplier = data.getDouble("allyHealDamageMultiplier", 0.5);
    private final double allyHealPerDevotionMin = data.getDouble("allyHealPerDevotionMin", 0.3);
    private final double allyHealPerDevotionMax = data.getDouble("allyHealPerDevotionMax", 1.5);
    private final int auraDuration = data.getInt("auraDuration", 120);

    private final Map<LivingEntity, Double> toDamageOnNextTick = new HashMap<>();
    private AoE resolveAura;

    public ShoutOfResolve(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, SoundCategory.RECORDS, 1.5f, 0.8f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WARDEN_ROAR, SoundCategory.RECORDS, 0.8f, 1.2f);

        resolveAura = createCircularAoE(caster.getLocation(), range, 1, auraDuration)
                .followEntity(caster)
                .onTick(aoe -> {
                    if (caster.getTicksLived() % 10 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            aoe.getCenter().add(0, 3, 0),
                            15, range * 0.5, 2, range * 0.5, 0.05);
                    }
                    if (caster.getTicksLived() % 15 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.END_ROD,
                            aoe.getCenter().add(0, 1, 0),
                            12, range * 0.7, 1.5, range * 0.7, 0.03);
                    }
                    if (caster.getTicksLived() % 20 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                            aoe.getCenter(),
                            20, range * 0.8, 0.5, range * 0.8, 0.1);
                    }
                })
                .addBlockChange(Material.GOLD_BLOCK, Material.YELLOW_STAINED_GLASS, Material.ORANGE_STAINED_GLASS)
                .sendBlockChanges();

        caster.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
            caster.getLocation().add(0, 2, 0),
            30, 1, 2, 1, 0.1);

        return super.onCast();
    }

    @Override
    public boolean onAddEffect(SpellEffect effect, boolean isNew) {
        if (isNew && !effect.data.isPositive()) {
            caster.getWorld().spawnParticle(Particle.BLOCK,
                caster.getLocation().add(0, 1, 0),
                3, 0.5, 0.5, 0.5, 0, Material.BARRIER.createBlockData());
            return false;
        }
        return super.onAddEffect(effect, isNew);
    }

    @EventHandler
    private void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        if (damager instanceof LivingEntity livingDamager && target instanceof LivingEntity livingTarget) {

            if (target.getLocation().distance(caster.getLocation()) <= range && !Spellbook.canAttack(caster, livingTarget)) {
                double damageMultiplier = Spellbook.getRangedValue(data, caster, livingTarget, Attribute.RESISTANCE_PHYSICAL, allyDamageMultiplierMin, allyDamageMultiplierMax, "allyDamageMultiplier");
                double damage = event.getDamage() * (1 - damageMultiplier);
                event.setDamage(damage);

                double reflectMultiplier = Spellbook.getRangedValue(data, caster, livingTarget, Attribute.ADVANTAGE_PHYSICAL, allyDamageReflectMin, allyDamageReflectMax, "allyDamageReflect");
                double reflectDamage = event.getDamage() * reflectMultiplier;
                toDamageOnNextTick.put(livingDamager, reflectDamage);

                double healPerDevotion = Spellbook.getRangedValue(data, caster, livingTarget, Attribute.STAT_HEALINGPOWER, allyHealPerDevotionMin, allyHealPerDevotionMax, "allyHealPerDevotion");
                double healAmount = (reflectDamage * allyHealDamageMultiplier) + (caster.getEnergy() * healPerDevotion);

                livingTarget.setHealth(Math.min(
                    livingTarget.getAttribute(Attribute.MAX_HEALTH).getValue(),
                    livingTarget.getHealth() + healAmount
                ));
                caster.setHealth(Math.min(
                    caster.getAttribute(Attribute.MAX_HEALTH).getValue(),
                    caster.getHealth() + healAmount * 0.5
                ));

                livingTarget.getWorld().spawnParticle(Particle.HEART,
                    livingTarget.getLocation().add(0, 2, 0),
                    3, 0.3, 0.3, 0.3, 0);
                livingTarget.getWorld().playSound(livingTarget.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.RECORDS, 0.3f, 1.5f);
            }

            if (livingTarget == caster) {
                double damageMultiplier = Spellbook.getRangedValue(data, caster, livingDamager, Attribute.RESISTANCE_PHYSICAL, guardianDamageMultiplierMin, guardianDamageMultiplierMax, "guardianDamageMultiplier");
                double damage = event.getDamage() * (1 - damageMultiplier);
                event.setDamage(damage);

                double reflectMultiplier = Spellbook.getRangedValue(data, caster, livingDamager, Attribute.ADVANTAGE_PHYSICAL, guardianDamageReflectMin, guardianDamageReflectMax, "guardianDamageReflect");
                double reflectDamage = event.getDamage() * reflectMultiplier;
                toDamageOnNextTick.put(livingDamager, reflectDamage);

                caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                    caster.getLocation().add(0, 1, 0),
                    8, 0.5, 1, 0.5, 0.1);
                caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.RECORDS, 1, 0.8f);
            }
        }
    }

    @Override
    protected void onTick() {
        for (Map.Entry<LivingEntity, Double> entry : toDamageOnNextTick.entrySet()) {
            LivingEntity entity = entry.getKey();
            double damage = entry.getValue();
            if (entity.isValid() && entity.getWorld() == caster.getWorld()) {
                entity.damage(damage, caster, PDamageType.MAGIC);
                entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                    entity.getLocation().add(0, 1, 0),
                    5, 0.5, 1, 0.5, 0.02);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.RECORDS, 0.5f, 1.5f);
            }
        }
        toDamageOnNextTick.clear();
        super.onTick();
    }

    @Override
    protected void cleanup() {
        if (resolveAura != null) {
            resolveAura.revertBlockChanges();
        }
        HandlerList.unregisterAll(this);
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_PHYSICAL, allyDamageMultiplierMin, allyDamageMultiplierMax, "allyDamageMultiplier"), VALUE_COLOR));
        placeholderNames.add("allyDamageMultiplier");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, allyDamageReflectMin, allyDamageReflectMax, "allyDamageReflect"), VALUE_COLOR));
        placeholderNames.add("allyDamageReflect");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_PHYSICAL, guardianDamageMultiplierMin, guardianDamageMultiplierMax, "guardianDamageMultiplier"), VALUE_COLOR));
        placeholderNames.add("guardianDamageMultiplier");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, guardianDamageReflectMin, guardianDamageReflectMax, "guardianDamageReflect"), VALUE_COLOR));
        placeholderNames.add("guardianDamageReflect");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, allyHealPerDevotionMin, allyHealPerDevotionMax, "allyHealPerDevotion"), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("allyHealPerDevotion");
    }
}

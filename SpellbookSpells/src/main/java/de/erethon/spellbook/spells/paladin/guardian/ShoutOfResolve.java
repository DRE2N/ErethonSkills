package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CylinderEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

    // For several seconds, the Guardian is immune to all negative effects and reflects a portion of damage back to the attacker.
    // Allies close to the Guardian gain a portion of the Guardian's damage reduction and reflect damage.
    // They are additionally healed for a portion of the damage reflected, based on the Guardians devotion.
    // The Guardian himself receives all the healing to allies as well.

    private final double range = data.getDouble("range", 5);
    private final double guardianDamageMultiplierMin = data.getDouble("guardianDamageMultiplierMin", 0.2);
    private final double guardianDamageMultiplierMax = data.getDouble("guardianDamageMultiplierMax", 0.8);
    private final double allyDamageMultiplierMin = data.getDouble("allyDamageMultiplierMin", 0.2);
    private final double allyDamageMultiplierMax = data.getDouble("allyDamageMultiplierMax", 0.8);
    private final double guardianDamageReflectMin = data.getDouble("guardianDamageReflectMin", 0.2);
    private final double guardianDamageReflectMax = data.getDouble("guardianDamageReflectMax", 0.8);
    private final double allyDamageReflectMin = data.getDouble("allyDamageReflectMin", 0.2);
    private final double allyDamageReflectMax = data.getDouble("allyDamageReflectMax", 0.8);
    private final double allyHealDamageMultiplier = data.getDouble("allyHealDamageMultiplier", 0.33);
    private final double allyHealPerDevotionMin = data.getDouble("allyHealPerDevotionMin", 0.5);
    private final double allyHealPerDevotionMax = data.getDouble("allyHealPerDevotionMax", 2);

    private final Map<LivingEntity, Double> toDamageOnNextTick = new HashMap<>();

    public ShoutOfResolve(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data); // 75
    }

    @Override
    public boolean onCast() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        caster.getWorld().spawnParticle(Particle.GLOW, caster.getLocation(), 3, 0.5, 0.5, 0.5);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1, 1);
        CylinderEffect effect = new CylinderEffect(Spellbook.getInstance().getEffectManager());
        effect.setLocation(caster.getLocation().clone().add(0, 1, 0));
        effect.particle = Particle.DUST;
        effect.particleCount = 32;
        effect.radius = (float) range;
        effect.iterations = -1;
        effect.duration = 20 * duration;
        effect.period = 20;
        effect.height = 3;
        effect.solid = false;
        effect.color = Color.BLUE;
        effect.start();
        return super.onCast();
    }

    @Override
    public boolean onAddEffect(SpellEffect effect, boolean isNew) {
        if (isNew && !effect.data.isPositive()) {
            return false;
        }
        return super.onAddEffect(effect, isNew);
    }

    @EventHandler
    private void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();
        if (damager instanceof  LivingEntity livingDamager && target instanceof LivingEntity livingTarget) {
            if (target.getLocation().distance(caster.getLocation()) <= range && !Spellbook.canAttack(caster, livingTarget)) {
                double damageMultiplier = Spellbook.getRangedValue(data, caster, livingTarget, Attribute.RESISTANCE_PHYSICAL, allyDamageMultiplierMin, allyDamageMultiplierMax, "allyDamageMultiplier");
                double damage = event.getDamage() * (1 - damageMultiplier);
                event.setDamage(damage);
                double reflectMultiplier = Spellbook.getRangedValue(data, caster, livingTarget, Attribute.ADVANTAGE_PHYSICAL, allyDamageReflectMin, allyDamageReflectMax, "allyDamageReflect");
                double reflectDamage = event.getDamage() * reflectMultiplier;
                toDamageOnNextTick.put(livingDamager, reflectDamage);
                double healPerDevotion = Spellbook.getRangedValue(data, caster, livingTarget, Attribute.STAT_HEALINGPOWER, allyHealPerDevotionMin, allyHealPerDevotionMax, "allyHealPerDevotion");
                double healAmount = (reflectDamage * allyHealDamageMultiplier) * (caster.getEnergy() * healPerDevotion);
                livingTarget.heal(healAmount);
                caster.heal(healAmount); // This is likely very strong, let's see
                livingTarget.getWorld().spawnParticle(Particle.GLOW, livingTarget.getLocation(), 3, 0.5, 0.5, 0.5);
            }
            if (livingTarget == caster) {
                double damageMultiplier = Spellbook.getRangedValue(data, caster, livingDamager, Attribute.RESISTANCE_PHYSICAL, guardianDamageMultiplierMin, guardianDamageMultiplierMax, "guardianDamageMultiplier");
                double damage = event.getDamage() * (1 - damageMultiplier);
                event.setDamage(damage);
                double reflectMultiplier = Spellbook.getRangedValue(data, caster, livingDamager, Attribute.ADVANTAGE_PHYSICAL, guardianDamageReflectMin, guardianDamageReflectMax, "guardianDamageReflect");
                double reflectDamage = event.getDamage() * reflectMultiplier;
                toDamageOnNextTick.put(livingDamager, reflectDamage);
            }
        }
    }

    @Override
    protected void onTick() {
        for (Map.Entry<LivingEntity, Double> entry : toDamageOnNextTick.entrySet()) {
            LivingEntity entity = entry.getKey();
            double damage = entry.getValue();
            if (entity.isValid() && entity.getWorld() == caster.getWorld() && entity.getLocation().distance(caster.getLocation()) <= range) {
                entity.damage(damage, caster, PDamageType.MAGIC);
                entity.getWorld().spawnParticle(Particle.GLOW, entity.getLocation(), 3, 0.5, 0.5, 0.5);
            }
        }
        toDamageOnNextTick.clear();
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        HandlerList.unregisterAll(this);
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
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, allyHealPerDevotionMin, allyHealPerDevotionMax, "allyHealPerDevotion"), VALUE_COLOR));
        placeholderNames.add("allyHealPerDevotion");
    }
}

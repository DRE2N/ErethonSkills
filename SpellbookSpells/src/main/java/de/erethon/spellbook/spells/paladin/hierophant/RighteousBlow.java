package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Comparator;

/** Right-click glaive contact builds Wrath; an optional spend cleaves two nearby enemies. */
public class RighteousBlow extends HierophantBaseSpell {

    private static final NamespacedKey READY_KEY = new NamespacedKey("spellbook", "hierophant_righteous_ready_at");
    private final double range = data.getDouble("range", 3.5);
    private final int wrathOnHit = data.getInt("wrathOnHit", 10);
    private final int empoweredCost = data.getInt("empoweredCost", 40);
    private final int cooldownSeconds = data.getInt("cooldown", 4);

    public RighteousBlow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        long readyAt = caster.getPersistentDataContainer().getOrDefault(READY_KEY, PersistentDataType.LONG, 0L);
        return System.currentTimeMillis() >= readyAt && super.onPrecast()
                && lookForTarget((int) Math.ceil(range));
    }

    @Override
    public boolean onCast() {
        boolean empowered = caster.getEnergy() >= empoweredCost;
        if (!super.onCast()) return false;
        caster.getPersistentDataContainer().set(READY_KEY, PersistentDataType.LONG,
                System.currentTimeMillis() + cooldownSeconds * 1000L);
        if (empowered) caster.setEnergy(Math.max(0, caster.getEnergy() - empoweredCost));
        if (target == null || !target.isValid() || !Spellbook.canAttack(caster, target)
                || target.getWorld() != caster.getWorld() || !caster.hasLineOfSight(target)
                || caster.getLocation().distanceSquared(target.getLocation()) > range * range) return true;
        Attribute attribute = empowered ? Attribute.ADVANTAGE_MAGICAL : Attribute.ADVANTAGE_PHYSICAL;
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, attribute);
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(damage, caster,
                empowered ? PDamageType.MAGIC : PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) return true;
        if (!empowered) caster.addEnergy(wrathOnHit);
        BossControl.apply(caster, target, empowered ? 25 : 20);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG,
                0.85f, empowered ? 0.8f : 1.2f);
        target.getWorld().spawnParticle(Particle.END_ROD, target.getEyeLocation(),
                SkillVfx.availableTo(caster) ? 2 : empowered ? 12 : 5,
                0.25, 0.3, 0.25, 0.03);
        if (empowered) target.getWorld().getNearbyLivingEntities(target.getLocation(), 3).stream()
                .filter(enemy -> enemy != target && Spellbook.canAttack(caster, enemy)
                        && caster.hasLineOfSight(enemy))
                .sorted(Comparator.comparingDouble(enemy -> enemy.getLocation().distanceSquared(target.getLocation())))
                .limit(2)
                .forEach(enemy -> {
                    double cleave = Spellbook.getVariedAttributeBasedDamage(data, caster, enemy, false,
                            Attribute.ADVANTAGE_MAGICAL) * 0.5;
                    CombatSourceContext.runSpell(this, () -> enemy.damage(cleave, caster, PDamageType.MAGIC));
                });
        triggerTraits(target);
        return true;
    }
}

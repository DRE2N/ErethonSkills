package de.erethon.spellbook.traits.paladin.guardian;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class ShieldOfFaithTrait extends SpellTrait {

    // The Guardian can raise a shield that reduces incoming damage and heals nearby allies when the shield is up and he is hit.

    private final double damageModifier = data.getDouble("damageModifier", 0.25);
    private final double healRadius = data.getDouble("healRadius", 2.5);
    private final double healMin = data.getDouble("healMin", 20);
    private final double healMax = data.getDouble("healMax", 40);

    public ShieldOfFaithTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (isShieldUp()) {
            // Check if the attacker is somewhat in front of the caster
            double angleDiff = getAngleDiff(attacker);
            if (angleDiff < 90) {
                damage = damage * damageModifier;
                caster.getWorld().playSound(caster, Sound.ITEM_SHIELD_BLOCK, SoundCategory.RECORDS, 1.0f, 1.2f);
                double heal = Spellbook.getRangedValue(data, caster, attacker, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal");
                for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(healRadius)) {
                    if (living != caster && living != attacker && !Spellbook.canAttack(caster, living)) {
                        living.heal(heal);
                        living.getWorld().spawnParticle(Particle.HEART, living.getLocation(), 2, 0.5, 0.5, 0.5);
                        living.getWorld().playSound(living.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.RECORDS,0.5f, 1);
                    }
                }
            }
        }
        return super.onDamage(attacker, damage, type);
    }

    private double getAngleDiff(LivingEntity attacker) {
        double angle = Math.toDegrees(Math.atan2(attacker.getLocation().getZ() - caster.getLocation().getZ(), attacker.getLocation().getX() - caster.getLocation().getX()));
        if (angle < 0) {
            angle += 360;
        }
        double direction = Math.toDegrees(caster.getLocation().getYaw());
        if (direction < 0) {
            direction += 360;
        }
        double angleDiff = Math.abs(angle - direction);
        if (angleDiff > 180) {
            angleDiff = 360 - angleDiff;
        }
        return angleDiff;
    }

    private boolean isShieldUp() {
        return caster.getTags().contains("paladin.shield");
    }
}

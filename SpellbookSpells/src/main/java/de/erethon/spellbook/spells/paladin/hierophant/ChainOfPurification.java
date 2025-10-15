package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ChainOfPurification extends HierophantBaseSpell {

    // The hierophant launches a chain of holy energy in front of him. The first enemy hit is pulled halfway to the caster and stunned for 2 seconds.
    // If the wrath is above 50, the chain also pulls additional enemies in a 3-block radius to the target and creates crackling energy ground effects.

    private final int range = data.getInt("range", 20);
    private final double pullSpeed = data.getDouble("pullSpeed", 3.0);
    private final int minimumEnergyForBonus = data.getInt("minimumEnergyForBonus", 50);
    private final int bonusEnemyCount = data.getInt("bonusEnemyCount", 2);

    private LineEffect lineEffect;
    private Location chainEndLocation;
    private boolean hasHit = false;

    public ChainOfPurification(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        chainEndLocation = caster.getLocation();
        lineEffect = new LineEffect(Spellbook.getInstance().getEffectManager());
        lineEffect.setEntity(caster);
        lineEffect.setTarget(chainEndLocation);
        lineEffect.particles = 20;
        lineEffect.particle = org.bukkit.Particle.DRAGON_BREATH;
        lineEffect.duration = duration * 20;
        lineEffect.start();

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.5f);

        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (hasHit) {
            lineEffect.cancel();
            keepAliveTicks = 0;
            onTickFinish();
            return;
        }
        if (caster.getLocation().distance(chainEndLocation) > range) {
            lineEffect.cancel();
            keepAliveTicks = 0;
            onTickFinish();
            return;
        }
        Vector direction = caster.getEyeLocation().getDirection();
        direction.multiply(0.5);
        chainEndLocation.add(direction);
        lineEffect.setTarget(chainEndLocation);

        BoundingBox chainEndBoundingBox = new BoundingBox(
                chainEndLocation.getX() - 1.5,
                chainEndLocation.getY() - 1.5,
                chainEndLocation.getZ() - 1.5,
                chainEndLocation.getX() + 1.5,
                chainEndLocation.getY() + 1.5,
                chainEndLocation.getZ() + 1.5
        );

        int entityLimit = 1;
        boolean hasHighWrath = caster.getEnergy() > minimumEnergyForBonus;
        if (hasHighWrath) {
            entityLimit += bonusEnemyCount;
            caster.setEnergy(0);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 0.8f, 0.8f);
        }

        int entities = 0;
        for (Entity entity : caster.getWorld().getNearbyEntities(chainEndBoundingBox)) {
            if (entity instanceof LivingEntity living && living != caster && Spellbook.canAttack(caster, living)) {
                hasHit = true;
                entities++;
                if (entities > entityLimit) {
                    break;
                }

                Vector pullDirection = caster.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
                living.setVelocity(pullDirection.multiply(pullSpeed));

                living.getWorld().spawnParticle(Particle.ENCHANTED_HIT, living.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);
                living.getWorld().playSound(living.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.2f);

                if (hasHighWrath) {
                    createCircularAoE(living.getLocation(), 3, 1, 80)
                            .onEnter((aoe, ent) -> {
                                if (Spellbook.canAttack(caster, ent) && ent != living) {
                                    Vector additionalPull = caster.getLocation().toVector().subtract(ent.getLocation().toVector()).normalize();
                                    ent.setVelocity(additionalPull.multiply(pullSpeed * 0.7));
                                    ent.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ent.getLocation(), 5, 0.3, 0.3, 0.3);
                                }
                            })
                            .onTick(aoe -> {
                                for (LivingEntity ent : aoe.getEntitiesInside()) {
                                    if (Spellbook.canAttack(caster, ent)) {
                                        ent.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ent.getLocation(), 1, 0.2, 0.2, 0.2);
                                    }
                                }
                            });
                }
            }
        }
        super.onTick();
    }

    @Override
    protected void cleanup() {
        lineEffect.cancel();
        super.cleanup();
    }
}

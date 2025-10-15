package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class ConcussiveBlast extends SharpshooterBaseSpell {

    // Instantly unleash a short-range (4-block cone) blast of kinetic energy in front of you.
    // Deals minor damage but knocks back all enemies hit and applies a brief "Disorient" effect (Minecraft's Nausea for 1s).

    private final double coneRange = data.getDouble("coneRange", 4.0);
    private final double coneAngle = data.getDouble("coneAngle", 60.0);
    private final double minKnockbackStrength = data.getDouble("minKnockbackStrength", 1.5);
    private final double maxKnockbackStrength = data.getDouble("maxKnockbackStrength", 2.5);
    private final int disorientDuration = data.getInt("disorientDuration", 1) * 20;

    public ConcussiveBlast(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        executeBlast();
        return super.onCast();
    }

    private void executeBlast() {
        Location blastOrigin = caster.getLocation();
        Vector direction = caster.getLocation().getDirection();
        blastOrigin = blastOrigin.add(direction.clone().multiply(-0.3)); // Start a bit behind caster so close enemies are also hit

        caster.getWorld().playSound(blastOrigin, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 1.8f);
        caster.getWorld().playSound(blastOrigin, Sound.ENTITY_BREEZE_WIND_BURST, SoundCategory.PLAYERS, 1.5f, 0.8f);

        blastOrigin.getWorld().spawnParticle(Particle.EXPLOSION, blastOrigin.add(direction.clone().multiply(1)), 1, 0, 0, 0, 0);
        blastOrigin.getWorld().spawnParticle(Particle.CLOUD, blastOrigin, 15, 1, 0.5, 1, 0.3);

        Location finalBlastOrigin = blastOrigin;
        createConeAoE(blastOrigin, coneRange, coneAngle, 2.5, direction, 5)
                .onEnter((aoe, entity) -> {
                    if (entity != caster && Spellbook.canAttack(caster, entity)) {
                        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
                        entity.damage(damage, caster, PDamageType.PHYSICAL);

                        double knockbackMagnitude = Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_PHYSICAL, minKnockbackStrength, maxKnockbackStrength, "knockbackStrength");
                        Vector knockbackDirection = entity.getLocation().subtract(finalBlastOrigin).toVector().normalize();
                        knockbackDirection.setY(Math.max(knockbackDirection.getY(), 0.3));
                        entity.setVelocity(knockbackDirection.multiply(knockbackMagnitude));

                        entity.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, disorientDuration, 1));

                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.8f, 1.2f);
                        entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
                    }
                }).onTick(
                        aoe -> {
                            for (LivingEntity entity : aoe.getEntitiesInside()) {
                                if (Spellbook.canAttack(caster, entity) && entity != caster) {
                                    entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.05);
                                }
                            }
                        });
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, null, Attribute.ADVANTAGE_PHYSICAL, minKnockbackStrength, maxKnockbackStrength, "knockbackStrength"), VALUE_COLOR));
        placeholderNames.add("knockbackStrength");
    }
}

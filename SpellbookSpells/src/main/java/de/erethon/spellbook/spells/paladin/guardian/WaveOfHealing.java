package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class WaveOfHealing extends PaladinBaseSpell {

    // The Guardian creates a wave of healing that flows forward in a cone, healing all allies it touches.
    // Heal is increased based on the current devotion of the caster.
    // Range scales with advantage_magical, while healing scales with stat_healingpower.

    private final int rangeMin = data.getInt("rangeMin", 8);
    private final int rangeMax = data.getInt("rangeMax", 14);
    private final double baseHealing = data.getDouble("baseHealing", 15);
    private final double healPerDevotionMin = data.getDouble("healPerDevotionMin", 1);
    private final double healPerDevotionMax = data.getDouble("healPerDevotionMax", 5);
    private final int coneAngle = data.getInt("coneAngle", 60);
    private final int waveDuration = data.getInt("waveDuration", 100);

    public WaveOfHealing(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        double range = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, rangeMin, rangeMax, "range");
        double healPerDevotion = Spellbook.getRangedValue(data, caster, target, Attribute.STAT_HEALINGPOWER, healPerDevotionMin, healPerDevotionMax, "healPerDevotion");

        Set<LivingEntity> healedEntities = new HashSet<>();

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 1.0f, 0.8f);

        AoE healingWave = createConeAoE(caster.getLocation(), range, coneAngle, 2, caster.getLocation().getDirection(), waveDuration)
                .onEnter((aoe, entity) -> {
                    if (entity instanceof LivingEntity target && !Spellbook.canAttack(caster, target) && !healedEntities.contains(target)) {
                        healedEntities.add(target);

                        target.getEffects().forEach(effect -> {
                            if (!effect.data.isPositive()) {
                                target.removeEffect(effect.data);
                            }
                        });

                        double totalHealing = baseHealing + (caster.getEnergy() * healPerDevotion);
                        double currentHealth = target.getHealth();
                        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();

                        target.setHealth(Math.min(maxHealth, currentHealth + totalHealing));

                        target.getWorld().spawnParticle(Particle.HEART,
                            target.getLocation().add(0, 2, 0),
                            3, 0.5, 0.5, 0.5, 0);
                        target.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                            target.getLocation().add(0, 1, 0),
                            5, 0.3, 0.8, 0.3, 0.02);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.RECORDS, 0.3f, 1.5f);
                    }
                })
                .onTick(aoe -> {
                    if (caster.getTicksLived() % 5 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.END_ROD,
                            aoe.getCenter().add(0, 1, 0),
                            8, range * 0.3, 1, range * 0.3, 0.03);
                    }
                    if (caster.getTicksLived() % 8 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.INSTANT_EFFECT,
                            aoe.getCenter().add(0, 0.2, 0),
                            4, range * 0.4, 0.5, range * 0.4, 0);
                    }
                })
                .addBlockChange(Material.BLUE_CONCRETE)
                .sendBlockChanges();

        triggerTraits(healedEntities);
        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, rangeMin, rangeMax, "range"), VALUE_COLOR));
        placeholderNames.add("range");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healPerDevotionMin, healPerDevotionMax, "healPerDevotion"), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("healPerDevotion");
    }
}

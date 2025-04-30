package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WaveOfHealing extends PaladinBaseSpell {

    // The Guardian creates a wave of healing that heals all allies in a radius around the caster.
    // Heal is increased based on the current devotion of the caster.
    // Range scales with advantage_magical, while healing scales with stat_healingpower.

    private final int rangeMin = data.getInt("rangeMin", 3);
    private final int rangeMax = data.getInt("rangeMax", 8);
    private final double baseHealing = data.getDouble("baseHealing", 5);
    private final double healPerDevotionMin = data.getDouble("healPerDevotionMin", 1);
    private final double healPerDevotionMax = data.getDouble("healPerDevotionMax", 5);

    private final Set<CircleEffect> circleEffects = new HashSet<>();
    private BukkitRunnable moveCircles;

    public WaveOfHealing(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 20 * 5;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data); // 10
    }


    @Override
    public boolean onCast() {
        Set<LivingEntity> entities = new HashSet<>();
        double range = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, rangeMin, rangeMax, "range");
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            if (Spellbook.canAttack(caster, living)) continue;
            entities.add(living);
            living.getEffects().forEach(e -> {
                if (!e.data.isPositive()) {
                    living.removeEffect(e.data);
                }
            });
            Location livingLocation = living.getLocation().clone();
            livingLocation.setPitch(0);
            CircleEffect circle = new CircleEffect(Spellbook.getInstance().getEffectManager());
            circle.particle = Particle.END_ROD;
            circle.radius = 1.5f;
            circle.particleCount = 16;
            circle.duration = 60 * 50;
            circle.iterations = -1;
            circle.setLocation(livingLocation);
            circle.start();
            circleEffects.add(circle);
        }
        triggerTraits(entities);
        moveCircles = new BukkitRunnable() {
            @Override
            public void run() {
                for (CircleEffect circle : circleEffects) {
                    circle.setLocation(circle.getLocation().add(0, 0.1, 0));
                }
            }
        };
        for (LivingEntity living : entities) {
            double healing = baseHealing + (caster.getEnergy() * Spellbook.getRangedValue(data, caster, living, Attribute.STAT_HEALINGPOWER, healPerDevotionMin, healPerDevotionMax, "healPerDevotion"));
            living.heal(healing);
            caster.heal(healing);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1, 1);
            caster.getWorld().spawnParticle(Particle.END_ROD, caster.getLocation(), 10, 3, 3, 3);
            caster.setEnergy(0);
        }
        moveCircles.runTaskTimer(Spellbook.getInstance().getImplementer(), 0, 1);
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        if (moveCircles != null) {
            moveCircles.cancel();
        }
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        placeholderNames.add("range");
        return super.getPlaceholders(c);
    }
}

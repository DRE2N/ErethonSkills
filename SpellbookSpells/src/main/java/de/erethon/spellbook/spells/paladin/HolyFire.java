package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HolyFire extends PaladinBaseSpell {

    private final int duration = data.getInt("duration", 10);
    private final float range = (float) data.getDouble("range", 0.8);
    private final double healAmount = data.getDouble("baseFinishHeal", 15);

    private CircleEffect circleEffect;

    public HolyFire(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        channelDuration = keepAliveTicks;
    }

    @Override
    public boolean onCast() {
        circleEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circleEffect.particle = Particle.SOUL_FIRE_FLAME;
        circleEffect.duration = keepAliveTicks * 50;
        circleEffect.iterations = -1;
        circleEffect.period = 20;
        circleEffect.particleCount = 32;
        circleEffect.radius = range;
        circleEffect.wholeCircle = true;
        circleEffect.enableRotation = false;
        circleEffect.setLocation(caster.getLocation().clone().add(0, 1, 0));
        circleEffect.start();
        return super.onCast();
    }

    @Override
    protected void onTick() {
        Set<LivingEntity> entities = new HashSet<>();
        caster.getNearbyEntities(range, 2, range).forEach(e -> {
            if (e instanceof LivingEntity living && Spellbook.canAttack(caster, living)) {
                living.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, living, false, Attribute.ADVANTAGE_MAGICAL));
                entities.add(living);
            }
        });
        triggerTraits(entities);
        Location loc = caster.getLocation().clone();
        loc.setPitch(0);
        loc.add(0, 1, 0);
        circleEffect.setLocation(loc);
    }

    @Override
    protected void onChannelFinish() {
        CircleEffect explode = new CircleEffect(Spellbook.getInstance().getEffectManager());
        explode.particle = Particle.SOUL_FIRE_FLAME;
        explode.duration = 20 * 50;
        explode.iterations = -1;
        explode.particleCount = 64;
        explode.radius = range;
        explode.wholeCircle = true;
        explode.enableRotation = false;
        Location loc = caster.getLocation().clone();
        loc.setPitch(0);
        loc.add(0, 1, 0);
        explode.setLocation(loc);
        explode.start();
        Set<LivingEntity> enemies = new HashSet<>();
        loc.getWorld().playSound(Sound.sound(org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, Sound.Source.RECORD, 1, 1), loc.getX(), loc.getY(), loc.getZ());
        BukkitRunnable explodeTask = new BukkitRunnable() {
            @Override
            public void run() {
                explode.radius += 0.1f;
                if (explode.isDone()) {
                    loc.getNearbyLivingEntities(explode.radius).forEach(e -> {
                        if (!Spellbook.canAttack(caster, e)) {
                            e.setHealth(e.getHealth() + healAmount + Spellbook.getScaledValue(data, caster, Attribute.STAT_HEALINGPOWER));
                        } else {
                            enemies.add(e);
                        }
                    });
                    cancel();
                }
            }
        };
        triggerTraits(enemies, 1);
        explodeTask.runTaskTimer(Spellbook.getInstance().getImplementer(), 0, 1);

    }

    @Override
    protected void cleanup() {
        circleEffect.cancel();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(range, VALUE_COLOR));
        placeholderNames.add("range");
        spellAddedPlaceholders.add(Component.text(healAmount + Spellbook.getScaledValue(data, caster, Attribute.STAT_HEALINGPOWER), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("healing");
        return super.getPlaceholders(c);
    }
}

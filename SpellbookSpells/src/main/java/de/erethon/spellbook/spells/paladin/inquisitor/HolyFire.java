package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.effect.ParticleEffect;
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

public class HolyFire extends InquisitorBaseSpell {

    // Creates an expanding circle of fire that damages enemies and heals allies
    // Does bonus healing based on judgment stacks on the enemies it has dealt damage to

    private final int duration = data.getInt("duration", 10);
    private final float rangeMin = (float) data.getDouble("rangeMin", 0.8);
    private final float rangeMax = (float) data.getDouble("rangeMax", 3.0);
    private final double healAmount = data.getDouble("baseFinishHeal", 15);
    private final double bonusHealPerJudgementStack = data.getDouble("bonusHealPerJudgementStack", 25);

    private CircleEffect circleEffect;
    private float range;

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
        range = (float) Spellbook.getRangedValue(data, caster,Attribute.ADVANTAGE_MAGICAL, rangeMin, rangeMax, "range");
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
        Set<LivingEntity> friends = new HashSet<>();
        loc.getWorld().playSound(Sound.sound(org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, Sound.Source.RECORD, 1, 1), loc.getX(), loc.getY(), loc.getZ());
        BukkitRunnable explodeTask = new BukkitRunnable() {
            @Override
            public void run() {
                explode.radius += 0.1f;
                if (explode.isDone()) {
                    loc.getNearbyLivingEntities(explode.radius).forEach(e -> {
                        if (!Spellbook.canAttack(caster, e)) {
                            e.setHealth(Math.max(e.getMaxHealth(), (e.getHealth() + healAmount + Spellbook.getScaledValue(data, caster, Attribute.STAT_HEALINGPOWER))));
                            friends.add(e);
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
        if (explode.isDone()) {
            int totalJudgementStacks = 0;
            for (LivingEntity living : enemies) {
                int judgementStacks = getJudgementStacksOnTarget(living);
                totalJudgementStacks += judgementStacks;
                for (int i = 0; i <= judgementStacks; i++) {
                    removeJudgement(living);
                }
            }
            int bonusHeal = (int) (bonusHealPerJudgementStack * totalJudgementStacks);
            for (LivingEntity living : friends) {
                living.setHealth(living.getHealth() + bonusHeal);
            }
        }
    }

    @Override
    protected void cleanup() {
        circleEffect.cancel();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, rangeMin, rangeMax, "range"), VALUE_COLOR));
        placeholderNames.add("range");
    }
}

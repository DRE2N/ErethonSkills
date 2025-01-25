package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCastEvent;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChainsOfPain extends PaladinBaseSpell implements Listener {

    private final double maxDistance = data.getDouble("maxDistance", 7);
    private final double lookForSecondTargetRange = data.getDouble("lookForSecondTargetRange", 5);
    private final EffectData stun = Spellbook.getEffectData("Stun");
    private final int stunDuration = data.getInt("stunDuration", 100);

    private LivingEntity secondTarget;
    private boolean linkActive = false;
    private LineEffect effect;
    private long lastPushTimeTime= 0;
    private final Set<SpellbookSpell> alreadyCasted = new HashSet<>();

    public ChainsOfPain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected boolean onPrecast() {
        if (!lookForTarget(data.getInt("range", 16))) return false;
        for (LivingEntity living : target.getLocation().getNearbyLivingEntities(lookForSecondTargetRange)) {
            if (living == caster) continue;
            if (!Spellbook.canAttack(caster, living)) continue;
            secondTarget = living;
            break;
        }
        if (secondTarget == null) {
            return false;
        }
        return super.onPrecast();
    }


    @Override
    public boolean onCast() {
        linkActive = true;
        effect = new LineEffect(Spellbook.getInstance().getEffectManager());
        effect.setLocation(target.getLocation().add(0, 1, 0));
        effect.setTargetLocation(secondTarget.getLocation().add(0, 1, 0));
        effect.infinite();
        effect.particle = Particle.DUST;
        effect.color = Color.MAROON;
        effect.particleSize = 0.3f;
        effect.start();
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, SoundCategory.RECORDS, 1, 0.5f);
        secondTarget.getWorld().playSound(secondTarget.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, SoundCategory.RECORDS, 1, 0.5f);
        return super.onCast();
    }

    @EventHandler
    public void onSpellCast(SpellCastEvent event) {
        if (target == null || secondTarget == null) return;
        SpellbookSpell activeSpell = event.getActiveSpell();
        if (alreadyCasted.contains(activeSpell)) return; // Don't want to loop forever
        LivingEntity eventCaster = event.getCaster();
        if (eventCaster == null) return;
        if (!Spellbook.canAttack(caster, eventCaster)) return;
        // TODO: Check if this actually works with all spells
        if (activeSpell instanceof SpellbookBaseSpell spell) {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    if (spell.getTarget() != null) {
                        try {
                            SpellbookBaseSpell newSpell = spell.getClass().getDeclaredConstructor(LivingEntity.class, SpellData.class).newInstance(eventCaster, spell.getData());
                            if (spell.getTarget() == target) {
                                newSpell.setTarget(secondTarget);
                            } else if (spell.getTarget() == secondTarget) {
                                newSpell.setTarget(target);
                            }
                            newSpell.onCast(); // let's hope no spell _requiRESISTANCE_ onPrecast to work. We don't want cooldown or mana costs to be applied here.
                            alreadyCasted.add(newSpell);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                        LineEffect spellDoubleEffect = new LineEffect(Spellbook.getInstance().getEffectManager());
                        spellDoubleEffect.setLocation(effect.getLocation());
                        spellDoubleEffect.setTargetLocation(effect.getTarget());
                        spellDoubleEffect.duration = 10 * 50;
                        spellDoubleEffect.particle = Particle.DUST;
                        spellDoubleEffect.color = Color.WHITE;
                        spellDoubleEffect.particleSize = 0.2f;
                        spellDoubleEffect.isZigZag = true;
                        spellDoubleEffect.zigZags = 16;
                        spellDoubleEffect.zigZagOffset = new Vector(0, 0.1f, 0);
                        spellDoubleEffect.start();
                    }
                }
            };
            runnable.runTaskLater(Spellbook.getInstance().getImplementer(), 1); // Wait one tick to make sure onPrecast of the original spell is done
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (target == null || secondTarget == null) return;
        if (event.getEntity() == target || event.getEntity() == secondTarget) {
            interrupt();
        }
    }

    @Override
    protected void onTick() {
        if (System.currentTimeMillis() - lastPushTimeTime > stunDuration + (stunDuration / 4) * 50L) {
            if (target.getLocation().distanceSquared(secondTarget.getLocation()) > maxDistance * maxDistance) {
                // Push them back together
                target.setVelocity(target.getLocation().toVector().subtract(secondTarget.getLocation().toVector()).normalize().multiply(-0.5));
                target.addEffect(caster, stun, stunDuration, 1);
                secondTarget.addEffect(caster, stun, stunDuration, 1);
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_DESTROY, SoundCategory.RECORDS, 1, 0.5f);
                secondTarget.getWorld().playSound(secondTarget.getLocation(), Sound.BLOCK_ANVIL_DESTROY, SoundCategory.RECORDS, 1, 0.5f);
            }
            lastPushTimeTime = System.currentTimeMillis();
        }
        if (linkActive) {
            effect.setLocation(target.getLocation().add(0, 1, 0));
            effect.setTargetLocation(secondTarget.getLocation().add(0, 1, 0));
        }
    }

    @Override
    protected void cleanup() {
        if (effect == null) return;
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, SoundCategory.RECORDS, 1, 1.5f);
        secondTarget.getWorld().playSound(secondTarget.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, SoundCategory.RECORDS, 1, 1.5f);
        effect.cancel();
        HandlerList.unregisterAll(this);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(maxDistance, VALUE_COLOR));
        placeholderNames.add("maxDistance");
        spellAddedPlaceholders.add(Component.text(lookForSecondTargetRange, VALUE_COLOR));
        placeholderNames.add("lookForSecondTargetRange");
        spellAddedPlaceholders.add(Component.text(stunDuration, VALUE_COLOR));
        placeholderNames.add("stunDuration");
        return super.getPlaceholders(c);
    }
}

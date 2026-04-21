package de.erethon.spellbook.spells.warrior.duelist;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Duel extends DuelistBaseSpell implements Listener {

    private final double maxDistance = data.getDouble("maxDistance", 5);
    private final int furyStacks = data.getInt("furyStacks", 3);
    private final int powerStacks = data.getInt("powerStacks", 3);

    private LivingEntity duelOpponent;
    private BukkitRunnable effects;

    private final Set<ItemDisplay> swords = new HashSet<>();

    public Duel(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        duelOpponent = target;
        caster.addEffect(caster, Spellbook.getEffectData("Fury"), keepAliveTicks, furyStacks);
        caster.addEffect(caster, Spellbook.getEffectData("Power"), keepAliveTicks, powerStacks);

        Location midpoint = getMidpoint();
        spawnCrossedSwords(midpoint);
        playDuelStartEffect(midpoint);

        caster.setGlowing(true);
        duelOpponent.setGlowing(true);

        effects = new BukkitRunnable() {
            @Override
            public void run() {
                playEffects();
            }
        };
        effects.runTaskTimer(Spellbook.getInstance().getImplementer(), 0, 10);
        return super.onCast();
    }

    private Location getMidpoint() {
        return caster.getLocation().toVector()
            .getMidpoint(duelOpponent.getLocation().toVector())
            .toLocation(caster.getWorld())
            .add(0, 1, 0);
    }

    private void spawnCrossedSwords(Location midpoint) {
        // Two swords angled toward each other — crossed dueling swords
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            float tiltAngle = (float) Math.toRadians(idx == 0 ? 30 : -30);
            ItemDisplay sword = midpoint.getWorld().spawn(midpoint.clone().add(0, 1, 0), ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.IRON_SWORD));
                d.setBillboard(Display.Billboard.FIXED);
                d.setPersistent(false);
                d.setGlowing(true);
                d.setGlowColorOverride(DUELIST_PRIMARY);
                d.setTeleportDuration(8);
                d.setTransformation(new Transformation(
                    new Vector3f(idx == 0 ? -0.4f : 0.4f, 0, 0),
                    new Quaternionf().rotateY(tiltAngle).rotateZ((float) Math.toRadians(-30)),
                    new Vector3f(2f, 2f, 2f),
                    new Quaternionf()
                ));
            });
            swords.add(sword);
        }
    }

    private void playDuelStartEffect(Location midpoint) {
        // Ring of blue dust at the midpoint
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            Location p = midpoint.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
            midpoint.getWorld().spawnParticle(Particle.DUST, p, 1, 0.05, 0.05, 0.05, 0,
                new Particle.DustOptions(DUELIST_PRIMARY, 1.2f));
        }
        midpoint.getWorld().spawnParticle(Particle.ENCHANTED_HIT, midpoint, 10, 0.5, 0.3, 0.5, 0.1);
        caster.getWorld().playSound(caster.getLocation(), org.bukkit.Sound.ITEM_SHIELD_BREAK, SoundCategory.RECORDS, 0.8f, 1.3f);
    }

    @Override
    protected void onTick() {
        if (caster.getLocation().distanceSquared(duelOpponent.getLocation()) > maxDistance * maxDistance) {
            duelOpponent.setVelocity(duelOpponent.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(-0.5));
        }
    }

    private void playEffects() {
        Location midpoint = getMidpoint();

        for (ItemDisplay sword : swords) {
            sword.teleport(midpoint.clone().add(0, 1, 0));
        }

        CircleEffect circleEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circleEffect.radius = 1.8f;
        circleEffect.enableRotation = true;
        circleEffect.orient = false;
        circleEffect.particle = org.bukkit.Particle.DUST;
        circleEffect.color = Color.fromRGB(58, 124, 201);
        circleEffect.particleSize = 0.8f;
        circleEffect.particles = 12;
        circleEffect.setLocation(midpoint);
        circleEffect.start();

        caster.getWorld().playSound(caster.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.RECORDS, 0.6f, 1f);
    }

    @EventHandler
    private void onDamage(PrePlayerAttackEntityEvent event) {
        if (event.getAttacked() != duelOpponent) return;
        if (event.getPlayer() != caster) {
            event.setCancelled(true);
            event.getPlayer().playSound(Sound.sound(org.bukkit.Sound.ITEM_SHIELD_BREAK, Sound.Source.RECORD, 1f, 1f));
        }
    }

    @Override
    protected void cleanup() {
        effects.cancel();
        caster.setGlowing(false);
        if (duelOpponent != null) duelOpponent.setGlowing(false);
        for (ItemDisplay sword : swords) {
            if (sword.isValid()) {
                sword.getLocation().getWorld().spawnParticle(Particle.DUST, sword.getLocation(), 8, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(DUELIST_PRIMARY, 1.0f));
                sword.remove();
            }
        }
        HandlerList.unregisterAll(this);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(maxDistance, VALUE_COLOR));
        placeholderNames.add("maxDistance");
        spellAddedPlaceholders.add(Component.text(furyStacks, VALUE_COLOR));
        placeholderNames.add("furyStacks");
        spellAddedPlaceholders.add(Component.text(powerStacks, VALUE_COLOR));
        placeholderNames.add("powerStacks");
        return super.getPlaceholders(c);
    }
}

package de.erethon.spellbook.spells.warrior.duelist;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Duel extends WarriorBaseSpell implements Listener {

    private final double maxDistance = data.getDouble("maxDistance", 5);
    private final int furyStacks = data.getInt("furyStacks", 3);
    private final int powerStacks = data.getInt("powerStacks", 3);

    private LivingEntity duelOpponent;
    private BukkitRunnable effects;

    private final Set<ItemDisplay> swords = new HashSet<>();

    public Duel(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
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
        Location midpoint = caster.getLocation().toVector().getMidpoint(duelOpponent.getLocation().toVector()).toLocation(caster.getWorld());
        midpoint = midpoint.add(0, 3, 0);
        ItemDisplay firstSword = caster.getWorld().spawn(midpoint, ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.IRON_SWORD));
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(2, 2, 2), new AxisAngle4f()));
        });
        ItemDisplay secondSword = caster.getWorld().spawn(midpoint, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.IRON_SWORD));
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(-2, 2, 2), new AxisAngle4f()));
        });
        swords.add(firstSword);
        swords.add(secondSword);
        effects = new BukkitRunnable() {
            @Override
            public void run() {
                playEffects();
            }
        };
        effects.runTaskTimer(Spellbook.getInstance().getImplementer(), 0, 10);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (caster.getLocation().distanceSquared(duelOpponent.getLocation()) > maxDistance * maxDistance) {
            duelOpponent.setVelocity(duelOpponent.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(-0.5));
        }
    }

    private void playEffects() {
        caster.getWorld().playSound(caster.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.RECORDS, 1, 1);
        CircleEffect circleEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circleEffect.radius = 2.2f;
        circleEffect.enableRotation = false;
        circleEffect.orient = false;
        circleEffect.particle = org.bukkit.Particle.FLAME;
        Location midpoint = caster.getLocation().toVector().getMidpoint(duelOpponent.getLocation().toVector()).toLocation(caster.getWorld()).add(0, 1, 0);
        midpoint.setPitch(0);
        for (ItemDisplay sword : swords) {
            sword.teleport(midpoint.add(0, 2, 0));
            sword.setTeleportDuration(10);
        }
        circleEffect.setLocation(midpoint);
        circleEffect.start();
    }

    @EventHandler
    private void onDamage(PrePlayerAttackEntityEvent event) {
        if (event.getAttacked() != duelOpponent) return;
        if (event.getPlayer() != caster) {
            event.setCancelled(true);
            event.getPlayer().playSound(Sound.sound(org.bukkit.Sound.ITEM_SHIELD_BREAK, Sound.Source.RECORD, 1, 1));
        }
    }

    @Override
    protected void cleanup() {
        effects.cancel();
        for (ItemDisplay sword : swords) {
            sword.remove();
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

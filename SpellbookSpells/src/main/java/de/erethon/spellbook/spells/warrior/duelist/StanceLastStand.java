package de.erethon.spellbook.spells.warrior.duelist;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

public class StanceLastStand extends AbstractWarriorStance {

    private static final Color DUELIST_PRIMARY = Color.fromRGB(58, 124, 201);
    private static final Color DUELIST_ACCENT = Color.fromRGB(192, 192, 192);
    private static final String PARRY_TAG = "duelist.parry";

    private final double damageReduction = data.getDouble("damageMultiplier", 0.7);
    private final double attackReduction = data.getDouble("attackMultiplier", 0.2);
    private final int hitsForParry = data.getInt("hitsForParry", 3);

    private ItemDisplay display;
    private final Transformation transformation = new Transformation(new Vector3f(0, 0, 1), new AxisAngle4f(0, 0, 0, 0), new Vector3f(1, 1, 1), new AxisAngle4f(0, 0, 1, 0));

    private int hitsTaken = 0;

    public StanceLastStand(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        display = caster.getWorld().spawn(caster.getLocation(), ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(new ItemStack(Material.SHIELD));
            itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIRSTPERSON_RIGHTHAND);
            itemDisplay.setGlowing(true);
            itemDisplay.setGlowColorOverride(DUELIST_PRIMARY);
            itemDisplay.setTransformation(transformation);
        });
        caster.addPassenger(display);
        playStanceActivationEffect();
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (display != null && display.isValid()) {
            display.setRotation(display.getLocation().getYaw() + 10, display.getLocation().getPitch());
        }
        if (currentTicks % 5 == 0) {
            playAuraEffect();
        }
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        playHitEffect();
        hitsTaken++;
        if (hitsTaken >= hitsForParry) {
            hitsTaken = 0;
            caster.getTags().add(PARRY_TAG);
            new BukkitRunnable() {
                @Override
                public void run() {
                    caster.getTags().remove(PARRY_TAG);
                }
            }.runTaskLater(Spellbook.getInstance().getImplementer(), 60L);
            playParryGrantedEffect();
        }
        return damage * damageReduction;
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        return damage * attackReduction;
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        playStanceEndEffect();
    }

    @Override
    protected void cleanup() {
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    private void playStanceActivationEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 20, 0.6, 0.8, 0.6, 0.05);
        caster.getWorld().spawnParticle(Particle.DUST, loc, 16, 0.5, 0.7, 0.5, 0,
            new Particle.DustOptions(DUELIST_ACCENT, 1.5f));
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.RECORDS, 0.8f, 1.2f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.RECORDS, 0.8f, 1.0f);
    }

    private void playAuraEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            Location p = loc.clone().add(Math.cos(angle) * 1.0, 0, Math.sin(angle) * 1.0);
            caster.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0.1, 0, 0,
                new Particle.DustOptions(DUELIST_ACCENT, 0.8f));
        }
    }

    private void playHitEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 6, 0.3, 0.3, 0.3, 0.05);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_STEP, SoundCategory.RECORDS, 0.7f, 1.4f);
    }

    private void playParryGrantedEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(Particle.DUST, loc, 12, 0.4, 0.4, 0.4, 0,
            new Particle.DustOptions(DUELIST_PRIMARY, 1.5f));
        caster.getWorld().spawnParticle(Particle.CRIT, loc, 6, 0.3, 0.3, 0.3, 0.1);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.RECORDS, 0.7f, 1.8f);
    }

    private void playStanceEndEffect() {
        if (display != null && display.isValid()) {
            Location loc = display.getLocation();
            loc.getWorld().spawnParticle(Particle.CRIT, loc, 12, 0.4, 0.4, 0.4, 0.15);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(DUELIST_ACCENT, 1.2f));
        }
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(damageReduction * 100, VALUE_COLOR));
        placeholderNames.add("damage reduction");
        spellAddedPlaceholders.add(Component.text(attackReduction * 100, VALUE_COLOR));
        placeholderNames.add("attack reduction");
        return super.getPlaceholders(c);
    }
}

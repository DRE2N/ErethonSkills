package de.erethon.spellbook.spells.warrior.duelist;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SideStep extends DuelistBaseSpell {

    private final double velocity = data.getDouble("velocity", 1.2f);
    private final double rotationAngle = data.getDouble("rotationAngle", 45);
    private float startYaw;
    private final List<ItemDisplay> afterimages = new ArrayList<>();

    public SideStep(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 5;
    }

    @Override
    public boolean onCast() {
        startYaw = caster.getYaw();
        spawnAfterimageSwords(caster.getLocation().clone());
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_STONE_BUTTON_CLICK_ON, Sound.Source.RECORD, 1f, 1f));
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, Sound.Source.RECORD, 0.8f, 1.5f));
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        Location loc = caster.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 2, 0.2, 0.3, 0.2, 0,
            new Particle.DustOptions(DUELIST_PRIMARY, 1.0f));

        float scale = Math.max(0f, 1.0f - (currentTicks / (float) keepAliveTicks));
        for (int i = 0; i < afterimages.size(); i++) {
            ItemDisplay img = afterimages.get(i);
            if (img == null || !img.isValid()) continue;
            float offsetX = i == 0 ? 0.3f : -0.3f;
            float tiltY = (float) Math.toRadians(i == 0 ? 20 : -20);
            img.setInterpolationDelay(-1);
            img.setInterpolationDuration(2);
            img.setTransformation(new Transformation(
                new Vector3f(offsetX, 0, 0),
                new Quaternionf().rotateY(tiltY).rotateZ((float) Math.toRadians(45)),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
            ));
        }
    }

    private void spawnAfterimageSwords(Location loc) {
        Location spawnLoc = loc.clone().add(0, 1, 0);
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            ItemDisplay sword = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.IRON_SWORD));
                d.setBillboard(Display.Billboard.FIXED);
                d.setPersistent(false);
                d.setGlowing(true);
                d.setGlowColorOverride(DUELIST_PRIMARY);
                d.setInterpolationDuration(2);
                float offsetX = idx == 0 ? 0.3f : -0.3f;
                float tiltY = (float) Math.toRadians(idx == 0 ? 20 : -20);
                d.setTransformation(new Transformation(
                    new Vector3f(offsetX, 0, 0),
                    new Quaternionf().rotateY(tiltY).rotateZ((float) Math.toRadians(45)),
                    new Vector3f(1f, 1f, 1f),
                    new Quaternionf()
                ));
            });
            afterimages.add(sword);
        }
    }

    @Override
    protected void onTickFinish() {
        if (startYaw >= caster.getYaw()) {
            caster.setVelocity(caster.getVelocity().add(caster.getLocation().getDirection().rotateAroundY(rotationAngle).multiply(velocity)));
        } else {
            caster.setVelocity(caster.getVelocity().add(caster.getLocation().getDirection().rotateAroundY(-rotationAngle).multiply(velocity)));
        }
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PHANTOM_SWOOP, Sound.Source.RECORD, 1f, 1.5f));

        caster.getTags().add(PARRY_TAG);
        Location loc = caster.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 6, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 4, 0.3, 0.3, 0.3, 0,
            new Particle.DustOptions(DUELIST_PRIMARY, 1.0f));

        new BukkitRunnable() {
            @Override
            public void run() {
                caster.getTags().remove(PARRY_TAG);
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 60L);

        for (ItemDisplay img : afterimages) {
            if (img != null && img.isValid()) img.remove();
        }
        afterimages.clear();
    }
}

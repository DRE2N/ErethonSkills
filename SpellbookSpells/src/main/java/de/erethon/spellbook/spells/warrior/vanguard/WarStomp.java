package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class WarStomp extends VanguardBaseSpell {

    private static final String SEISMIC_TAG = "vanguard.seismic";

    private final double shockwaveRadius = data.getDouble("shockwaveRadius", 4.0);
    private final double shockwaveHeight = data.getDouble("shockwaveHeight", 3.0);
    private final int minStunDuration = data.getInt("minStunDuration", 2) * 20;
    private final int maxStunDuration = data.getInt("maxStunDuration", 5) * 20;

    private final EffectData stunEffectData = Spellbook.getEffectData("Stun");

    private ItemDisplay windupDisplay;
    private final List<BlockDisplay> ringDisplays = new ArrayList<>();
    private int ringAge = 0;

    public WarStomp(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        spawnWindupDisplay();
        playWindupEffect();
        return super.onCast();
    }

    private void spawnWindupDisplay() {
        windupDisplay = caster.getWorld().spawn(caster.getLocation().add(0, 3.5, 0), ItemDisplay.class, d -> {
            d.setItemStack(new org.bukkit.inventory.ItemStack(Material.IRON_CHESTPLATE));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            d.setPersistent(false);
            d.setTeleportDuration(2);
        });
    }

    private void playWindupEffect() {
        caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation().add(0, 0.1, 0), 8, 0.5, 0.1, 0.5, 0.05);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.5f);
    }

    private void executeWarStomp() {
        removeWindupDisplay();
        playStompEffect();
        spawnImpactRing();

        createCircularAoE(caster.getLocation(), shockwaveRadius, shockwaveHeight, 40)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && Spellbook.canAttack(caster, entity)) {
                        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
                        entity.damage(physicalDamage, caster, PDamageType.PHYSICAL);

                        int stunDuration = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_PHYSICAL, minStunDuration, maxStunDuration, "stunDuration");
                        entity.addEffect(caster, stunEffectData, stunDuration, 1);

                        entity.getTags().add(SEISMIC_TAG);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                entity.getTags().remove(SEISMIC_TAG);
                            }
                        }.runTaskLater(Spellbook.getInstance().getImplementer(), 60L);

                        playHitEffect(entity);
                    }
                });
    }

    private void spawnImpactRing() {
        int count = 12;
        for (int i = 0; i < count; i++) {
            final int idx = i;
            double angle = Math.PI * 2 * i / count;
            double x = Math.cos(angle) * shockwaveRadius;
            double z = Math.sin(angle) * shockwaveRadius;
            BlockDisplay block = caster.getWorld().spawn(caster.getLocation().add(x, 0.2, z), BlockDisplay.class, d -> {
                d.setBlock(Material.STONE_BRICKS.createBlockData());
                d.setPersistent(false);
                d.setInterpolationDuration(3);
                d.setInterpolationDelay(-1);
                d.setTeleportDuration(2);
                d.setTransformation(new Transformation(
                    new Vector3f(-0.15f, 0, -0.15f),
                    new Quaternionf(),
                    new Vector3f(0.3f, 0.3f, 0.3f),
                    new Quaternionf()
                ));
            });
            ringDisplays.add(block);
        }
        ringAge = 0;
    }

    private void removeWindupDisplay() {
        if (windupDisplay != null && windupDisplay.isValid()) {
            windupDisplay.remove();
            windupDisplay = null;
        }
    }

    private void playStompEffect() {
        caster.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, caster.getLocation().add(0, 0.1, 0), 30, 1.5, 0.1, 1.5, 0.1,
            Material.STONE.createBlockData());
        caster.getWorld().spawnParticle(Particle.LARGE_SMOKE, caster.getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
    }

    private void playHitEffect(LivingEntity target) {
        target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.1,
            Material.STONE.createBlockData());
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.6f, 0.8f);
    }

    @Override
    protected void onTick() {
        super.onTick();

        // Move windup display downward over the first 20 ticks
        if (windupDisplay != null && windupDisplay.isValid() && currentTicks < 20) {
            if (currentTicks == 5 || currentTicks == 15) {
                double y = 3.5 - (currentTicks / 20.0) * 3.0;
                windupDisplay.teleport(caster.getLocation().add(0, y, 0));
            }
        }

        if (currentTicks < 20) return;

        if (currentTicks == 20) {
            executeWarStomp();
            return;
        }

        if (currentTicks >= 40) {
            cleanup();
            this.cancel();
            return;
        }

        // Animate ring displays shrinking outward
        ringAge = currentTicks - 20;
        double progress = ringAge / 20.0;
        float scale = (float) Math.max(0, 0.3 - progress * 0.3);

        for (BlockDisplay block : ringDisplays) {
            if (block == null || !block.isValid()) continue;
            block.setInterpolationDelay(-1);
            block.setInterpolationDuration(2);
            block.setTransformation(new Transformation(
                new Vector3f(-scale / 2, 0, -scale / 2),
                new Quaternionf(),
                new Vector3f(scale, scale * 2, scale),
                new Quaternionf()
            ));
        }

        // Radial wave particles
        double currentRadius = shockwaveRadius * progress;
        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2 * i / 16);
            double x = Math.cos(angle) * currentRadius;
            double z = Math.sin(angle) * currentRadius;
            caster.getWorld().spawnParticle(Particle.DUST,
                caster.getLocation().add(x, 0.2, z), 1, 0, 0, 0, 0,
                new Particle.DustOptions(org.bukkit.Color.GRAY, 1.0f));
        }
    }

    @Override
    protected void cleanup() {
        removeWindupDisplay();
        for (BlockDisplay block : ringDisplays) {
            if (block != null && block.isValid()) block.remove();
        }
        ringDisplays.clear();
    }
}

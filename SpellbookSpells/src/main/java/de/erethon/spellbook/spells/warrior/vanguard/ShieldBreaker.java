package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
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

public class ShieldBreaker extends VanguardBaseSpell {

    private final int range = data.getInt("range", 4);
    private final int stunDuration = data.getInt("stunDuration", 3) * 20;
    private final int slowDuration = data.getInt("slowDuration", 4) * 20;
    private final int slowStacks = data.getInt("slowStacks", 2);

    private final EffectData stabilityEffectData = Spellbook.getEffectData("Stability");
    private final EffectData resistanceEffectData = Spellbook.getEffectData("Resistance");
    private final EffectData stunEffectData = Spellbook.getEffectData("Stun");
    private final EffectData slowEffectData = Spellbook.getEffectData("Slow");

    private ItemDisplay windupDisplay;

    public ShieldBreaker(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        spawnWindupDisplay();
        playWindupEffect();

        new BukkitRunnable() {
            @Override
            public void run() {
                removeWindupDisplay();
                executeStrike();
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 10L);

        return super.onCast();
    }

    private void spawnWindupDisplay() {
        Location loc = caster.getLocation().add(0, 1.5, 0);
        windupDisplay = loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.IRON_SWORD));
            d.setBillboard(Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setGlowing(true);
            d.setInterpolationDuration(8);
            d.setInterpolationDelay(-1);
            d.setTeleportDuration(2);
            // Start raised, interpolate to drawn-back position
            Vector3f dir = new Vector3f((float) caster.getLocation().getDirection().getX(),
                0, (float) caster.getLocation().getDirection().getZ()).normalize();
            d.setTransformation(new Transformation(
                new Vector3f(-dir.x * 0.5f, 0.2f, -dir.z * 0.5f),
                new Quaternionf().rotateY((float) Math.atan2(-dir.x, dir.z)).rotateX(-(float) Math.PI / 4),
                new Vector3f(1.3f, 1.3f, 1.3f),
                new Quaternionf()
            ));
        });
    }

    private void removeWindupDisplay() {
        if (windupDisplay != null && windupDisplay.isValid()) {
            windupDisplay.remove();
            windupDisplay = null;
        }
    }

    private void playWindupEffect() {
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, caster.getLocation().add(0, 1.5, 0), 1, 0, 0, 0, 0);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_AXE_SCRAPE, 0.8f, 0.9f);
    }

    private void executeStrike() {
        boolean hasStability = target.hasEffect(stabilityEffectData);
        boolean hasResistance = target.hasEffect(resistanceEffectData);

        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        target.damage(physicalDamage, caster, PDamageType.PHYSICAL);

        playStrikeEffects();

        if (hasStability || hasResistance) {
            if (hasStability) target.removeEffect(stabilityEffectData);
            if (hasResistance) target.removeEffect(resistanceEffectData);

            target.addEffect(caster, stunEffectData, stunDuration, 1);

            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.2f);

            spawnShatterFragments(target.getLocation().add(0, 1, 0));
        } else {
            target.addEffect(caster, slowEffectData, slowDuration, slowStacks);
            target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 0.5, 0), 12, 0.3, 0.3, 0.3, 0.1,
                Material.DIRT.createBlockData());
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 0.8f);
        }
    }

    private void spawnShatterFragments(Location origin) {
        float[][] directions = {{1, 0}, {-1, 0}, {0, 1}};
        List<BlockDisplay> fragments = new ArrayList<>();
        for (float[] dir : directions) {
            BlockDisplay frag = origin.getWorld().spawn(origin, BlockDisplay.class, d -> {
                d.setBlock(Material.IRON_BLOCK.createBlockData());
                d.setPersistent(false);
                d.setInterpolationDuration(4);
                d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(0.25f, 0.25f, 0.25f),
                    new Quaternionf()
                ));
            });
            fragments.add(frag);
        }

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                for (int i = 0; i < fragments.size(); i++) {
                    BlockDisplay frag = fragments.get(i);
                    if (!frag.isValid()) continue;
                    float[] dir = directions[i];
                    frag.teleport(origin.clone().add(dir[0] * t * 0.15, t * 0.1, dir[1] * t * 0.15));
                }
                if (t >= 8) {
                    for (BlockDisplay frag : fragments) {
                        if (frag.isValid()) frag.remove();
                    }
                    cancel();
                }
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    private void playStrikeEffects() {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.2);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.9f);
    }
}

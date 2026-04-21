package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ExecutionersSwing extends VanguardBaseSpell {

    private static final Color HIGH_MOMENTUM_COLOR = Color.fromRGB(200, 40, 40);

    private final int range = data.getInt("range", 4);
    private final int channelDuration = data.getInt("channelDuration", 3) * 20;
    private final double missingHealthMultiplier = data.getDouble("missingHealthMultiplier", 2.0);
    private final double momentumDamagePerStack = data.getDouble("momentumDamagePerStack", 0.15);
    private final int momentumPerStack = data.getInt("momentumPerStack", 10);

    private int ticksElapsed = 0;
    private boolean hasExecuted = false;
    private int momentumConsumed = 0;

    private ItemDisplay channelDisplay;

    public ExecutionersSwing(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = channelDuration + 10;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        momentumConsumed = caster.getEnergy() / momentumPerStack;
        caster.setEnergy(0);

        spawnChannelDisplay();
        playChannelStartEffect();
        return super.onCast();
    }

    private void spawnChannelDisplay() {
        channelDisplay = caster.getWorld().spawn(caster.getLocation().add(0, 2.5, 0), ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.IRON_SWORD));
            d.setBillboard(Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setGlowing(true);
            d.setInterpolationDuration(5);
            d.setTeleportDuration(2);
            d.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf().rotateX((float) Math.PI),
                new Vector3f(1.0f, 1.0f, 1.0f),
                new Quaternionf()
            ));
        });
    }

    @Override
    protected void onTick() {
        ticksElapsed++;

        if (ticksElapsed <= channelDuration) {
            updateChannelDisplay();
            playChannelEffect();
        } else if (ticksElapsed == channelDuration + 1 && !hasExecuted) {
            executeSwing();
            hasExecuted = true;
        }
    }

    private void updateChannelDisplay() {
        if (channelDisplay == null || !channelDisplay.isValid()) return;

        channelDisplay.teleport(caster.getLocation().add(0, 2.5, 0));

        float progress = (float) ticksElapsed / channelDuration;
        float scale = 1.0f + progress * 1.5f;

        boolean highMomentum = momentumConsumed > 5;
        if (highMomentum) {
            channelDisplay.setGlowColorOverride(HIGH_MOMENTUM_COLOR);
        }

        channelDisplay.setInterpolationDelay(-1);
        channelDisplay.setTransformation(new Transformation(
            new Vector3f(0, 0, 0),
            new Quaternionf().rotateX((float) Math.PI),
            new Vector3f(scale, scale, scale),
            new Quaternionf()
        ));
    }

    private void playChannelStartEffect() {
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, caster.getLocation().add(0, 2, 0), 15, 0.5, 0.3, 0.5, 0.1);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.8f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 0.6f);
    }

    private void playChannelEffect() {
        if (ticksElapsed % 5 == 0) {
            double progress = (double) ticksElapsed / channelDuration;
            int particleCount = (int) (5 + progress * 20);

            caster.getWorld().spawnParticle(Particle.CRIT, caster.getLocation().add(0, 1.5, 0), particleCount, 0.3, 0.5, 0.3, 0.2);
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1.5, 0), (int) (particleCount * 0.5), 0.4, 0.3, 0.4, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), (float) (1.0 + progress)));

            float pitch = (float) (0.8f + progress * 0.6f);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.3f, pitch);
        }
    }

    private void executeSwing() {
        if (channelDisplay != null && channelDisplay.isValid()) {
            // Scale up then slam to target
            channelDisplay.setInterpolationDelay(-1);
            channelDisplay.setInterpolationDuration(3);
            channelDisplay.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf().rotateX((float) Math.PI),
                new Vector3f(3.0f, 3.0f, 3.0f),
                new Quaternionf()
            ));

            ItemDisplay finalDisplay = channelDisplay;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (finalDisplay.isValid()) {
                        finalDisplay.teleport(target.getLocation().add(0, 1, 0));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (finalDisplay.isValid()) finalDisplay.remove();
                            }
                        }.runTaskLater(Spellbook.getInstance().getImplementer(), 3L);
                    }
                }
            }.runTaskLater(Spellbook.getInstance().getImplementer(), 3L);
            channelDisplay = null;
        }

        double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        double missingHealthPercent = 1.0 - (target.getHealth() / target.getAttribute(Attribute.MAX_HEALTH).getValue());
        double missingHealthBonus = baseDamage * missingHealthMultiplier * missingHealthPercent;
        double momentumBonus = baseDamage * momentumDamagePerStack * momentumConsumed;
        double totalDamage = baseDamage + missingHealthBonus + momentumBonus;

        target.damage(totalDamage, caster, PDamageType.PHYSICAL);
        playExecutionEffects(missingHealthPercent > 0.5, momentumConsumed > 5);
    }

    private void playExecutionEffects(boolean isHighMissingHealth, boolean hasHighMomentum) {
        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 40, 0.6, 0.6, 0.6, 0.4);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.3);

        if (isHighMissingHealth) {
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 30, 0.7, 0.7, 0.7, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 2.5f));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.2f);
        }

        if (hasHighMomentum) {
            caster.getWorld().spawnParticle(Particle.FIREWORK, caster.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.5f);
        }

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 0.7f);
    }

    @Override
    protected void cleanup() {
        if (channelDisplay != null && channelDisplay.isValid()) {
            channelDisplay.remove();
            channelDisplay = null;
        }
    }
}

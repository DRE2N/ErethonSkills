package de.erethon.spellbook.spells.ranger.druid;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

public class SpiritGrove extends DruidBaseSpell {

    private final int groveDuration = data.getInt("groveDuration", 7) * 20;
    private final double radius = data.getDouble("radius", 5.5);
    private final double healMin = data.getDouble("healMin", 18);
    private final double healMax = data.getDouble("healMax", 55);
    private final double damageMin = data.getDouble("damageMin", 10);
    private final double damageMax = data.getDouble("damageMax", 35);
    private final double seedPulseBonus = data.getDouble("seedPulseBonus", 0.25);
    private final int pulseInterval = data.getInt("pulseInterval", 20);
    private final int regenerationDuration = data.getInt("regenerationDuration", 40);
    private final int slowDuration = data.getInt("slowDuration", 30);
    private final int slowStacks = data.getInt("slowStacks", 1);

    private Location targetLocation;
    private AoE groveAoE;
    private int pulseTicks;

    public SpiritGrove(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = groveDuration;
    }

    @Override
    protected boolean onPrecast() {
        if (!super.onPrecast()) {
            return false;
        }
        Block targetBlock = caster.getTargetBlockExact(range);
        if (targetBlock == null || !targetBlock.isSolid()) {
            return false;
        }
        targetLocation = targetBlock.getLocation().add(0.5, 1, 0.5);
        return true;
    }

    @Override
    public boolean onCast() {
        pulseTicks = pulseInterval;
        groveAoE = createCircularAoE(targetLocation, radius, 2, groveDuration)
            .onTick(aoe -> {
                if (caster.getTicksLived() % 10 == 0) {
                    aoe.getCenter().getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, aoe.getCenter().clone().add(0, 1, 0), 10, radius * 0.5, 0.7, radius * 0.5, 0.02);
                }
                if (caster.getTicksLived() % 20 == 0) {
                    aoe.getCenter().getWorld().spawnParticle(Particle.DUST, aoe.getCenter().clone().add(0, 0.2, 0), 18, radius * 0.6, 0.2, radius * 0.6, 0,
                        new Particle.DustOptions(Color.fromRGB(88, 170, 78), 1.2f));
                }
            })
            .addBlocksOnTopGroundLevel(1, Material.MOSS_CARPET, Material.PALE_MOSS_CARPET, Material.SHORT_GRASS)
            .sendBlockChanges();

        caster.getWorld().playSound(targetLocation, Sound.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.RECORDS, 1.0f, 0.8f);
        caster.getWorld().playSound(targetLocation, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.RECORDS, 0.7f, 1.6f);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (groveAoE == null) {
            return;
        }

        pulseTicks--;
        if (pulseTicks > 0) {
            return;
        }
        pulseTicks = pulseInterval;

        double heal = Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal");
        double damage = Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, damageMin, damageMax, "damage");

        for (LivingEntity entity : targetLocation.getNearbyLivingEntities(radius)) {
            if (entity.getLocation().distanceSquared(targetLocation) > radius * radius) {
                continue;
            }
            if (Spellbook.canAttack(caster, entity)) {
                int seeds = consumeSeeds(entity);
                entity.damage(damage * (1 + seeds * seedPulseBonus), caster);
                applySlow(entity, slowDuration + seeds * 10, slowStacks);
                if (seeds == 0) {
                    addSeed(entity, 1);
                }
                entity.getWorld().spawnParticle(Particle.MYCELIUM, entity.getLocation().add(0, 1, 0), 6, 0.3, 0.6, 0.3, 0);
                continue;
            }
            int seeds = consumeSeeds(entity);
            entity.heal(heal * (1 + seeds * seedPulseBonus));
            applyRegeneration(entity, regenerationDuration + seeds * 10, 1);
            if (seeds == 0) {
                addSeed(entity, 1);
            }
            entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0);
        }

        caster.getWorld().playSound(targetLocation, Sound.BLOCK_MOSS_STEP, SoundCategory.RECORDS, 0.4f, 1.2f);
    }

    @Override
    protected void cleanup() {
        if (groveAoE != null) {
            groveAoE.revertBlockChanges();
            groveAoE.end();
        }
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal"), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("heal");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, damageMin, damageMax, "damage"), ATTR_MAGIC_COLOR));
        placeholderNames.add("damage");
    }
}

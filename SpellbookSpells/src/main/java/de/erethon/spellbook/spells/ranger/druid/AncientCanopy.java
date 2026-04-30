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
import org.bukkit.entity.LivingEntity;

public class AncientCanopy extends DruidBaseSpell {

    private final int canopyDuration = data.getInt("canopyDuration", 9) * 20;
    private final int pulseInterval = data.getInt("pulseInterval", 20);
    private final double radius = data.getDouble("radius", 7.0);
    private final double healMin = data.getDouble("healMin", 30);
    private final double healMax = data.getDouble("healMax", 80);
    private final double damageMin = data.getDouble("damageMin", 18);
    private final double damageMax = data.getDouble("damageMax", 50);
    private final double seedBurstFactor = data.getDouble("seedBurstFactor", 0.45);
    private final int resistanceDuration = data.getInt("resistanceDuration", 45);
    private final int stabilityDuration = data.getInt("stabilityDuration", 35);
    private final int slowDuration = data.getInt("slowDuration", 45);

    private Location canopyLocation;
    private AoE canopyAoE;
    private int pulseTicks;

    public AncientCanopy(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = canopyDuration;
    }

    @Override
    protected boolean onPrecast() {
        if (!super.onPrecast()) {
            return false;
        }
        canopyLocation = getTargetGround(range);
        return canopyLocation != null;
    }

    @Override
    public boolean onCast() {
        pulseTicks = 1;
        canopyAoE = createCircularAoE(canopyLocation, radius, 4, canopyDuration)
            .onTick(aoe -> {
                if (caster.getTicksLived() % 10 == 0) {
                    aoe.getCenter().getWorld().spawnParticle(Particle.CHERRY_LEAVES, aoe.getCenter().clone().add(0, 2.6, 0), 12, radius * 0.55, 0.5, radius * 0.55, 0.02);
                }
            })
            .addBlocksOnTopGroundLevel(1, Material.MOSS_CARPET, Material.PALE_MOSS_CARPET, Material.FLOWERING_AZALEA_LEAVES, Material.SHORT_GRASS)
            .sendBlockChanges();

        canopyLocation.getWorld().spawnParticle(Particle.DUST, canopyLocation.clone().add(0, 1.2, 0), 42, radius * 0.55, 0.6, radius * 0.55, 0,
            new Particle.DustOptions(Color.fromRGB(176, 222, 111), 1.4f));
        canopyLocation.getWorld().playSound(canopyLocation, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.RECORDS, 1.0f, 1.45f);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (canopyLocation == null) {
            return;
        }
        pulseTicks--;
        if (pulseTicks > 0) {
            return;
        }
        pulseTicks = pulseInterval;

        double heal = Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal");
        double damage = Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, damageMin, damageMax, "damage");

        for (LivingEntity entity : canopyLocation.getNearbyLivingEntities(radius)) {
            if (entity.getLocation().distanceSquared(canopyLocation) > radius * radius) {
                continue;
            }
            int seeds = consumeSeeds(entity);
            if (isEnemy(entity)) {
                entity.damage(damage * (1 + seeds * seedBurstFactor), caster);
                applySlow(entity, slowDuration + seeds * 15, 1);
                if (seeds == 0) {
                    addSeed(entity, 1);
                }
                entity.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, entity.getLocation().add(0, 1, 0), 8, 0.35, 0.45, 0.35, 0.02);
                continue;
            }
            entity.heal(heal * (1 + seeds * seedBurstFactor));
            applyRegeneration(entity, pulseInterval + 20 + seeds * 10, 1);
            if (resistance != null) {
                entity.addEffect(caster, resistance, resistanceDuration, 1);
            }
            if (stability != null && seeds > 0) {
                entity.addEffect(caster, stability, stabilityDuration, 1);
            }
            entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 1, 0), 2 + seeds, 0.3, 0.45, 0.3, 0);
        }
        canopyLocation.getWorld().playSound(canopyLocation, Sound.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.RECORDS, 0.45f, 1.2f);
    }

    @Override
    protected void cleanup() {
        if (canopyAoE != null) {
            canopyAoE.revertBlockChanges();
            canopyAoE.end();
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

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

public class BrambleSnare extends DruidBaseSpell {

    private final double radius = data.getDouble("radius", 3.5);
    private final double damageMin = data.getDouble("damageMin", 25);
    private final double damageMax = data.getDouble("damageMax", 70);
    private final int slowDuration = data.getInt("slowDuration", 45);
    private final int seededSlowDuration = data.getInt("seededSlowDuration", 85);
    private final int aoeDuration = data.getInt("aoeDuration", 60);

    private Location snareLocation;
    private AoE snareAoE;

    public BrambleSnare(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = aoeDuration;
    }

    @Override
    protected boolean onPrecast() {
        if (!super.onPrecast()) {
            return false;
        }
        snareLocation = getTargetGround(range);
        return snareLocation != null;
    }

    @Override
    public boolean onCast() {
        double damage = Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, damageMin, damageMax, "damage");
        snareAoE = createCircularAoE(snareLocation, radius, 2, aoeDuration)
            .addBlocksOnTopGroundLevel(1, Material.MOSS_CARPET, Material.SHORT_GRASS, Material.DEAD_BUSH)
            .sendBlockChanges();

        for (LivingEntity entity : snareLocation.getNearbyLivingEntities(radius)) {
            if (!isEnemy(entity) || entity.getLocation().distanceSquared(snareLocation) > radius * radius) {
                continue;
            }
            int seeds = consumeSeeds(entity);
            entity.damage(damage * (1 + seeds * 0.25), caster);
            applySlow(entity, seeds > 0 ? seededSlowDuration : slowDuration, 1);
            if (seeds == 0) {
                addSeed(entity, 1);
            }
            entity.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().add(0, 0.8, 0), 10, 0.3, 0.35, 0.3, 0, Material.VINE.createBlockData());
        }

        snareLocation.getWorld().spawnParticle(Particle.DUST, snareLocation, 24, radius * 0.45, 0.15, radius * 0.45, 0,
            new Particle.DustOptions(Color.fromRGB(77, 138, 64), 1.2f));
        snareLocation.getWorld().playSound(snareLocation, Sound.BLOCK_ROOTS_BREAK, SoundCategory.RECORDS, 1.0f, 0.7f);
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        if (snareAoE != null) {
            snareAoE.revertBlockChanges();
            snareAoE.end();
        }
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, damageMin, damageMax, "damage"), ATTR_MAGIC_COLOR));
        placeholderNames.add("damage");
    }
}

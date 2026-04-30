package de.erethon.spellbook.spells.ranger.druid;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class VineStep extends DruidBaseSpell {

    private final int maxHeight = data.getInt("maxHeight", 16);
    private final double terrainPullStrength = data.getDouble("terrainPullStrength", 1.25);
    private final double enemyPullStrength = data.getDouble("enemyPullStrength", 1.05);
    private final int seededSlowDuration = data.getInt("seededSlowDuration", 55);
    private final int normalSlowDuration = data.getInt("normalSlowDuration", 30);

    private Block targetBlock;
    private LivingEntity targetEntity;

    public VineStep(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        if (!super.onPrecast()) {
            return false;
        }
        Entity entity = caster.getTargetEntity(range);
        if (entity instanceof LivingEntity living && isEnemy(living)) {
            targetEntity = living;
            return true;
        }
        targetBlock = caster.getTargetBlockExact(range);
        if (targetBlock == null || !targetBlock.isSolid()) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        int heightDiff = targetBlock.getY() - caster.getLocation().getBlockY();
        if (heightDiff < -4 || heightDiff > maxHeight) {
            caster.sendParsedActionBar("<color:#ff0000>Zu hoch!");
            return false;
        }
        return true;
    }

    @Override
    public boolean onCast() {
        if (targetEntity != null) {
            int seeds = consumeSeeds(targetEntity);
            Vector pull = caster.getLocation().toVector().subtract(targetEntity.getLocation().toVector());
            pullEntity(targetEntity, pull, enemyPullStrength + seeds * 0.12);
            applySlow(targetEntity, seeds > 0 ? seededSlowDuration : normalSlowDuration, 1);
            if (seeds == 0) {
                addSeed(targetEntity, 1);
            }
            playVineLine(caster.getEyeLocation(), targetEntity.getLocation().add(0, 1, 0), 16);
            targetEntity.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, targetEntity.getLocation().add(0, 1, 0), 8, 0.35, 0.45, 0.35, 0.02);
            targetEntity.getWorld().playSound(targetEntity.getLocation(), Sound.BLOCK_VINE_STEP, SoundCategory.RECORDS, 0.8f, 0.8f);
            return super.onCast();
        }

        Vector pull = targetBlock.getLocation().add(0.5, 0.7, 0.5).toVector().subtract(caster.getLocation().toVector());
        pullEntity(caster, pull, terrainPullStrength);
        playVineLine(caster.getEyeLocation(), targetBlock.getLocation().add(0.5, 0.5, 0.5), 16);
        caster.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, caster.getLocation().add(0, 1, 0), 6, 0.25, 0.3, 0.25, 0.02);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_VINE_PLACE, SoundCategory.RECORDS, 0.8f, 1.2f);
        return super.onCast();
    }
}

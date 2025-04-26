package de.erethon.spellbook.spells.ranger.pathfinder;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class HealingWave extends RangerBaseSpell {

    private final Set<FallingBlock> blocks = new HashSet<>();

    public HealingWave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 500;
    }

    @Override
    public boolean onCast() {
        Location base = caster.getLocation().add(caster.getLocation().getDirection().multiply(2));
        Vector direction = caster.getLocation().getDirection();
        for (int i = 0; i < 10; i++) {
            Location location = base.clone().add(direction.getX() + i, direction.getY(), direction.getZ());
            FallingBlock block = base.getWorld().spawnFallingBlock(location, Material.BLUE_CONCRETE.createBlockData());
            block.setCancelDrop(true);
            block.setHurtEntities(false);
            block.setVelocity(caster.getLocation().getDirection().multiply(1.5f));
            blocks.add(block);
        }
        for (LivingEntity livingEntity : base.getNearbyLivingEntities(3)) {
            if (livingEntity.getLocation().distanceSquared(caster.getLocation()) > 9) continue;
            livingEntity.setHealth(Math.min(livingEntity.getHealth() + Spellbook.getScaledValue(data, caster, Attribute.STAT_HEALINGPOWER), livingEntity.getAttribute(Attribute.MAX_HEALTH).getValue()));
            caster.getWorld().playSound(livingEntity.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 0.5f);
            caster.getWorld().spawnParticle(Particle.HEART, livingEntity.getLocation().clone().add(0, 1, 0), 4, 1, 0, 1);
        }
        return super.onCast();
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        for (FallingBlock block : blocks) {
            block.remove();
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
    }
}

package de.erethon.spellbook.spells.ranger.hawkeye;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class HuntersLeap extends RangerBaseSpell {

    // Target a location on the ground within range. Perform a high, arcing leap towards that location, quickly traversing the distance. Triggers Flow State upon landing.

    private final int range = data.getInt("range", 16);
    private final double arcHeight = data.getDouble("arcHeight", 6.0);

    private Block targetBlock;

    public HuntersLeap(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 40;
    }

    @Override
    protected boolean onPrecast() {
        targetBlock = caster.getTargetBlockExact(range);
        if (targetBlock == null || targetBlock.getType().isAir()) {
            caster.sendParsedActionBar("<red>Target Block not found!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (targetBlock == null) {
            return;
        }
        if (caster.getLocation().distance(targetBlock.getLocation()) < 0.3f) {
            addFlow();
            cleanup();
            return;
        }
        caster.getWorld().spawnParticle(Particle.CLOUD, targetBlock.getLocation().add(0, 1, 0), 1);
        // Arc vector
        double x = targetBlock.getX() - caster.getLocation().getX();
        Vector vector = getVector(x);
        caster.setVelocity(vector);
    }

    private Vector getVector(double x) {
        double y = targetBlock.getY() - caster.getLocation().getY();
        double z = targetBlock.getZ() - caster.getLocation().getZ();
        double distance = Math.sqrt(x * x + y * y + z * z);
        double arcX = x / distance * arcHeight;
        double arcY = y / distance * arcHeight;
        double arcZ = z / distance * arcHeight;
        return new Vector(arcX, arcY, arcZ);
    }
}

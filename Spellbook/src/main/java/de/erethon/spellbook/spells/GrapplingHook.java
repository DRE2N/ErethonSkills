package de.erethon.spellbook.spells;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.caster.SpellCaster;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GrapplingHook extends SpellbookSpell {

    Block targetBlock = null;
    Vector vec;

    public GrapplingHook(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        targetBlock = caster.getEntity().getTargetBlock(data.getInt("range", 64));
        if (targetBlock == null) {
            caster.sendActionbar("<red>Kein Target!");
            return false;
        }
        if (targetBlock.getType() != Material.RED_CONCRETE) {
            caster.sendActionbar("<red>Kein Grappling-Target!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        vec = caster.getLocation().clone().toVector().subtract(targetBlock.getLocation().clone().toVector());
        LineEffect line = new LineEffect(effectManager);
        line.setLocation(caster.getLocation().add(0, -1, 0));
        line.setTargetLocation(targetBlock.getLocation().add(0.5, 0.5, 0.5));
        line.particle = Particle.REDSTONE;
        line.particleSize = 0.3f;
        line.color = Color.WHITE;
        line.iterations = 20;
        line.start();
        keepAliveTicks = 10;
        return true;
    }

    @Override
    protected void onTickFinish() {
        double speed = -1 * data.getDouble("speedModifier", 2);
        LivingEntity entity = caster.getEntity();
        entity.setFallDistance(0);
        entity.setVelocity(vec.multiply(speed));
    }
}

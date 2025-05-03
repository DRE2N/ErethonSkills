package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class VineStep extends RangerBaseSpell {

    // Leashes out with a vine, pulling the caster towards the target block. If the target is a living entity, it will be pulled towards the caster.

    private Block targetBlock = null;
    private LivingEntity targetLivingEntity = null;
    private Vector vec;

    public VineStep(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        Entity entity = caster.getTargetEntity(32);
        if (entity instanceof LivingEntity) {
            targetLivingEntity = (LivingEntity) entity;
            return true;
        }
        targetBlock = caster.getTargetBlockExact(data.getInt("range", 64));
        if (targetBlock == null) {
            caster.sendParsedActionBar("<color:#ff0000>Kein Ziel gefunden!");
            return false;
        }
        int heightDiff = targetBlock.getY() - caster.getLocation().getBlockY();
        if (heightDiff < 0 || heightDiff > data.getInt("maxHeight", 16)) {
            caster.sendParsedActionBar("<color:#ff0000>Zu hoch!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        if (targetBlock != null) {
            vec = caster.getLocation().clone().toVector().subtract(targetBlock.getLocation().clone().toVector());
            LineEffect line = new LineEffect(Spellbook.getInstance().getEffectManager());
            line.setLocation(caster.getLocation().add(0, -1, 0));
            line.setTargetLocation(targetBlock.getLocation().add(0.5, 0.5, 0.5));
            line.particle = Particle.DUST;
            line.particleSize = 0.3f;
            line.color = Color.GREEN;
            line.iterations = 20;
            line.start();
            keepAliveTicks = 10;
            return true;
        }
        if (targetLivingEntity != null) {
            vec = targetLivingEntity.getLocation().clone().toVector().subtract(caster.getLocation().clone().toVector());
            LineEffect line = new LineEffect(Spellbook.getInstance().getEffectManager());
            line.setEntity(caster);
            line.setTargetEntity(targetLivingEntity);
            line.particle = Particle.DUST;
            line.particleSize = 0.3f;
            line.color = Color.GREEN;
            line.iterations = 20;
            line.start();
            keepAliveTicks = 10;
            return true;
        }
        return false;
    }

    @Override
    protected void cleanup() {
        double speed = -1 * data.getDouble("speedModifier", 2);
        caster.setFallDistance(0);
        if (targetBlock != null) {
            caster.setVelocity(vec.multiply(speed));
        }
        if (targetLivingEntity != null) {
            // Pull them together
            caster.setVelocity(vec.multiply(speed / 2));
            if (Spellbook.getInstance().isCCImmune(targetLivingEntity)) {
                return;
            }
            targetLivingEntity.setVelocity(vec.multiply(speed / 2).multiply(-1));
        }
    }

}

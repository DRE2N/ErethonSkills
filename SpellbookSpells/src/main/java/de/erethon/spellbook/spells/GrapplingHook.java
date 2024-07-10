package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.SpellCaster;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class GrapplingHook extends SpellbookSpell {

    Block targetBlock = null;
    Vector vec;

    public GrapplingHook(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        targetBlock = caster.getTargetBlockExact(data.getInt("range", 64));
        if (targetBlock == null) {
            caster.sendParsedActionBar("<color:#ff0000>Kein Ziel gefunden!");
            return false;
        }
        if (targetBlock.getType() != Material.RED_CONCRETE) {
            caster.sendParsedActionBar("<color:#ff0000>Kein Grappling-Ziel gefunden!");

            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        vec = caster.getLocation().clone().toVector().subtract(targetBlock.getLocation().clone().toVector());
        LineEffect line = new LineEffect(Spellbook.getInstance().getEffectManager());
        line.setLocation(caster.getLocation().add(0, -1, 0));
        line.setTargetLocation(targetBlock.getLocation().add(0.5, 0.5, 0.5));
        line.particle = Particle.DUST;
        line.particleSize = 0.3f;
        line.color = Color.WHITE;
        line.iterations = 20;
        line.start();
        keepAliveTicks = 10;
        return true;
    }

    @Override
    protected void cleanup() {
        double speed = -1 * data.getDouble("speedModifier", 2);
        caster.setFallDistance(0);
        caster.setVelocity(vec.multiply(speed));
    }
}

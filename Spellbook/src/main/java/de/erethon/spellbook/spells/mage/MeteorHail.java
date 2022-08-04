package de.erethon.spellbook.spells.mage;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Fireball;

import java.util.Random;

/**
 * @author Fyreum
 */
public class MeteorHail extends MageBaseSpell {

    int radius;
    Block targetBlock;
    Random random;

    public MeteorHail(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
        radius = data.getInt("radius", 5);
    }

    @Override
    protected boolean onPrecast() {
        targetBlock = caster.getEntity().getTargetBlock(64);
        if (targetBlock == null) {
            caster.sendActionbar("<red>Kein Target!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        random = new Random();
        tickInterval = 10;
        keepAliveTicks = 100;
        return true;
    }

    @Override
    protected void onTick() {
        Location location = targetBlock.getLocation();
        int xOffset = (random.nextBoolean() ? 1 : -1) * random.nextInt(radius + 1);
        int zOffset = (random.nextBoolean() ? 1 : -1) * random.nextInt(radius + 1);
        Location castLocation = location.clone().add(xOffset, 20, zOffset);
        Fireball meteor = castLocation.getWorld().spawn(castLocation, Fireball.class);
        meteor.setShooter(caster.getEntity());
        meteor.setVelocity(location.subtract(castLocation).toVector().multiply(10));
    }

    @Override
    public void onAfterCast() {
        caster.setCooldown(data);
    }

}

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
        tickInterval = 5;
        keepAliveTicks = 100;
        return true;
    }

    @Override
    protected void onTick() {
        Location castLocation = getOffsetLocation(20);
        Fireball meteor = castLocation.getWorld().spawn(castLocation, Fireball.class);
        meteor.setShooter(caster.getEntity());
        meteor.setIsIncendiary(false);
        meteor.setYield(0F);
        meteor.setSilent(true);
        meteor.setDirection(getOffsetLocation(0).subtract(castLocation).toVector());
        meteor.setVelocity(meteor.getDirection().multiply(10));
    }

    private Location getOffsetLocation(int yOffset) {
        Location location = targetBlock.getLocation();
        int xOffset = (random.nextBoolean() ? 1 : -1) * random.nextInt(radius + 1);
        int zOffset = (random.nextBoolean() ? 1 : -1) * random.nextInt(radius + 1);
        return location.clone().add(xOffset, yOffset, zOffset);
    }

    @Override
    public void onAfterCast() {
        caster.setCooldown(data);
    }

}

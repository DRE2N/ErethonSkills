package de.erethon.spellbook.spells.archer;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class ArrowRain extends ArcherBaseSpell {

    int radius;
    Block targetBlock;
    Random random;

    public ArrowRain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        radius = data.getInt("radius", 5);
    }

    @Override
    protected boolean onPrecast() {
        targetBlock = caster.getTargetBlock(64);
        if (targetBlock == null) {
            caster.sendActionbar("<color:#ff0000>Kein Ziel gefunden!");
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
        Arrow arrow = castLocation.getWorld().spawn(castLocation, Arrow.class);
        arrow.setShooter(caster);
        arrow.setSilent(true);
        arrow.getLocation().setDirection(getOffsetLocation(0).subtract(castLocation).toVector());
        arrow.setVelocity(arrow.getLocation().getDirection().multiply(5));
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

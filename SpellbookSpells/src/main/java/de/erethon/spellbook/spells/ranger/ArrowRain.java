package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Random;

public class ArrowRain extends RangerBaseSpell implements Listener {

    int radius;
    Block targetBlock;
    Random random;

    public ArrowRain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        radius = data.getInt("radius", 5);
    }

    @Override
    protected boolean onPrecast() {
        targetBlock = caster.getTargetBlockExact(64);
        if (targetBlock == null) {
            caster.sendParsedActionBar("<color:#ff0000>Kein Ziel gefunden!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        random = new Random();
        tickInterval = 5;
        keepAliveTicks = 100;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return true;
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() != caster || event.getHitEntity() == null || !(event.getHitEntity() instanceof LivingEntity)) return;
        triggerTraits((LivingEntity) event.getHitEntity());
    }

    @Override
    protected void onTick() {
        Location castLocation = getOffsetLocation(20);
        Arrow arrow = castLocation.getWorld().spawn(castLocation, Arrow.class);
        arrow.setShooter(caster);
        arrow.setSilent(true);
        arrow.getLocation().setDirection(getOffsetLocation(0).subtract(castLocation).toVector());
        arrow.setVelocity(arrow.getLocation().getDirection().multiply(2));
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

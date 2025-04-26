package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class ShadowTether extends AssassinBaseSpell {

    // Throws a tether forward, that temporarily binds the target (can't move way further from the caster) and pulls them towards the caster.

    private final int warmupTicks = data.getInt("warmupTicks", 10);
    private final double tetherSpeed = data.getDouble("tetherSpeed", 1.1);

    private int tick = 0;
    private Location tetherGraspLocation;
    private LivingEntity tetheredEntity;

    public ShadowTether(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        return super.onCast();
    }

    @Override
    protected void onTick() {
        tick++;
        if (tick < warmupTicks) {
            return;
        }
        // Start the tether
        if (tick == warmupTicks) {
            caster.getWorld().spawnParticle(Particle.WHITE_ASH, caster.getLocation(), 3);
            tetherGraspLocation = caster.getLocation();
        }
        // If we are already tethering an entity, pull them towards the caster
        if (tetheredEntity != null) {
            if (tetheredEntity.getLocation().distanceSquared(caster.getLocation()) > 2) {
                tetheredEntity.setVelocity(caster.getLocation().subtract(tetheredEntity.getLocation()).toVector().normalize().multiply(tetherSpeed));
                caster.getWorld().playSound(tetheredEntity, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 0.1f);
            }
            return;
        }
        // Slowly move the tether forward if we are not tethering an entity
        Location location = caster.getLocation().clone();
        location.setPitch(0);
        location.setYaw(caster.getLocation().getYaw());
        tetherGraspLocation.add(location.getDirection().multiply(tetherSpeed));
        caster.getWorld().spawnParticle(Particle.SMOKE, tetherGraspLocation, 3);
        for (LivingEntity living : tetherGraspLocation.getNearbyLivingEntities(2)) {
            if (living == caster) {
                continue;
            }
            tetheredEntity = living;
            triggerTraits(tetheredEntity, 0);
            if (tetheredEntity.getTags().contains("assassin.daggerthrow.marked")) {
                triggerTraits(tetheredEntity, 1);
                tetheredEntity.getTags().remove("assassin.daggerthrow.marked");
            }
            break;
        }
    }
}

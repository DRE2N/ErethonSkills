package de.erethon.spellbook.spells.ranger.hawkeye;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.utils.RangerUtils;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PiercingArrows extends ProjectileRelatedSkill {

    private final int range = data.getInt("range", 3);
    private final double maxMinAngle = data.getDouble("maxMinAngle", 70);

    public PiercingArrows(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        affectedArrows = 100;
        trailColor = Color.GRAY;
        keepAliveTicks = data.getInt("duration", 100);
    }

    @Override
    protected void onDamage(EntityDamageByEntityEvent event, Projectile projectile) {
        Set<LivingEntity> affected = new HashSet<>();
        for (LivingEntity living : event.getEntity().getLocation().getNearbyLivingEntities(range)) {
            if (living == caster || living == event.getEntity()) {
                continue;
            }
            Vector cDir = caster.getLocation().getDirection();
            Vector eDir = event.getEntity().getLocation().getDirection();
            double degrees = (Math.atan2(cDir.getX() * eDir.getZ() - cDir.getZ() * eDir.getX(), cDir.getX() * eDir.getX() + cDir.getZ() * eDir.getZ()) * 180) / Math.PI;
            if (Spellbook.getInstance().isDebug()) {
                caster.sendMessage("Degrees: " + degrees);
            }
            if (degrees <= maxMinAngle && degrees >= -maxMinAngle) {
                RangerUtils.sendProjectile((LivingEntity) event.getEntity(), living, caster, 2, Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADVANTAGE_MAGICAL), PDamageType.MAGIC);
                affected.add(living);
            }
        }
        triggerTraits(affected);
    }



}

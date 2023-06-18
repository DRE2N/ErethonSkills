package de.erethon.spellbook.spells.ranger;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

public class RicochetArrow extends RangerBaseSpell implements Listener {

    private final int ricochetRange = data.getInt("ricochetRange", 7);
    private final int maxRicochets = data.getInt("maxRicochets", 8);
    private final double damageReductionPerRicochet = data.getDouble("damageReductionPerRicochet", 1);
    private final int projectileSpeed = data.getInt("projectileSpeed", 2);

    private Projectile initialProjectile;
    private int ricochets = 0;

    public RicochetArrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(true);
    }

    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        initialProjectile = sendProjectile(caster, target);
        return true;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() != initialProjectile) {
            return;
        }
        for (LivingEntity living : event.getHitEntity().getLocation().getNearbyLivingEntities(ricochetRange)) {
            if (living == caster || living == event.getHitEntity()) {
                continue;
            }
            if (ricochets >= maxRicochets) {
                return;
            }
            sendProjectile((LivingEntity) event.getHitEntity(), living);
            ricochets++;
        }
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
    }

    private AbstractArrow sendProjectile(LivingEntity start, LivingEntity target) {
        Vector from = start.getLocation().toVector();
        Vector to = target.getLocation().toVector();
        Vector direction = to.subtract(from);
        direction = direction.normalize();
        direction.multiply(projectileSpeed);
        AbstractArrow proj = start.launchProjectile(Arrow.class, direction);
        //proj.setGravity(false);
        proj.setShooter(caster);
        proj.setDamageType(DamageType.MAGIC);
        proj.setDamage(Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADV_MAGIC) - (ricochets * damageReductionPerRicochet));
        return proj;
    }
}

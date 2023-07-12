package de.erethon.spellbook.spells.ranger;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.utils.RangerUtils;
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
        initialProjectile = RangerUtils.sendProjectile(caster, target, caster,  projectileSpeed, Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADV_MAGIC), DamageType.MAGIC);
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
            RangerUtils.sendProjectile((LivingEntity) event.getHitEntity(), living, caster,  projectileSpeed,
                    Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADV_MAGIC) - (ricochets * damageReductionPerRicochet) , DamageType.MAGIC);
            ricochets++;
        }
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
    }

}

package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftArrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;

import java.util.HashSet;
import java.util.Set;

public class MagicEye extends ProjectileRelatedSkill {

    private final double range = data.getDouble("range", 32);
    private final double xzRange = data.getDouble("arrow.xzRange", 1.2);
    private final double yRange = data.getDouble("arrow.yRange", 1.5);

    private final Set<LivingEntity> affected = new HashSet<>();
    private final Set<AbstractArrow> shotArrows = new HashSet<>();
    private final Set<AbstractArrow> removal = new HashSet<>();

    public MagicEye(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        shouldEndWhenProjectileHits = false;
        trailColor = Color.RED;
        keepAliveTicks = spellData.getInt("duration", 1000);
    }

    @Override
    protected boolean onCast() {
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            if (living == caster || !Spellbook.canAttack(caster, living)) continue;
            living.setGlowing(true);
            affected.add(living);
        }

        return super.onCast();
    }

    @Override
    protected void onShoot(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        arrow.setGlowing(true);
        shotArrows.add(arrow);
        CraftArrow projectile = (CraftArrow) arrow;
        projectile.getHandle().setNoPhysics(true);
        event.setConsumeItem(false);
        if (event.getEntity() instanceof Player player) {
            player.updateInventory(); // Client might disagree about the arrow
        }
    }

    @Override
    protected void onTick() {
        for (AbstractArrow arrow : shotArrows) {
            arrow.getLocation().getNearbyLivingEntities(xzRange, yRange).forEach(living -> {
                if (living == caster || !Spellbook.canAttack(caster, living)) return;
                living.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADV_PHYSICAL), caster, DamageType.PHYSICAL);
                arrow.remove();
                removal.add(arrow);
            });
        }
        shotArrows.removeAll(removal);
        removal.clear();
    }

    @Override
    protected void cleanup() {
        for (LivingEntity living : affected) {
            living.setGlowing(false);
        }
        for (AbstractArrow arrow : shotArrows) {
            arrow.setGlowing(false);
            arrow.remove();
        }
        super.cleanup();
    }
}
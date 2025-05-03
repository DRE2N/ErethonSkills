package de.erethon.spellbook.spells.ranger.hawkeye;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import de.erethon.spellbook.utils.RangerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

public class BowstringSnap extends RangerBaseSpell implements Listener {

    //  Fire a blunt arrow. Deals minimal damage but Knocks Back the target hit and propels the Hawkeye backwards a short distance.
    //  Grant Flow State upon successful hit.

    private final double knockbackModifier = data.getDouble("knockbackModifier", 1.3f);
    private final double projectileSpeed = data.getInt("projectileSpeed", 2);

    private Arrow arrow;

    public BowstringSnap(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 200; // 10 seconds is enough to get the arrow to hit something
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        arrow = caster.launchProjectile(Arrow.class, caster.getLocation().getDirection().multiply(projectileSpeed));
        return super.onCast();
    }

    @EventHandler
    private void onHit(ProjectileHitEvent event) {
        if (event.getEntity() != arrow) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity living && Spellbook.canAttack(caster, living)) {
            Vector arrowDirection = arrow.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize();
            Vector knockback = arrowDirection.multiply(knockbackModifier);
            living.setVelocity(living.getVelocity().add(knockback));
            if (!inFlowState()) {
                addFlow();
            }
        }
    }


}

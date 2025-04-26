package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class ShadowJump extends AssassinBaseSpell {

    // RMB. Dashes forward, becoming invisible for a short time if passing through an entity.

    public ShadowJump(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        Location location = caster.getLocation();
        location.setPitch(-10);
        caster.setVelocity(location.getDirection().multiply(data.getDouble("dashMultiplier", 2.0)));
        triggerTraits(0);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        caster.getWorld().spawnParticle(Particle.WHITE_ASH, caster.getLocation(), 1);
        for (LivingEntity entity : caster.getLocation().getNearbyLivingEntities(2)) {
            if (entity == caster) {
                continue;
            }
            if (entity.getBoundingBox().overlaps(caster.getBoundingBox())) {
                caster.setInvisible(true);
                break;
            }
        }
    }

    @Override
    protected void onTickFinish() {
        caster.setInvisible(false);
    }

}

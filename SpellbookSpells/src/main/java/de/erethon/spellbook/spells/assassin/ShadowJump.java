package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class ShadowJump extends AssassinBaseSpell{

    public ShadowJump(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("invisDuration", 15);
    }

    @Override
    protected boolean onCast() {
        Location location = caster.getLocation();
        location.setPitch(-10);
        caster.setVelocity(location.getDirection().multiply(data.getDouble("dashMultiplier", 2.0)));
        caster.setInvisible(true);
        triggerTraits(0);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        caster.getWorld().spawnParticle(Particle.WHITE_ASH, caster.getLocation(), 1);
    }

    @Override
    protected void onTickFinish() {
        caster.setInvisible(false);
    }
}

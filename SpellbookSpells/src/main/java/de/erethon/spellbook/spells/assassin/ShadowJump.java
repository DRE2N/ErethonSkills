package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class ShadowJump extends AssassinBaseSpell {

    public ShadowJump(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
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

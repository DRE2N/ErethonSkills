package de.erethon.spellbook.spells.ranger.hawkeye;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class LethalFocus extends RangerBaseSpell {

    // The Ranger enters a focussed mode. Headshots deal massively increased damage, and bow projectiles are much faster.

    public LethalFocus(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        caster.getTags().add("spellbook.ranger.lethalfocus");
        return super.onCast();
    }

    @Override
    protected void onTick() {
        caster.getWorld().spawnParticle(Particle.CHERRY_LEAVES, caster.getLocation(), 3, 0.5, 0.5, 0.5);
        super.onTick();
    }

    @Override
    protected void onTickFinish() {
        caster.getTags().remove("spellbook.ranger.lethalfocus");
        super.onTickFinish();
    }
}

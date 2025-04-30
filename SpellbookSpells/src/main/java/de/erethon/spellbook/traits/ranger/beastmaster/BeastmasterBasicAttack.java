package de.erethon.spellbook.traits.ranger.beastmaster;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class BeastmasterBasicAttack extends SpellTrait {

    private int flowTick = 0;

    public BeastmasterBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTick() {
        flowTick++;
        if (flowTick >= 20) {
            flowTick = 0;
            if (caster.getTags().contains("spellbook.ranger.flow")) {
                caster.getLocation().getWorld().spawnParticle(Particle.TINTED_LEAVES, caster.getLocation(), 5, 0.5, 0.5, 0.5);
            }
        }
    }
}

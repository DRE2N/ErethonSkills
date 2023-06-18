package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class TremblingHit extends SpellTrait {

    private final int radius = data.getInt("radius", 3);
    private final int stunDuration = data.getInt("stunDuration", 20);
    private final EffectData stun = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Stun");

    public TremblingHit(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity targets : trigger.getTargets()) {
            for (LivingEntity living : targets.getLocation().getNearbyLivingEntities(radius)) {
                if (living == caster) continue;
                if (!Spellbook.canAttack(caster, living)) continue;
                living.addEffect(caster, stun, stunDuration, 1);
            }
        }
    }
}

package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.LivingEntity;

public class LargeBlade extends SpellTrait {

    private final double range = data.getDouble("range", 5);
    private final double velocity = data.getDouble("velocity", 1.5);

    public LargeBlade(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTarget().getLocation().getNearbyLivingEntities(range)) {
            if (living == caster) continue;
            if (!Spellbook.canAttack(caster, living)) continue;
            living.setVelocity(caster.getLocation().getDirection().multiply(velocity));
            living.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.RECORD, 1, 1));
        }
    }
}

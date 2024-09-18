package de.erethon.spellbook.fx.cues;

import de.slikey.effectlib.Effect;
import org.bukkit.entity.LivingEntity;

public record ParticleCue(Effect effect, boolean shouldFollowEntity) implements FXCue {

    @Override
    public void run(LivingEntity trigger) {
        if (shouldFollowEntity) {
            effect.setEntity(trigger);
        } else {
            effect.setLocation(trigger.getLocation());
        }
        effect.start();
    }

}

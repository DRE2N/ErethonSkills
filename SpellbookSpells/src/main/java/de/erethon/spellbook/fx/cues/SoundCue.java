package de.erethon.spellbook.fx.cues;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public record SoundCue(Sound sound, float volume, float pitch) implements FXCue {

    @Override
    public void run(LivingEntity trigger) {
        trigger.getWorld().playSound(trigger.getLocation(), sound, volume, pitch);
    }
}

package de.erethon.spellbook.fx.cues;

import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;

public record ExpandingCircleEffectCue(Color color, int duration, double scale) implements FXCue {

    @Override
    public void run(LivingEntity trigger) {

    }
}

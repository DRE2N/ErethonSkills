package de.erethon.spellbook.animation;

import de.slikey.effectlib.Effect;

import java.util.ArrayList;
import java.util.List;

public class AnimationStage {

    private List<Effect> effects = new ArrayList<>();

    public void play() {
        for (Effect effect : effects) {
            effect.start();
        }
    }
}

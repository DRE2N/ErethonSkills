package de.erethon.spellbook.effects;

import de.erethon.spellbook.caster.SpellCaster;

public class SpellEffect {

    SpellCaster target;
    int duration;

    public SpellEffect(SpellCaster target, int duration) {
        this.target = target;
        this.duration = duration;
    }

    public void tick() {
        duration--;
    }

}

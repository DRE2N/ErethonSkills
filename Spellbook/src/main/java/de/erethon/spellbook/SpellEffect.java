package de.erethon.spellbook;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;

public class SpellEffect {

    enum StackMode {
        INTENSIFY,
        PROLONG,
    }

    SpellCaster target;
    EffectData data;

    private int ticksLeft;
    private int stacks;

    private StackMode stackMode;

    public SpellEffect(EffectData data, SpellCaster target, int duration) {
        this.data = data;
        this.target = target;
        this.ticksLeft = duration;
    }

    public void tick() {
        ticksLeft--;
    }

    public void add(int duration, int stacks) {
        if (stackMode == StackMode.PROLONG) {
            this.ticksLeft += duration;
        } else if (stackMode == StackMode.INTENSIFY) {
            this.stacks += stacks;
        }
        onApply();
    }

    public boolean canAdd(int duration, int newStacks) {
        return ticksLeft + duration <= data.getMaxDuration() && stacks + newStacks <= data.getMaxStacks();
    }

    public void onApply() {
    }

    public void onRemove() {
    }

    public boolean shouldRemove() {
        return ticksLeft <= 0;
    }

    public SpellCaster getTarget() {
        return target;
    }

    public EffectData getData() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SpellEffect other) {
            return other.getData().equals(getData());
        }
        return false;
    }
}

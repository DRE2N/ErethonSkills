package de.erethon.spellbook;

import de.erethon.spellbook.caster.SpellCaster;

import java.util.UUID;

public abstract class SpellbookSpell {

    Spellbook spellbook = Spellbook.getInstance();
    protected final UUID uuid;

    protected final SpellData data;
    protected final SpellCaster caster;

    protected int keepAliveTicks = 0;
    private int currentTicks = 0;

    protected int tickInterval = 1;
    private int currentTickInterval = 0;

    public SpellbookSpell(SpellCaster caster, SpellData spellData) {
        this.data = spellData;
        this.caster = caster;
        uuid = UUID.randomUUID();
    }


    /**
     * This should be used to check for prerequisites for the spell, such as mana, target, location, etc.
     * @return true if the spell can be cast, false otherwise
     */
    protected abstract boolean onPrecast();

    /**
     * This should be used to implement the spell itself.
     * @return true if the spell was successfully cast, false otherwise
     */
    protected abstract boolean onCast();

    /**
     * This should be used do execute code after the spell was cast, like removing mana.

     */
    protected abstract void onAfterCast();

    protected abstract void onTick();

    protected abstract void onTickFinish();

    public void ready() {
        if (onPrecast()) {
            if (onCast()) {
                onAfterCast();
            }
        }
    }

    public void tick() {
        currentTicks++;
        if (currentTickInterval >= tickInterval) {
            currentTickInterval = 0;
            onTick();
        } else {
            currentTickInterval++;
        }
        if (shouldRemove()) {
            onTickFinish();
        }
    }


    public boolean shouldRemove() {
        if (keepAliveTicks < 0) {
            return false;
        }
        return currentTicks >= keepAliveTicks;
    }

    public SpellData getData() {
        return data;
    }

    public SpellCaster getCaster() {
        return caster;
    }

    public UUID getUuid() {
        return uuid;
    }
}

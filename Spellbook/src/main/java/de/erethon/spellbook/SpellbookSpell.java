package de.erethon.spellbook;

import de.erethon.spellbook.caster.SpellCaster;
import de.slikey.effectlib.EffectManager;

import java.util.UUID;

public abstract class SpellbookSpell {

    Spellbook spellbook = Spellbook.getInstance();
    protected final UUID uuid;

    protected final SpellData data;
    protected final SpellCaster caster;
    protected EffectManager effectManager;

    protected int keepAliveTicks = 0;
    private int currentTicks = 0;

    protected int tickInterval = 1;
    private int currentTickInterval = 0;

    private boolean failed = false;

    public SpellbookSpell(SpellCaster caster, SpellData spellData) {
        this.data = spellData;
        this.caster = caster;
        this.effectManager = spellbook.getEffectManager();
        uuid = UUID.randomUUID();
    }


    /**
     * This should be used to check for prerequisites for the spell, such as mana, target, location, etc.
     *
     * @return true if the spell can be cast, false otherwise
     */
    protected boolean onPrecast() {
        return true;
    }

    /**
     * This should be used to implement the spell itself.
     *
     * @return true if the spell was successfully cast, false otherwise
     */
    protected boolean onCast() {
        return true;
    }

    /**
     * This should be used do execute code after the spell was cast, like removing mana.
     */
    protected void onAfterCast() {
    }

    protected void onTick() {
    }

    protected void onTickFinish() {
    }

    public void ready() {
        if (onPrecast()) {
            if (onCast()) {
                onAfterCast();
            } else {
                failed = true;
            }
        } else {
            failed = true;
        }
    }

    public void tick() {
        if (failed) {
            return;
        }
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
        if (failed) {
            return true;
        }
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

    public String getId() {
        return data.getId();
    }
}

package de.erethon.spellbook.spells;

import de.erethon.spellbook.SpellError;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;

import java.util.UUID;

public class ActiveSpell {

    Spellbook spellbook = Spellbook.getInstance();
    private final UUID uuid;

    private final SpellData spellData;
    private final SpellCaster caster;

    private int keepAliveTicks = 0;
    private int currentTicks = 0;

    private SpellError error;

    public ActiveSpell(SpellCaster caster, SpellData spellData) {
        this.spellData = spellData;
        this.caster = caster;
        uuid = UUID.randomUUID();
    }

    public void ready() {
        if (getSpell().precast(caster, this)) {
            if (getSpell().cast(caster, this)) {
                getSpell().afterCast(caster, this);
            } else {
                sendError();
            }
        } else {
            sendError();
        }
    }

    public void tick() {
        currentTicks++;
        spellData.tick(caster, this);
    }

    public boolean shouldRemove() {
        return currentTicks >= keepAliveTicks;
    }

    public void setKeepAliveTicks(int keepAliveTicks) {
        this.keepAliveTicks = keepAliveTicks;
    }

    private void sendError() {
        if (error != null) {
            caster.sendMessage(error.getMessage());
        }
    }

    public SpellError getError() {
        return error;
    }

    public void setError(SpellError error) {
        this.error = error;
    }

    public UUID getUuid() {
        return uuid;
    }

    public SpellData getSpell() {
        return spellData;
    }

    public SpellCaster getCaster() {
        return caster;
    }
}

package de.erethon.spellbook;

import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class ActiveSpell {

    Spellbook spellbook = Spellbook.getInstance();
    private final UUID uuid;

    private final SpellData spellData;
    private final SpellCaster caster;

    private int keepAliveTicks = 0;
    private int currentTicks = 0;

    private int tickInterval = 1;
    private int currentTickInterval = 0;

    private Location location = null;
    private Entity targetEntity= null;

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
        if (currentTickInterval >= tickInterval) {
            currentTickInterval = 0;
            getSpell().tick(caster, this);
        } else {
            currentTickInterval++;
        }
    }

    public boolean shouldRemove() {
        return currentTicks >= keepAliveTicks;
    }

    /** Sets the amount of ticks this spell should be kept alive. Every tick, the tick() method is called.
     * if you want to run logic every x ticks, use the setTickInterval() method.
     * @param keepAliveTicks the keepAliveTicks to set. This is in SpellQueue ticks, not server ticks.
     */
    public void setKeepAliveTicks(int keepAliveTicks) {
        this.keepAliveTicks = keepAliveTicks;
    }

    private void sendError() {
        if (error != null) {
            caster.sendMessage(error.getMessage());
        }
    }

    /**
     * Sets a tick interval for this spell. This is in SpellQueue ticks. The default is 0.
     * The SpellQueue will always tick all ActiveSpells at the same interval, so if you want to have a spell that ticks differently from this,
     * this method can be used to achieve this.
     * @param tickInterval the interval between each tick
     */
    public void setTickInterval(int tickInterval) {
        this.tickInterval = tickInterval;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(Entity targetEntity) {
        this.targetEntity = targetEntity;
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

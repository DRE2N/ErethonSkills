package de.erethon.spellbook.events;

import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.ItemProjectile;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemProjectileHitEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final SpellbookSpell spell;
    private final ItemProjectile projectile;
    private final Arrow arrow;
    private final Entity hitEntity;

    public ItemProjectileHitEvent(SpellbookSpell spell, ItemProjectile projectile, Arrow arrow, Entity hitEntity) {
        this.spell = spell;
        this.projectile = projectile;
        this.arrow = arrow;
        this.hitEntity = hitEntity;
    }

    public SpellbookSpell getSpell() {
        return spell;
    }

    public ItemProjectile getProjectile() {
        return projectile;
    }

    public Arrow getArrow() {
        return arrow;
    }

    @Nullable
    public Entity getHitEntity() {
        return hitEntity;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

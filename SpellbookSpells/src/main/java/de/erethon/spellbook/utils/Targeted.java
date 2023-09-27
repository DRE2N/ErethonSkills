package de.erethon.spellbook.utils;

import org.bukkit.entity.LivingEntity;

public interface Targeted {

    LivingEntity getTarget();
    void setTarget(LivingEntity target);
}

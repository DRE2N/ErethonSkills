package de.erethon.spellbook.caster;

import de.erethon.spellbook.SpellData;
import org.bukkit.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public interface SpellCaster {

    Map<SpellData, Long> usedSpells = new LinkedHashMap<>();

    default void cast(SpellData spellData) {
        if (!canCast(spellData)) {
            spellData.queue(this);
        }
        spellData.queue(this);
    }

    void sendMessage(String message);

    LivingEntity getEntity();

    default int calculateCooldown(SpellData spellData) {
        long timestamp = getCooldown(spellData);
        long now = System.currentTimeMillis();
        int cooldown = (int) (now - timestamp) / 1000;
        return Math.max(spellData.getCooldown() - cooldown, 1); // 1 because amount = 0 removes the item.
    }

    default boolean canCast(SpellData spellData) {
        long time = usedSpells.getOrDefault(spellData, 0L);
        double skillCD = spellData.getCooldown();
        long now = System.currentTimeMillis();
        return now - time > (skillCD * 1000);
    }

    default long getCooldown(SpellData spellData) {
        return usedSpells.getOrDefault(spellData, 0L);
    }

    default void setCooldown(SpellData skill) {
        usedSpells.put(skill, System.currentTimeMillis());
    }

}

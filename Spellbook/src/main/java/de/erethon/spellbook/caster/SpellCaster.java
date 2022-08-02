package de.erethon.spellbook.caster;

import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.effects.SpellEffect;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface SpellCaster {

    Map<SpellData, Long> usedSpells = new LinkedHashMap<>();
    Set<SpellEffect> effects = new HashSet<>();
    Set<SpellbookSpell> passiveSpells = new HashSet<>();

    default void cast(SpellData spellData) {
        if (!canCast(spellData)) {
            spellData.queue(this);
        }
        spellData.queue(this);
    }

    // Messaging

    void sendMessage(String message);

    void sendActionbar(String message);

    default void tick() {
        for (SpellEffect effect : effects) {
            effect.tick();
        }
        for (SpellbookSpell spell : passiveSpells) {
            spell.tick();
        }
    }

    default void addPassiveSpell(SpellbookSpell spell) {
        passiveSpells.add(spell);
        spell.ready();
    }

    default void removePassiveSpell(SpellbookSpell spell) {
        passiveSpells.remove(spell);
    }

    default void addEffect(SpellEffect effect) {
        effects.add(effect);
    }

    default void removeEffect(SpellEffect effect) {
        effects.remove(effect);
    }

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

    int getEnergy();
    int setEnergy(int energy);
    int addEnergy(int energy);
    int removeEnergy(int energy);

}

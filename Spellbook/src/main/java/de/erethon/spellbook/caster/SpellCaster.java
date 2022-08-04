package de.erethon.spellbook.caster;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.effects.SpellEffect;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.Set;

public interface SpellCaster {
    default void cast(SpellData spellData) {
        if (!canCast(spellData)) {
            spellData.queue(this);
        }
        spellData.queue(this);
    }

    // Messaging

    void sendMessage(String message);

    void sendActionbar(String message);

    Team getTeam();

    void setTeam(Team team);

    default boolean sameTeam(SpellCaster other) {
        return getTeam().equals(other.getTeam());
    }

    Location getLocation();

    Map<SpellData, Long> getUsedSpells();

    Set<SpellEffect> getEffects();

    Set<SpellbookSpell> getPassiveSpells();


    default void tick() {
        for (SpellEffect effect : getEffects()) {
            effect.tick();
        }
        for (SpellbookSpell spell : getPassiveSpells()) {
            spell.tick();
        }
    }

    default void addPassiveSpell(SpellbookSpell spell) {
        System.out.println("Added passive spell " + spell.getId());
        getPassiveSpells().add(spell);
        spell.ready();
    }

    default void removePassiveSpell(SpellbookSpell spell) {
        getPassiveSpells().remove(spell);
    }

    default void addEffect(SpellEffect effect) {
        getEffects().add(effect);
    }

    default void removeEffect(SpellEffect effect) {
        getEffects().remove(effect);
    }

    LivingEntity getEntity();

    default int calculateCooldown(SpellData spellData) {
        long timestamp = getCooldown(spellData);
        long now = System.currentTimeMillis();
        int cooldown = (int) (now - timestamp) / 1000;
        return Math.max(spellData.getCooldown() - cooldown, 1); // 1 because amount = 0 removes the item.
    }

    default boolean canCast(SpellData spellData) {
        long time = getUsedSpells().getOrDefault(spellData, 0L);
        double skillCD = spellData.getCooldown();
        long now = System.currentTimeMillis();
        return now - time > (skillCD * 1000);
    }

    default long getCooldown(SpellData spellData) {
        return getUsedSpells().getOrDefault(spellData, 0L);
    }

    default void setCooldown(SpellData skill) {
        getUsedSpells().put(skill, System.currentTimeMillis());
    }

    int getEnergy();
    int setEnergy(int energy);
    int getMaxEnergy();
    int setMaxEnergy(int maxEnergy);
    int addEnergy(int energy);
    int removeEnergy(int energy);

}

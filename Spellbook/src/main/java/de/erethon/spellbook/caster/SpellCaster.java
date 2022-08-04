package de.erethon.spellbook.caster;

import de.erethon.spellbook.EffectData;
import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.SpellEffect;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Team;

import java.util.Iterator;
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
        Iterator<SpellEffect> effectIterator = getEffects().iterator();
        while(effectIterator.hasNext()) {
            SpellEffect effect = effectIterator.next();
            if (effect.shouldRemove()) {
                effect.onRemove();
                effectIterator.remove();
            } else {
                effect.tick();
            }
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

    default void addEffect(EffectData data, int duration, int stacks) {
        SpellEffect effect = data.getActiveEffect(this, duration, stacks);
        if (getEffects().contains(effect)) {
            SpellEffect oldEffect = getEffects().stream().filter(e -> e.getData().equals(effect.getData())).findFirst().get();
            if (oldEffect.canAdd(duration, stacks)) {
                oldEffect.add(duration, stacks);
            }
        } else {
            getEffects().add(effect);
        }
    }

    default void removeEffect(EffectData effect) {
        getEffects().removeIf(e -> e.getData().equals(effect));
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

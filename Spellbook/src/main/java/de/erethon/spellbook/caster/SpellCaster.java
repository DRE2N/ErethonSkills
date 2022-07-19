package de.erethon.spellbook.caster;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.spells.Spell;
import org.bukkit.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public class SpellCaster {

    Spellbook spellbook;
    LivingEntity entity;
    Map<Spell, Double> usedSpells = new LinkedHashMap<>();

    public SpellCaster(Spellbook spellbook, LivingEntity entity) {
        this.spellbook = spellbook;
        this.entity = entity;
    }

    public void cast(Spell spell) {
        if (!canCast(spell)) {
            spell.queue(this);
        }
        spell.queue(this);
    }

    public boolean canCast(Spell spell) {
        double time = usedSpells.get(spell);
        double skillCD = spell.getCooldown();
        double now = System.currentTimeMillis();
        return now - time > (skillCD * 1000);
    }

    public void setCooldown(Spell skill) {
        double now = System.currentTimeMillis();
        usedSpells.put(skill, now);
    }

    public LivingEntity getEntity() {
        return entity;
    }
}

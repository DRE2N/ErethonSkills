package de.erethon.spellbook.caster;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.spells.SpellData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class SpellCaster {

    Spellbook spellbook;
    LivingEntity entity;
    Map<SpellData, Double> usedSpells = new LinkedHashMap<>();

    public SpellCaster(Spellbook spellbook, LivingEntity entity) {
        this.spellbook = spellbook;
        this.entity = entity;
    }

    public void cast(SpellData spellData) {
        if (!canCast(spellData)) {
            spellData.queue(this);
        }
        spellData.queue(this);
    }

    public void sendMessage(String message) {
        if (entity instanceof Player) {
            entity.sendMessage(message); // TODO: Aergia actionbar
        }
    }

    public boolean canCast(SpellData spellData) {
        double time = usedSpells.get(spellData);
        double skillCD = spellData.getCooldown();
        double now = System.currentTimeMillis();
        return now - time > (skillCD * 1000);
    }

    public void setCooldown(SpellData skill) {
        double now = System.currentTimeMillis();
        usedSpells.put(skill, now);
    }

    public LivingEntity getEntity() {
        return entity;
    }
}

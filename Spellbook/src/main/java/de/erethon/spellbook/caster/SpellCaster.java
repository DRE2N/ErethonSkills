package de.erethon.spellbook.caster;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.spells.SpellData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SpellCaster {

    Spellbook spellbook;
    LivingEntity entity;
    Map<SpellData, Long> usedSpells = new LinkedHashMap<>();


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

    public int calculateCooldown(SpellData spellData) {
        long timestamp = getCooldown(spellData);
        long now = System.currentTimeMillis();
        int cooldown = (int) (now - timestamp) / 1000;
        return Math.max(spellData.getCooldown() - cooldown, 1); // 1 because amount = 0 removes the item.
    }

    public boolean canCast(SpellData spellData) {
        long time = usedSpells.getOrDefault(spellData, 0L);
        double skillCD = spellData.getCooldown();
        long now = System.currentTimeMillis();
        return now - time > (skillCD * 1000);
    }

    public long getCooldown(SpellData spellData) {
        return usedSpells.getOrDefault(spellData, 0L);
    }

    public void setCooldown(SpellData skill) {
        usedSpells.put(skill, System.currentTimeMillis());
    }

    public LivingEntity getEntity() {
        return entity;
    }
}

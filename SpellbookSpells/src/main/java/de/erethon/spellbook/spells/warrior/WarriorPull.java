package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class WarriorPull extends SpellbookSpell {

    Entity target = null;

    public WarriorPull(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        target = caster.getTargetEntity(data.getInt("range", 10));
        if (target == null) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        target.setVelocity(caster.getLocation().getDirection().multiply(-1).multiply(data.getDouble("strength", 1)));
        return true;
    }
}

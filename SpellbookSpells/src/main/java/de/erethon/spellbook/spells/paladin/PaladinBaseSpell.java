package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class PaladinBaseSpell extends SpellbookSpell {

    LivingEntity target;

    public PaladinBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    protected boolean lookForTarget() {
        Entity target = caster.getTargetEntity(32);
        if (target == null) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        if (!(target instanceof LivingEntity)) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        this.target = (LivingEntity) target;
        return true;
    }
}

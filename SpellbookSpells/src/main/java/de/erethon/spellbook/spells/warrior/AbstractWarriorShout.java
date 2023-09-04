package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public abstract class AbstractWarriorShout extends WarriorBaseSpell {

    public int range = data.getInt("range", 10);

    public AbstractWarriorShout(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }
}

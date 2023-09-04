package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class AbstractWarriorStance extends WarriorBaseSpell {

    public int duration = data.getInt("duration", 120);
    public int attributeBonus = data.getInt("attributeBonus", 20);

    public AbstractWarriorStance(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }
}

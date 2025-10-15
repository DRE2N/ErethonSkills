package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import org.bukkit.entity.LivingEntity;

public class HierophantBaseSpell extends PaladinBaseSpell {

    public HierophantBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }
}

package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import org.bukkit.entity.LivingEntity;

public class GuardianBaseSpell extends PaladinBaseSpell {

    public GuardianBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }
}

package de.erethon.spellbook.spells.paladin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class PaladinSpearSpell extends PaladinBaseSpell {

    public PDamageType damageType = PDamageType.PHYSICAL;
    public Attribute damageAttribute = Attribute.ADVANTAGE_PHYSICAL;

    public PaladinSpearSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }
}

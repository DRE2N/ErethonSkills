package de.erethon.spellbook.spells.paladin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class PaladinSpearSpell extends PaladinBaseSpell {

    public DamageType damageType = DamageType.PHYSICAL;
    public Attribute damageAttribute = Attribute.ADV_PHYSICAL;

    public PaladinSpearSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }
}

package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.paladin.PaladinSpearSpell;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class BlessedWeapons extends SpellTrait {
    public BlessedWeapons(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof PaladinSpearSpell spear) {
            spear.damageType = PDamageType.MAGIC;
            spear.damageAttribute = Attribute.ADV_MAGIC;
        }
        return cast;
    }
}

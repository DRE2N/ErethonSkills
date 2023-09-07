package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import org.bukkit.entity.LivingEntity;

public class AssassinBaseTrap extends AoEBaseSpell {

    public double damageMultiplier = 1.0;

    public AssassinBaseTrap(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }
}

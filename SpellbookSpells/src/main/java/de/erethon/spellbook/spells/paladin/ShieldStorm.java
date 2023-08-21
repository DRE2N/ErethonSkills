package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;

public class ShieldStorm extends PaladinBaseSpell {
    public ShieldStorm(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(false);
    }

    @Override
    protected boolean onCast() {
        return super.onCast();
    }
}

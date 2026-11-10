package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import org.bukkit.entity.LivingEntity;

public class VanguardBaseSpell extends WarriorBaseSpell {
    public VanguardBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        boolean cast = super.onCast();
        if (cast) {
            caster.addEnergy(data.getInt("energyBuildUpFromUse", 0));
        }
        return cast;
    }
}

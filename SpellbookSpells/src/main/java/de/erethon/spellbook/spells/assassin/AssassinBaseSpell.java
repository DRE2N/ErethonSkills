package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;

public abstract class AssassinBaseSpell extends SpellbookSpell {

    protected final int energyCost;

    public AssassinBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        energyCost = spellData.getInt("energyCost", 10);
    }

    @Override
    protected boolean onPrecast() {
        boolean canCast = energyCost <= caster.getEnergy();
        if (!canCast) {
            caster.sendActionbar("<red>Nicht genug Energie!");
        }
        return canCast;
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(energyCost);
    }

}


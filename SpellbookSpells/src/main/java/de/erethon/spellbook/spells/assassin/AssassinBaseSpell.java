package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import de.erethon.spellbook.utils.Targeted;
import org.bukkit.entity.LivingEntity;

public class AssassinBaseSpell extends SpellbookBaseSpell implements Targeted {

    public int duration = data.getInt("duration", 10);
    public int energyCost = data.getInt("energyCost", 0);

    public AssassinBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        caster.setEnergy(caster.getEnergy() - energyCost);
        return super.onCast();
    }

    public boolean hasEnergy(LivingEntity caster, SpellData data) {
        boolean canCast = energyCost <= caster.getEnergy();
        if (Spellbook.getInstance().isDebug()) {
            return true;
        }
        if (!canCast) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_ENERGY);
        }
        return canCast;
    }

    @Override
    public LivingEntity getTarget() {
        return target;
    }

    @Override
    public void setTarget(LivingEntity target) {
        this.target = target;
    }

}

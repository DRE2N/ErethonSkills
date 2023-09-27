package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.SpellbookBaseSpell;
import de.erethon.spellbook.utils.Targeted;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class AssassinBaseSpell extends SpellbookBaseSpell implements Targeted {

    public int energyCost = data.getInt("energyCost", 0);
    public int range = data.getInt("range", 32);
    public LivingEntity target;

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
        if (!canCast) {
            caster.sendParsedActionBar("<color:#ff0000>Nicht genug Energie!");
        }
        return canCast;
    }

    protected boolean lookForTarget() {
        return lookForTarget(false, range);
    }

    protected boolean lookForTarget(int range) {
        return lookForTarget(false, range);
    }

    protected boolean lookForTarget(boolean friendly) {
        return lookForTarget(friendly, range);
    }

    protected boolean lookForTarget(boolean friendly, int range) {
        Entity target = caster.getTargetEntity(range);
        if (target == null) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        if (!(target instanceof LivingEntity)) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        if (friendly && Spellbook.canAttack(caster, (LivingEntity) target)) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        this.target = (LivingEntity) target;
        return true;
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

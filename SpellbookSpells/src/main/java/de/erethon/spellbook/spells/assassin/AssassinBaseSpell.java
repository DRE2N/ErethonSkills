package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import de.erethon.spellbook.utils.Targeted;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class AssassinBaseSpell extends SpellbookBaseSpell implements Targeted {

    public int duration = data.getInt("duration", 10);
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
        if (Spellbook.getInstance().isDebug()) {
            return true;
        }
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
        Entity target = caster.getTargetEntity(range, true);
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
        if (!caster.hasLineOfSight(target)) {
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

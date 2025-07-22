package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import de.erethon.spellbook.utils.Targeted;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class WarriorBaseSpell extends SpellbookBaseSpell implements Targeted {

    protected final int duration = data.getInt("duration", 0);
    public int rageCost = data.getInt("rageCost", 0);

    public LivingEntity target;

    public WarriorBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    protected boolean onPrecast() {
        return hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        caster.setEnergy(caster.getEnergy() - rageCost);
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        return super.onCast();
    }

    public boolean hasEnergy(LivingEntity caster, SpellData data) {
        boolean canCast = rageCost <= caster.getEnergy();
        if (Spellbook.getInstance().isDebug()) {
            return true;
        }
        if (!canCast) {
            caster.sendParsedActionBar("<color:#ff0000>Nicht genug Rage!");
        }
        return canCast;
    }

    protected boolean lookForTarget() {
        return lookForTarget(false, data.getInt("range", 32));
    }

    protected boolean lookForTarget(int range) {
        return lookForTarget(false, range);
    }

    protected boolean lookForTarget(boolean friendly) {
        return lookForTarget(friendly, 32);
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

package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.SpellbookBaseSpell;
import de.erethon.spellbook.utils.Targeted;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class RangerBaseSpell extends SpellbookBaseSpell implements Targeted {

    protected final int manaCost;
    public LivingEntity target;

    public RangerBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        manaCost = spellData.getInt("manaCost", 0);
    }

    @Override
    protected boolean onPrecast() {
        boolean canCast = manaCost <= caster.getEnergy();
        if (!canCast) {
            caster.sendParsedActionBar("<color:#ff0000>Nicht genug Mana!");
        }
        if (Spellbook.getInstance().isDebug()) {
            return true;
        }
        return canCast;
    }

    @Override
    public boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        return super.onCast();
    }

    protected boolean lookForTarget() {
        return lookForTarget(false);
    }

    protected boolean lookForTarget(boolean enemyOnly) {
        Entity target = caster.getTargetEntity(32);
        if (target == null) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        if (!(target instanceof LivingEntity)) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        if (!Spellbook.canAttack(caster, (LivingEntity) target) && enemyOnly) {
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

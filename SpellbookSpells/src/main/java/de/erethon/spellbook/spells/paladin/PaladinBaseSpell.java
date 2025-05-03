package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class PaladinBaseSpell extends SpellbookBaseSpell {

    protected final int duration = data.getInt("duration", 10);
    protected final int energyCost = data.getInt("energyCost", 0);

    protected LivingEntity target;

    public PaladinBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        return super.onCast();
    }

    protected boolean lookForTarget() {
        return lookForTarget(false, 32);
    }

    protected boolean lookForTarget(int range) {
        return lookForTarget(false, range);
    }

    protected boolean lookForTarget(boolean friendly) {
        return lookForTarget(friendly, 32);
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

    public boolean hasEnergy(LivingEntity caster, SpellData data) {
        boolean canCast = energyCost <= caster.getEnergy();
        if (!canCast) {
            caster.sendParsedActionBar("<color:#ff0000>Nicht genug Energie!");
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

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        placeholderNames.add("duration");
        spellAddedPlaceholders.add(Component.text(data.getInt("range", 0), VALUE_COLOR));
        placeholderNames.add("range");
        return super.getPlaceholders(c);
    }
}

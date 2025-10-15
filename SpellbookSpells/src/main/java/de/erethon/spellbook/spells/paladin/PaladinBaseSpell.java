package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class PaladinBaseSpell extends SpellbookBaseSpell {

    protected final int duration = data.getInt("duration", 10);
    protected final int energyCost = data.getInt("energyCost", 0);
    protected final int cooldown = data.getInt("cooldown", 5) * 1000;

    public PaladinBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        if (caster.getUsedSpells().containsKey(data)) {
            long lastCast = caster.getUsedSpells().get(data);
            if (System.currentTimeMillis() - lastCast < cooldown) {
                caster.sendParsedActionBar(SpellbookCommonMessages.ON_COOLDOWN);
                return false;
            }
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        caster.setEnergy(caster.getEnergy() - energyCost);
        return super.onCast();
    }

    protected boolean lookForTarget() {
        return super.lookForTarget(32);
    }

    protected boolean lookForTarget(int range) {
        return super.lookForTarget(range);
    }

    protected boolean lookForTarget(boolean friendly) {
        return lookForTarget(friendly, 32);
    }

    protected boolean lookForTarget(boolean friendly, int range) {
        Entity target = caster.getTargetEntity(range, true);
        if (target == null) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        if (!(target instanceof LivingEntity)) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        if (friendly && Spellbook.canAttack(caster, (LivingEntity) target)) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NOT_FRIENDLY);
            return false;
        }
        if (!caster.hasLineOfSight(target)) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        this.target = (LivingEntity) target;
        return true;
    }

    public boolean hasEnergy(LivingEntity caster, SpellData data) {
        if (Spellbook.getInstance().isDebug()) {
            return true;
        }
        boolean canCast = energyCost <= caster.getEnergy();
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

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        placeholderNames.add("duration");
        spellAddedPlaceholders.add(Component.text(data.getInt("range", 0), VALUE_COLOR));
        placeholderNames.add("range");
        return super.getPlaceholders(c);
    }
}

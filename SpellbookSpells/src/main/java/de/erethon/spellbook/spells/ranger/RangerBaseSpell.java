package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import de.erethon.spellbook.utils.Targeted;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class RangerBaseSpell extends SpellbookBaseSpell implements Targeted {

    protected final int duration = data.getInt("duration", 10);
    protected final int manaCost = data.getInt("manaCost", 10);
    public LivingEntity target;

    public RangerBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
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

    protected void addFlow() {
        caster.getTags().add("spellbook.ranger.flow");
    }

    protected void removeFlow() {
        if (caster.getTags().contains("spellbook.ranger.lethalfocus")) {
            return;
        }
        caster.getTags().remove("spellbook.ranger.flow");
    }

    protected boolean inFlowState() {
        return caster.getTags().contains("spellbook.ranger.flow");
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        spellAddedPlaceholders.add(Component.text(manaCost, VALUE_COLOR));
        return super.getPlaceholders(c);
    }
}

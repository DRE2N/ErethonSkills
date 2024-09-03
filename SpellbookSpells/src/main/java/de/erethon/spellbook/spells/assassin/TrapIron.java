package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrapIron extends AssassinBaseTrap {

    private final int duration = data.getInt("duration", 10);

    public boolean triggeredFirstTime = false;


    public TrapIron(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        keepAliveTicks = duration;
        tickInterval = 20;
        return super.onCast();
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }

    @Override
    public void onTick() {
        super.onTick();
        Set<LivingEntity> targets = new HashSet<>();
        for (LivingEntity entity : getEntities()) {
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }
            targets.add(entity);
            entity.damage(Spellbook.getScaledValue(data, caster, entity, Attribute.ADV_PHYSICAL, damageMultiplier), caster, PDamageType.PHYSICAL);
            triggerTraits(entity, 1);
        }
        if (!targets.isEmpty() && !triggeredFirstTime) {
            triggeredFirstTime = true;
            triggerTraits(targets);
            triggerTraits(2);
        }
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        spellAddedPlaceholders.add(Component.text(Spellbook.getScaledValue(data, caster, caster, Attribute.ADV_PHYSICAL, damageMultiplier), ATTR_PHYSICAL_COLOR));
        return super.getPlaceholders(caster);
    }
}

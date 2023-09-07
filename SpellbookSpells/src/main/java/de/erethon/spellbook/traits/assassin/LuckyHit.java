package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class LuckyHit extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("LuckyHit", data.getDouble("bonusCritChance", 15), AttributeModifier.Operation.ADD_NUMBER);

    public LuckyHit(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.STAT_CRIT_CHANCE).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.STAT_CRIT_CHANCE).removeModifier(modifier);
    }
}

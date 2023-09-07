package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class Fighter extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("Fighter", data.getDouble("damageMultiplier", 1.2), AttributeModifier.Operation.ADD_SCALAR);

    public Fighter(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public void onAdd() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(modifier);
    }
}

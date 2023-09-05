package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class IronSkin extends SpellTrait {

    private final AttributeModifier bonus = new AttributeModifier("IronSkin", 2, AttributeModifier.Operation.ADD_NUMBER);

    public IronSkin(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).addTransientModifier(bonus);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(bonus);
    }
}

package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class Criminal extends SpellTrait {

    private final AttributeModifier weaponDamageBonus = new AttributeModifier("Criminal", data.getDouble("weaponDamageBonus"), AttributeModifier.Operation.ADD_NUMBER);

    public Criminal(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addTransientModifier(weaponDamageBonus);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).removeModifier(weaponDamageBonus);
    }
}

package de.erethon.spellbook.traits.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class SurvivalExpert extends SpellTrait {

    private final double bonusHealth = data.getDouble("bonusHealth", 0.2);
    private AttributeModifier modifier;

    public SurvivalExpert(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        if (modifier != null) {
            caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).removeModifier(modifier);
        }
        modifier = new AttributeModifier("survival_expert", caster.getMaxHealth() * bonusHealth, AttributeModifier.Operation.ADD_NUMBER);
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).removeModifier(modifier);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        onAdd(); // We need to re-calculate the modifier somewhere, so let's do it here
        return super.onDamage(attacker, damage, type);
    }
}
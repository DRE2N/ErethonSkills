package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class ShieldOfFaith extends PaladinBaseSpell {

    // RMB: The Guardian raises his shield, protecting himself from attacks from the front.
    // For blocked attacks, the Paladin and his allies are healed.
    // Note: Logic for this is mostly in the trait to allow for toggling the shield.
    // TODO: Better visual effects for the shield.

    private final AttributeModifier speedModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:paladin_shield"), data.getDouble("speedModifier", -0.2), AttributeModifier.Operation.ADD_NUMBER);

    public ShieldOfFaith(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data); // 0
    }

    @Override
    public boolean onCast() {
        if (isShieldUp()) {
            lowerShield();
        } else {
            raiseShield();
        }
        return super.onCast();
    }

    private boolean isShieldUp() {
        return caster.getTags().contains("paladin.shield");
    }

    private void raiseShield() {
        caster.getTags().add("paladin.shield");
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(speedModifier);
    }

    private void lowerShield() {
        caster.getTags().remove("paladin.shield");
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(speedModifier);
    }
}

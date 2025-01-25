package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.List;

public class BeastMode extends RangerPetBaseSpell {

    private final NamespacedKey key = new NamespacedKey("spellbook", "beastmode");
    private final double attributeMultiplier = data.getDouble("attributeMultiplier", 0.2);
    private final AttributeModifier modifier = new AttributeModifier(key, attributeMultiplier, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY);

    public BeastMode(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        caster.teleport(pet.getLocation());
        caster.getWorld().playSound(caster.getLocation(), org.bukkit.Sound.ENTITY_WOLF_HOWL, 1, 1);
        pet.remove();
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.STAT_HEALTH_REGEN).addTransientModifier(modifier);
        caster.getAttribute(Attribute.MAX_HEALTH).addTransientModifier(modifier);
        return true;
    }

    @Override
    protected void onTickFinish() {
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).removeModifier(modifier);
        caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).removeModifier(modifier);
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).removeModifier(modifier);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(modifier);
        caster.getAttribute(Attribute.STAT_HEALTH_REGEN).removeModifier(modifier);
        caster.getAttribute(Attribute.MAX_HEALTH).removeModifier(modifier);
        petTrait.spawn();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(attributeMultiplier * 100, ATTR_PHYSICAL_COLOR));
        placeholderNames.add("attribute multiplier");
        return super.getPlaceholders(c);
    }
}

package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import de.erethon.spellbook.utils.NMSUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ThrowBola extends AssassinBaseSpell {

    private final int duration = data.getInt("duration", 3);

    private final NamespacedKey key = new NamespacedKey("spellbook", "throwbola");
    private AttributeModifier modifier;
    private ArmorStand armorStand;

    public ThrowBola(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        modifier = new AttributeModifier(key, -100.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
        triggerTraits(target);
        armorStand = target.getWorld().spawn(target.getLocation(), ArmorStand.class);
        armorStand.setInvisible(true);
        armorStand.setMarker(true);
        armorStand.getEquipment().setHelmet(new ItemStack(Material.BROWN_CARPET)); // TODO: Model
        return true;
    }

    @Override
    protected void cleanup() {
        target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        armorStand.remove();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        return super.getPlaceholders(c);
    }
}

package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class LastBastion extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitlastbastion");
    private final double range = data.getDouble("range", 5);
    private final double resistancePercentagePerEnemy = data.getDouble("resistancePercentagePerEnemy", 0.05);
    private AttributeModifier modifier;

    public LastBastion(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTick() {
        if (modifier != null) {
            caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(modifier);
            caster.getAttribute(Attribute.RES_MAGIC).removeModifier(modifier);
        }
        int enemies = 0;
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range))  {
            if (living == caster) continue;
            if (!Spellbook.canAttack(caster, living)) continue;
            enemies++;
        }
        double resistance = (caster.getAttribute(Attribute.RES_PHYSICAL).getValue() * resistancePercentagePerEnemy) * enemies;
        modifier = new AttributeModifier(key, resistance, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.RES_MAGIC).addTransientModifier(modifier);
    }
}

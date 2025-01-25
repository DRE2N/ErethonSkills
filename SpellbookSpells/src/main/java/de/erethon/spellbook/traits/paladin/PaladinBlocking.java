package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.traits.ClassMechanic;
import net.minecraft.world.entity.player.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class PaladinBlocking extends ClassMechanic {

    private final NamespacedKey key = new NamespacedKey("spellbook", "paladinblocking");
    private final Player nmsPlayer;
    private double energyPerDamage = data.getDouble("energyPerDamage", 0.1);
    private final double damageMultiplier = data.getDouble("damageMultiplier", 0.33);
    private final AttributeModifier energyRegen = new AttributeModifier(key, data.getDouble("energyRegenRate", 0.1), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
    private final TraitData endurantTrait = Spellbook.getTraitData("EndurantShield");
    private final double endurantEnergyPerDamage = endurantTrait.getDouble("energyPerDamage", 0.1);

    public PaladinBlocking(TraitData data, LivingEntity caster) {
        super(data, caster);
        CraftPlayer cp = (CraftPlayer) caster;
        nmsPlayer = cp.getHandle();
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (caster.hasTrait(endurantTrait)) { // Can't do this in onAdd because there is no load order
            energyPerDamage = endurantEnergyPerDamage;
        }
        if (nmsPlayer.isBlocking() && caster.getEnergy() > 0) {// The Bukkit method is delayed by 5 ticks for whatever reason
            caster.setEnergy((int) Math.round(caster.getEnergy() - damage * energyPerDamage));
            triggerTraits();
            return damage * damageMultiplier;
        }
        return super.onDamage(attacker, damage, type);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.STAT_ENERGY_REGEN).addTransientModifier(energyRegen);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.STAT_ENERGY_REGEN).removeModifier(energyRegen);
    }
}

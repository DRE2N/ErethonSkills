package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.traits.ClassMechanic;
import net.minecraft.world.entity.player.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;

public class PaladinBlocking extends ClassMechanic {

    private final Player nmsPlayer;
    private final double energyPerDamage = data.getDouble("energyPerDamage", 0.1);
    private final double damageMultiplier = data.getDouble("damageMultiplier", 0.33);
    private final AttributeModifier energyRegen = new AttributeModifier("PaladinBlocking", data.getDouble("energyRegenRate", 0.1), AttributeModifier.Operation.ADD_NUMBER);

    public PaladinBlocking(TraitData data, LivingEntity caster) {
        super(data, caster);
        CraftPlayer cp = (CraftPlayer) caster;
        nmsPlayer = cp.getHandle();
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        if (nmsPlayer.isBlocking() && caster.getEnergy() > 0) {// The Bukkit method is delayed by 5 ticks for whatever reason
            caster.setEnergy((int) Math.round(caster.getEnergy() - damage * energyPerDamage));
            triggerTraits();
            return damage * damageMultiplier;
        }
        return super.onDamage(attacker, damage, type);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.STAT_ENERGYREGEN).addTransientModifier(energyRegen);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.STAT_ENERGYREGEN).removeModifier(energyRegen);
    }
}

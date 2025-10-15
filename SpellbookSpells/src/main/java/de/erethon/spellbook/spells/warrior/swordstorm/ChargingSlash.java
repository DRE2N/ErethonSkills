package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ChargingSlash extends SwordstormBaseSpell {

    // Perform a powerful forward slash. If Sprinting when activated: Deals significantly increased damage and applies increased Knockback to the target.
    // If not Sprinting: Deals moderate damage with normal knockback.

    private final int range = data.getInt("range", 5);
    private final double knockbackMultiplier = data.getDouble("knockbackSprintingMultiplier", 1.3);
    private final double knockbackMultiplierNormal = data.getDouble("knockbackNormalMultiplier", 1.1);

    public ChargingSlash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range) && !caster.getTags().contains("swordstorm.bladedance");
    }

    @Override
    public boolean onCast() {
        Player player = (Player) caster;
        if (player.isSprinting()) {
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL, "sprintingDamage");
            target.damage(damage, caster, PDamageType.PHYSICAL);
            Vector knockback = caster.getLocation().getDirection().multiply(knockbackMultiplier);
            target.setVelocity(target.getVelocity().add(knockback));
        } else {
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL, "normalDamage");
            target.damage(damage, caster, PDamageType.PHYSICAL);
            Vector knockback = caster.getLocation().getDirection().multiply(knockbackMultiplierNormal);
            target.setVelocity(target.getVelocity().add(knockback));
        }
        player.swingMainHand();
        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
    }
}

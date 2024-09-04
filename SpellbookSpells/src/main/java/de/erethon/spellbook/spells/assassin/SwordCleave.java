package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class SwordCleave extends AssassinBaseSpell {

    private final double radius = data.getDouble("radius", 1.5);
    public double damageMultiplier = data.getDouble("damageMultiplier", 0.6);

    public SwordCleave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        caster.attack(target);
        double attackDmg = Spellbook.getScaledValue(data, caster, target, Attribute.ADV_PHYSICAL);
        for (LivingEntity entity : target.getLocation().getNearbyLivingEntities(radius)) {
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }
            entity.damage(Spellbook.getVariedDamage(attackDmg, caster, true) * damageMultiplier, caster, PDamageType.PHYSICAL);
            triggerTraits(target);
        }
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(radius, VALUE_COLOR));
        placeholderNames.add("radius");
        spellAddedPlaceholders.add(Component.text(Spellbook.getScaledValue(data, caster, caster, Attribute.ADV_PHYSICAL), ATTR_PHYSICAL_COLOR));
        placeholderNames.add("damage");
        return super.getPlaceholders(c);
    }
}

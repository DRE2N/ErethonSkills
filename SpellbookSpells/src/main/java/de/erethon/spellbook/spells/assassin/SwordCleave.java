package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class SwordCleave extends AssassinBaseSpell {

    private final double radius = data.getDouble("radius", 1.5);

    public SwordCleave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    protected boolean onCast() {
        caster.attack(target);
        double attackDmg = Spellbook.getScaledValue(data, caster, target, Attribute.ADV_PHYSICAL);
        for (LivingEntity entity : target.getLocation().getNearbyLivingEntities(radius)) {
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }
            entity.damage(Spellbook.getVariedDamage(attackDmg, caster, true), caster, DamageType.PHYSICAL);
        }
        return true;
    }

}

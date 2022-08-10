package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class SwordCleave extends EntityTargetSpell {

    private final double radius = data.getDouble("radius", 1.5);

    public SwordCleave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return AssassinUtils.hasEnergy(caster, data) && super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        caster.attack(targetEntity);
        double attackDmg = caster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue() + caster.getAttribute(Attribute.ADV_PHYSICAL).getValue();
        for (LivingEntity entity : targetEntity.getLocation().getNearbyLivingEntities(radius)) {
            entity.damage(attackDmg * data.getDouble("damageMultiplier", 0.8), caster, DamageType.PHYSICAL);
        }
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 10));
    }
}

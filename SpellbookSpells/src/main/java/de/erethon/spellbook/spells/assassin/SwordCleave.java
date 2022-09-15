package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
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
        double attackDmg = Spellbook.getScaledValue(data, caster, Attribute.ADV_PHYSICAL);
        for (LivingEntity entity : targetEntity.getLocation().getNearbyLivingEntities(radius)) {
            entity.damage(Spellbook.getVariedDamage(attackDmg, caster, true), caster, DamageType.PHYSICAL);
        }
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 10));
    }
}

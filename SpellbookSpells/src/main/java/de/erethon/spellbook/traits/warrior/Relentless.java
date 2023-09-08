package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class Relentless extends SpellTrait {

    private final double attributeBonusPerEnemy = data.getDouble("attributeBonusPerEnemy", 1);
    private final double range = data.getDouble("range", 7);
    private AttributeModifier modifier;

    public Relentless(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTick() {
        int i = 0;
        for (LivingEntity livingEntity : caster.getLocation().getNearbyLivingEntities(range)) {
            if (Spellbook.canAttack(caster, livingEntity)) {
                i++;
            }
        }
        if (modifier != null) {
            caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(modifier);
            caster.getAttribute(Attribute.RES_MAGIC).removeModifier(modifier);
        }
        modifier = new AttributeModifier("Relentless", i * attributeBonusPerEnemy, AttributeModifier.Operation.ADD_NUMBER);
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.RES_MAGIC).addTransientModifier(modifier);
        super.onTick();
    }
}

package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class PurifyingFlame extends SpellTrait {

    private final int damage = data.getInt("damage", 15);

    public PurifyingFlame(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        if (trigger.getId() != 1) return;
        for (LivingEntity living : trigger.getTargets()) {
            //missing method living.damage(damage, caster, PDamageType.MAGIC);
        }
    }
}

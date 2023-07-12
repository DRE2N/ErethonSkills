package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class BetterBasicHeal extends SpellTrait {

    private final int range = data.getInt("range", 3);
    private final int allieBaseHeal = data.getInt("allieBaseHeal", 20);
    private final int selfHealPerAlly = data.getInt("selfHealPerAlly", 5);

    public BetterBasicHeal(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            if (living == caster) continue;
            if (Spellbook.canAttack(caster, living)) continue;
            living.setHealth(Math.min(living.getHealth() + allieBaseHeal * Spellbook.getScaledValue(data, caster, living, Attribute.STAT_HEALINGPOWER), living.getMaxHealth()));
            caster.setHealth(Math.min(caster.getHealth() + selfHealPerAlly, caster.getMaxHealth()));
        }

    }
}

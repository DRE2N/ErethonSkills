package de.erethon.spellbook.traits.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class BetterPiercingArrows extends SpellTrait {

    private final EffectData bleeding = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("bleeding");

    private final int bonusDamage = data.getInt("bonusDamage", 5);
    private final int bleedingDuration = data.getInt("bleedingDuration", 40);
    private final int bleedingStacks = data.getInt("bleedingStacks", 1);
    private final long bonusCooldown = data.getInt("bonusCooldown", 5);

    public BetterPiercingArrows(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell casted) {
        if (casted.getData().getId().equals("PiercingArrows")) {
            caster.getUsedSpells().put(casted.getData(), caster.getUsedSpells().get(casted.getData()) - bonusCooldown * 1000);
        }
        return super.onSpellCast(casted);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTargets()) {
            living.addEffect(caster, bleeding, bleedingDuration, bleedingStacks);
            living.damage(bonusDamage, caster, DamageType.PHYSICAL);
        }
    }
}

package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.Random;

public class InfiniteQuiver extends SpellTrait {

    private final int cooldownReduction = data.getInt("cooldownReduction", 5);
    private final double reductionChance = data.getDouble("reductionChance", 0.3);

    private final Random random = new Random();

    public InfiniteQuiver(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell casted) {
        if (random.nextDouble() <= reductionChance) {
            int spellID = random.nextInt(0, caster.getUsedSpells().size());
            int current = 0;
            for (Map.Entry<SpellData, Long> castData : caster.getUsedSpells().entrySet()) {
                if (current == spellID) {
                    caster.getUsedSpells().put(castData.getKey(), castData.getValue() - cooldownReduction);
                    return super.onSpellCast(casted);
                }
                current++;
            }
        }
        return super.onSpellCast(casted);
    }
}


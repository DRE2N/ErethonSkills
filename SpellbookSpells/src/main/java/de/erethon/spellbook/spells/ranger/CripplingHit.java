package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;

public class CripplingHit extends RangerBaseSpell{

    private final int nauseaDuration = data.getInt("nauseaDuration", 20);

    public CripplingHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(true);
    }

    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, nauseaDuration, 1, true, false));
        triggerTraits(Collections.singleton(target));
        triggerTraits(target);
        return true;
    }
}

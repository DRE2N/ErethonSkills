package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class StackingArrows extends SpellbookSpell {

    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("StackingArrowDebuff");

    public StackingArrows(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 100);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (RangerUtils.hasMana(caster, getData())) {
            target.addEffect(caster, effectData, 5, 1);
        }
        return super.onAttack(target, damage, type);
    }
}

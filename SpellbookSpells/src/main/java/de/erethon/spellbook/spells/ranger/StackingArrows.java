package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.RangerUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class StackingArrows extends RangerBaseSpell {

    private final int effectDuration = data.getInt("effectDuration", 5);
    private final int stacks = data.getInt("stacks", 1);
    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("StackingArrowDebuff");

    public StackingArrows(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (RangerUtils.hasMana(caster, getData())) {
            target.addEffect(caster, effectData, effectDuration, stacks);
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(effectDuration, VALUE_COLOR));
        placeholderNames.add("effect duration");
        spellAddedPlaceholders.add(Component.text(stacks, VALUE_COLOR));
        placeholderNames.add("stacks");
        return super.getPlaceholders(c);
    }
}

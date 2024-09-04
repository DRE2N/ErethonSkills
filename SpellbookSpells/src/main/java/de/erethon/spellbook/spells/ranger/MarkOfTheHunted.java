package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;
import java.util.List;

public class MarkOfTheHunted extends RangerBaseSpell {

    private final int effectDuration = data.getInt("effectDuration", 300);
    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("MarkOfTheHuntedEffect");

    public MarkOfTheHunted(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        if (!lookForTarget(true)) {
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        target.addEffect(caster, effectData, effectDuration, 1);
        triggerTraits(Collections.singleton(target));
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(effectDuration, VALUE_COLOR));
        placeholderNames.add("effect duration");
        return super.getPlaceholders(c);
    }
}

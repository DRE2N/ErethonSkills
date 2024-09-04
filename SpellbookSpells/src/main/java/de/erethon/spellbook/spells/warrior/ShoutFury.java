package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class ShoutFury extends AbstractWarriorShout {

    private final int effectDuration = data.getInt("duration", 200);
    private final int stacks = data.getInt("stacks", 1);
    private final EffectData effectData = Spellbook.getEffectData("Fury");
    public ShoutFury(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            if (living == caster) continue;
            if (Spellbook.canAttack(caster, living)) continue;
            living.addEffect(caster, effectData, effectDuration, stacks);
        }
        return super.onCast();
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

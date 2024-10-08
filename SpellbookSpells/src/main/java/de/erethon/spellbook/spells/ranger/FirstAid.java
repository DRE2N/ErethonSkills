package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class FirstAid extends RangerBaseSpell {

    private final double baseHeal = data.getDouble("baseHeal", 20);

    public FirstAid(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        caster.setHealth(Math.min(caster.getHealth() + baseHeal + Spellbook.getScaledValue(data, caster, Attribute.STAT_HEALINGPOWER), caster.getMaxHealth()));
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(baseHeal + Spellbook.getScaledValue(data, caster, Attribute.STAT_HEALINGPOWER), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("heal amount");
        return super.getPlaceholders(c);
    }
}

package de.erethon.spellbook.spells.warrior.duelist;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class AbstractWarriorStance extends WarriorBaseSpell {

    public int duration = data.getInt("duration", 10);
    public int attributeBonus = data.getInt("attributeBonus", 20);

    public AbstractWarriorStance(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        placeholderNames.add("duration");
        spellAddedPlaceholders.add(Component.text(attributeBonus, VALUE_COLOR));
        placeholderNames.add("attribute bonus");
        return super.getPlaceholders(c);
    }
}

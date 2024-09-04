package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public abstract class AbstractWarriorShout extends WarriorBaseSpell {

    public int range = data.getInt("range", 10);

    public AbstractWarriorShout(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(range, VALUE_COLOR));
        placeholderNames.add("range");
        return super.getPlaceholders(c);
    }
}

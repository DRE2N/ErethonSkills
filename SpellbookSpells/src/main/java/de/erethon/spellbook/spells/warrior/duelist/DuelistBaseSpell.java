package de.erethon.spellbook.spells.warrior.duelist;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;

public class DuelistBaseSpell extends WarriorBaseSpell {

    protected static final Color DUELIST_PRIMARY = Color.fromRGB(58, 124, 201);   // Steel Blue
    protected static final Color DUELIST_ACCENT = Color.fromRGB(192, 192, 192);   // Silver

    protected static final String PARRY_TAG = "duelist.parry";

    public DuelistBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }
}

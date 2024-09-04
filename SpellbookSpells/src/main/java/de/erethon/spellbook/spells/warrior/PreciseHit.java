package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class PreciseHit extends WarriorBaseSpell {

    public int BonusDamage = data.getInt("bonusDamage", 10);

    public PreciseHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }
    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        keepAliveTicks = 0;
        interrupt();
        return damage + BonusDamage;
    }
    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(BonusDamage, VALUE_COLOR));
        placeholderNames.add("bonus damage");
        return super.getPlaceholders(c);
    }
}







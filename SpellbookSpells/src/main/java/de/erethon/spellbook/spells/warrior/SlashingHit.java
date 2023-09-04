package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class SlashingHit extends WarriorBaseSpell {

    private final EffectData bleeding = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Bleeding");

    public SlashingHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(3);
    }

    @Override
    protected boolean onCast() {
        target.addEffect(caster, bleeding, (int) (data.getInt("baseDuration", 40) + Spellbook.getScaledValue(data, caster, target, Attribute.ADV_PHYSICAL)), data.getInt("stacks", 4));
        return super.onCast();
    }
}

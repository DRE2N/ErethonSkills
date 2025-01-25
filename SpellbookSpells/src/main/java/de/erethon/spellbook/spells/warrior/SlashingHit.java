package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class SlashingHit extends WarriorBaseSpell {

    private final EffectData bleeding = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Bleeding");
    private final int effectDuration = data.getInt("baseDuration", 40);
    private final int stacks = data.getInt("stacks", 1);
    public double damageMultiplier = 1.0;
    public double bleedingStackMultiplier = 1.0;

    public SlashingHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(3);
    }

    @Override
    public boolean onCast() {
        target.addEffect(caster, bleeding, effectDuration + ((int) Spellbook.getScaledValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL)), (int) (stacks * bleedingStackMultiplier));
        //missing method target.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL) * damageMultiplier, caster, PDamageType.PHYSICAL);
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(Spellbook.getVariedAttributeBasedDamage(data, caster, caster, true, Attribute.ADVANTAGE_PHYSICAL) * damageMultiplier, ATTR_PHYSICAL_COLOR));
        placeholderNames.add("damage");
        spellAddedPlaceholders.add(Component.text(effectDuration + ((int) Spellbook.getScaledValue(data, caster, caster, Attribute.ADVANTAGE_PHYSICAL)), VALUE_COLOR));
        placeholderNames.add("effect duration");
        spellAddedPlaceholders.add(Component.text((int) (stacks * bleedingStackMultiplier), VALUE_COLOR));
        placeholderNames.add("stacks");
        return super.getPlaceholders(c);
    }
}

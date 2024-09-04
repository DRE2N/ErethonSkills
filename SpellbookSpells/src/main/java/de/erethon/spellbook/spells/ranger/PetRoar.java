package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class PetRoar extends RangerPetBaseSpell {

    private final int effectDuration = data.getInt("baseDuration", 20);
    private final int stacks = data.getInt("stacks", 1);
    private final double radius = data.getDouble("radius", 8);
    private final EffectData weakness = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Weakness");

    public PetRoar(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        for (LivingEntity living : pet.getLocation().getNearbyLivingEntities(radius)) {
            if (living == caster) continue;
            if (!Spellbook.canAttack(caster, living)) continue;
            living.addEffect(caster, weakness, effectDuration + (int) Math.round(Spellbook.getScaledValue(data, living, Attribute.ADV_MAGIC)), stacks);
        }
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(effectDuration + Math.round(Spellbook.getScaledValue(data, caster, Attribute.ADV_MAGIC)), VALUE_COLOR));
        placeholderNames.add("effect duration");
        spellAddedPlaceholders.add(Component.text(data.getInt("stacks", 1), VALUE_COLOR));
        placeholderNames.add("stacks");
        spellAddedPlaceholders.add(Component.text(data.getDouble("radius", 8), VALUE_COLOR));
        placeholderNames.add("radius");
        return super.getPlaceholders(c);
    }
}

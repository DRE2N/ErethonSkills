package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class PetRoar extends RangerPetBaseSpell {

    private final EffectData weakness = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Weakness");

    public PetRoar(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        for (LivingEntity living : pet.getLocation().getNearbyLivingEntities(data.getDouble("radius", 8))) {
            if (living == caster) continue;
            if (!Spellbook.canAttack(caster, living)) continue;
            living.addEffect(caster, weakness, (int) (data.getInt("baseDuration", 20) + Math.round(Spellbook.getScaledValue(data, living, Attribute.ADV_MAGIC))), data.getInt("stacks", 1));
        }
        return super.onCast();
    }
}

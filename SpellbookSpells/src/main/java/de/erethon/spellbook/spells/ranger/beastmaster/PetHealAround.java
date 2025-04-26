package de.erethon.spellbook.spells.ranger.beastmaster;

import com.destroystokyo.paper.ParticleBuilder;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class PetHealAround extends RangerPetBaseSpell {


    private final int range = data.getInt("range", 4);
    private final int baseHeal = data.getInt("baseHeal", 100);

    public PetHealAround(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        for (LivingEntity living : pet.getBukkitLivingEntity().getLocation().getNearbyLivingEntities(range)) {
            if (living == caster) continue;
            if (living == pet.getBukkitLivingEntity()) continue;
            if (Spellbook.canAttack(caster, living)) continue;
            living.setHealth(Math.min(living.getHealth() + baseHeal * Spellbook.getScaledValue(data, caster, living, Attribute.STAT_HEALINGPOWER), living.getMaxHealth()));
            new ParticleBuilder(Particle.HEART).location(living.getLocation().add(0, 1, 0)).count(3).spawn();
        }
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(range, VALUE_COLOR));
        placeholderNames.add("range");
        spellAddedPlaceholders.add(Component.text(baseHeal + Spellbook.getScaledValue(data, caster, Attribute.STAT_HEALINGPOWER), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("heal amount");
        return super.getPlaceholders(c);
    }
}

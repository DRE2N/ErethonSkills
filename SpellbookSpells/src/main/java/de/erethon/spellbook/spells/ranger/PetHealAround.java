package de.erethon.spellbook.spells.ranger;

import com.destroystokyo.paper.ParticleBuilder;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

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
}

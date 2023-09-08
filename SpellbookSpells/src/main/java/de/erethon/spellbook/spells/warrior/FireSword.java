package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class FireSword extends WarriorBaseSpell {

    private final int bonusDamage = data.getInt("bonusDamage", 10);

    public FireSword(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 200);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (type == DamageType.MAGIC) {
            return super.onDamage(target, damage, type);
        }
        target.damage(damage + bonusDamage, caster, DamageType.MAGIC);
        target.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 1, new Particle.DustOptions(Color.ORANGE,3f));
        triggerTraits(target);
        return 0; // We can't deal damage twice in the same attack.
    }
}
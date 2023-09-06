package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class Evasion extends AssassinBaseSpell {

    Random random = new Random();
    double chance = data.getDouble("chance", 0.7);

    public Evasion(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 3) * 20;
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        if (random.nextDouble() < chance) {
            return 0;
        }
        return super.onDamage(attacker, damage, type);
    }
}


package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Random;

public class Evasion extends AssassinBaseSpell {

    Random random = new Random();
    double chance = data.getDouble("chance", 0.7);
    private final int duration = data.getInt("duration", 20);

    public Evasion(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (random.nextDouble() < chance) {
            return 0;
        }
        return super.onDamage(attacker, damage, type);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        spellAddedPlaceholders.add(Component.text(chance, VALUE_COLOR));
        return super.getPlaceholders(caster);
    }
}


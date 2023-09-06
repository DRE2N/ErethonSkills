package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class PoisonWeapon extends AssassinBaseSpell {

    int count = data.getInt("count", 3);
    EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Poison");

    public PoisonWeapon(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 200);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (count > 0) {
            count--;
            target.addEffect(caster, effectData, 1, 1);
        }
        return super.onAttack(target, damage, type);
    }
}

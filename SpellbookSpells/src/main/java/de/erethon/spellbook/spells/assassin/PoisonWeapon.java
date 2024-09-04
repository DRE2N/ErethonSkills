package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class PoisonWeapon extends AssassinBaseSpell {

    private final int count = data.getInt("count", 3);
    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Poison");
    private int currentCount = 0;

    public PoisonWeapon(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        currentCount = count;
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (currentCount > 0) {
            currentCount--;
            target.addEffect(caster, effectData, 1, 1);
            triggerTraits(target);
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(count, VALUE_COLOR));
        placeholderNames.add("count");
        return super.getPlaceholders(c);
    }
}

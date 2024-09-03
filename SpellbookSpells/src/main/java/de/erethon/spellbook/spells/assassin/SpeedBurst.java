package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class SpeedBurst extends AssassinBaseSpell {

    private final int duration = data.getInt("duration", 10);

    public SpeedBurst(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Speed");
        caster.addEffect(caster, effectData, duration * 20, 1);
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        return super.getPlaceholders(c);
    }
}


package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class ShoutRoar extends AbstractWarriorShout {

    private final EffectData effect = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("RoarDebuff");
    private final int effectDuration = data.getInt("effectDuration", 40);

    public ShoutRoar(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        caster.getWorld().playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.RECORD, 0.8f, 1));
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            if (living == caster) continue;
            if (!Spellbook.canAttack(caster, living)) continue;
            living.addEffect(caster, effect, effectDuration, 1);
        }
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(effectDuration, VALUE_COLOR));
        placeholderNames.add("effect duration");
        return super.getPlaceholders(c);
    }
}

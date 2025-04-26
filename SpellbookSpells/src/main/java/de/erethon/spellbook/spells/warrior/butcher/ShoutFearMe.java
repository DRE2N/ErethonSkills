package de.erethon.spellbook.spells.warrior.butcher;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class ShoutFearMe extends AbstractWarriorShout {

    private EffectData fearEffect = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Fear");
    public int effectDuration = data.getInt("effectDuration", 10);

    public ShoutFearMe(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Location inFront = caster.getLocation().add(caster.getLocation().getDirection().multiply(2));
        for (LivingEntity entity : inFront.getWorld().getNearbyEntitiesByType(LivingEntity.class, inFront, range)) {
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }
            entity.addEffect(caster, fearEffect, effectDuration, 1);
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

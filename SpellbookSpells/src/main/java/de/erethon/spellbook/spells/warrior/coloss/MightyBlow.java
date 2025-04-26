package de.erethon.spellbook.spells.warrior.coloss;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;

public class MightyBlow extends WarriorBaseSpell {

    public double rangeMultiplier = data.getInt("rangeMultiplier", 2);
    private final int radius = data.getInt("radius", 3);
    private final double pushStrength = data.getDouble("pushStrength", 2);

    public MightyBlow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Location location = caster.getLocation();
        Vector inFront = location.toVector().add(location.getDirection().multiply(rangeMultiplier));
        inFront.setY(location.getY());
        caster.playSound(Sound.sound(org.bukkit.Sound.ITEM_FIRECHARGE_USE, Sound.Source.RECORD, 1, 1));
        inFront.toLocation(caster.getWorld()).getNearbyLivingEntities(radius).forEach(entity -> {
            if (entity == caster) return;
            if (!Spellbook.canAttack(caster, entity)) return;
            entity.setVelocity(location.getDirection().multiply(pushStrength));
            entity.playSound(Sound.sound(org.bukkit.Sound.ITEM_FIRECHARGE_USE, Sound.Source.RECORD, 1, 1));
        });
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(radius, VALUE_COLOR));
        placeholderNames.add("radius");
        return super.getPlaceholders(c);
    }
}

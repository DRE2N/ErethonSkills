package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;

public class PushBack extends RangerBaseSpell {

    private final double pushMultiplier = data.getDouble("pushMultiplier", 2);
    private final int radius = data.getInt("radius", 2);

    public PushBack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Vector inFront = caster.getLocation().getDirection().multiply(2);
        Vector pushVector = inFront.multiply(pushMultiplier);
        Location target = caster.getLocation().add(inFront);
        for (LivingEntity entity : target.getNearbyLivingEntities(radius)) {
            if (entity == caster) continue;
            if (!Spellbook.canAttack(caster, entity)) continue;
            entity.setVelocity(pushVector);
        }
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(radius, VALUE_COLOR));
        placeholderNames.add("radius");
        return super.getPlaceholders(c);
    }
}

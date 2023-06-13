package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class PushBack extends RangerBaseSpell {

    private final double pushMultiplier = data.getDouble("pushMultiplier", 2);
    private final int radius = data.getInt("radius", 2);

    public PushBack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        Vector inFront = caster.getLocation().getDirection().multiply(2);
        Vector pushVector = inFront.multiply(pushMultiplier);
        Location target = caster.getLocation().add(inFront);
        target.getNearbyLivingEntities(radius).forEach(entity -> entity.setVelocity(pushVector));
        return super.onCast();
    }
}

package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;

public class EvasiveRetreat extends RangerBaseSpell {


    public EvasiveRetreat(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 10);
    }

    @Override
    public boolean onCast() {
        Arrow arrow = caster.getWorld().spawn(caster.getLocation(), Arrow.class);
        arrow.setVelocity(caster.getLocation().getDirection().multiply(3));
        arrow.setDamageType(PDamageType.MAGIC);
        arrow.setGravity(false);
        Location location = caster.getLocation();
        location.setPitch(-10);
        caster.setVelocity(location.getDirection().multiply(data.getDouble("retreatMultiplier", 2.0) * -1));
        caster.setInvulnerable(true);
        return true;
    }

    @Override
    protected void onTickFinish() {
        caster.setInvulnerable(false);
        super.onTickFinish();
    }
}

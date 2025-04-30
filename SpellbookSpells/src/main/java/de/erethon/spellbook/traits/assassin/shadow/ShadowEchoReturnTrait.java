package de.erethon.spellbook.traits.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class ShadowEchoReturnTrait extends SpellTrait {

    private Location returnLocation;
    private boolean isWaitingForReturn = false;
    private boolean hasDamagedMarkedTarget = false;

    public ShadowEchoReturnTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (target.getTags().contains("assassin.daggerthrow.marked")) {
            hasDamagedMarkedTarget = true;
        }
        return super.onAttack(target, damage, type);
    }

    public void setReturnLocation(Location location) {
        this.returnLocation = location;
        this.isWaitingForReturn = true;
    }

    public boolean isWaitingForReturn() {
        return isWaitingForReturn;
    }

    public Location getReturnLocation() {
        return returnLocation;
    }

    public boolean hasDamagedMarkedTarget() {
        return hasDamagedMarkedTarget;
    }
}

package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.utils.RangerUtils;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.LivingEntity;

public class KnockbackArrow extends RangerBaseSpell {

    private final double knockbackMultiplier = data.getDouble("knockbackMultiplier", 0.4);

    public KnockbackArrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(true);
    }

    @Override
    public boolean onCast() {
        RangerUtils.sendProjectile(caster, target, caster, 2, 0, DamageType.PHYSICAL);
        double distance = target.getLocation().distance(caster.getLocation());
        distance = distance / 10;
        double knockback = Math.min(0, 16 - (knockbackMultiplier * distance));
        target.setVelocity(caster.getLocation().getDirection().multiply(knockback));
        target.playSound(Sound.sound(org.bukkit.Sound.ENTITY_GENERIC_SWIM, Sound.Source.RECORD, 1, 1));
        return super.onCast();
    }
}

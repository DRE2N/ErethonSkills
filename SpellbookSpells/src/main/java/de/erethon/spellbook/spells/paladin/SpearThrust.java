package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.LivingEntity;

public class SpearThrust extends PaladinSpearSpell {

    private final double velocity = data.getDouble("velocity", 1.5);

    public SpearThrust(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(3);
    }

    @Override
    protected boolean onCast() {
        target.setVelocity(caster.getLocation().getDirection().multiply(velocity));
        target.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.RECORD, 0.8f, 1));
        triggerTraits(target);
        return super.onCast();
    }
}

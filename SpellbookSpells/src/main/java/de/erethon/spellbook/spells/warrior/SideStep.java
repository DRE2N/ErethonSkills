package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.LivingEntity;

public class SideStep extends WarriorBaseSpell {

    private final double velocity = data.getDouble("velocity", 1.2f);
    private final double rotationAngle = data.getDouble("rotationAngle", 45);
    private float startYaw;

    public SideStep(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        startYaw = caster.getYaw();
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_STONE_BUTTON_CLICK_ON, Sound.Source.RECORD, 1, 1));
        return super.onCast();
    }

    @Override
    protected void onTickFinish() {
        if (startYaw >= caster.getYaw()) {
            caster.setVelocity(caster.getVelocity().add(caster.getLocation().getDirection().rotateAroundY(rotationAngle).multiply(velocity)));
        }
        else {
            caster.setVelocity(caster.getVelocity().add(caster.getLocation().getDirection().rotateAroundY(-rotationAngle).multiply(velocity)));
        }
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PHANTOM_SWOOP, Sound.Source.RECORD, 1, 1.5f));
    }
}

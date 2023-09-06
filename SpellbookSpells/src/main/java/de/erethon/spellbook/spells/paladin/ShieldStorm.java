package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class ShieldStorm extends PaladinBaseSpell {

    private final double stormSpeed = data.getDouble("stormSpeed", 1.5);
    private final double throwbackSpeed = data.getDouble("throwbackSpeed", 2);

    private final int delay = data.getInt("delay", 40);

    public ShieldStorm(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(8);
    }

    @Override
    protected boolean onCast() {
        caster.setVelocity(caster.getLocation().getDirection().multiply(stormSpeed));
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.RECORD, 1, 0));
        BukkitRunnable runLater = new BukkitRunnable() {
            @Override
            public void run() {
                if (caster.getLocation().distanceSquared(target.getLocation()) > 4) return; // Don't throw if the caster didn't get close enough because they hit something
                target.setVelocity(caster.getLocation().getDirection().multiply(-throwbackSpeed));
                target.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.RECORD, 1, 0));
                triggerTraits();
            }
        };
        runLater.runTaskLater(Spellbook.getInstance().getImplementer(), delay);
        return super.onCast();
    }
}
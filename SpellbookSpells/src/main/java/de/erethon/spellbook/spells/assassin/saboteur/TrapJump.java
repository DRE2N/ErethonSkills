package de.erethon.spellbook.spells.assassin.saboteur;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.spells.assassin.AssassinBaseTrap;
import de.erethon.spellbook.traits.assassin.saboteur.TrapTrackingTrait;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class TrapJump extends AssassinBaseSpell {

    private final double maxDistance = data.getDouble("maxDistance", 10.0);
    private final int chargeUpTicks = data.getInt("chargeUpTicks", 20);

    private TrapTrackingTrait trapTrackingTrait;

    // TrapJump: Teleports to the closest trap within a certain distance. The caster is blinded for a short time.

    public TrapJump(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        for (SpellTrait trait : caster.getActiveTraits()) {
            if (trait instanceof TrapTrackingTrait) {
                trapTrackingTrait = (TrapTrackingTrait) trait;
                break;
            }
        }
        if (trapTrackingTrait == null) {
            return false;
        }
        if (trapTrackingTrait.getTraps().isEmpty()) {
            caster.sendParsedActionBar("<red>No traps!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        if (trapTrackingTrait == null) {
            caster.sendParsedActionBar("<red>Missing trait.");
            return false;
        }
        if (trapTrackingTrait.getTraps().isEmpty()) {
            caster.sendParsedActionBar("<red>No traps!");
            return false;
        }
        AssassinBaseTrap trap = trapTrackingTrait.getClosestTrap(caster.getLocation());
        if (trap == null) {
            caster.sendParsedActionBar("<red>No traps!");
            return false;
        }
        if (trap.target.distance(caster.getLocation()) > maxDistance) {
            caster.sendParsedActionBar("<red>Trap too far!");
            return false;
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.0f);
        caster.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                caster.teleport(trap.target);
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.0f);
                caster.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        };
        runnable.runTaskLater(Spellbook.getInstance().getImplementer(), chargeUpTicks);
        return super.onCast();
    }
}

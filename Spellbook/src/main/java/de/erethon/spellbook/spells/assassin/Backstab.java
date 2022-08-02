package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import de.slikey.effectlib.effect.CylinderEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Backstab extends AssassinBaseSpell {

    Entity target;
    Location location = null;

    public Backstab(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    protected boolean onPrecast() {
        target = caster.getEntity().getTargetEntity(data.getInt("range", 10));
        if (target == null) {
            caster.sendActionbar("<red>Kein Target!");
            return false;
        }
        location = target.getLocation().clone().toVector().subtract(target.getLocation().getDirection().multiply(1.5)).toLocation(target.getWorld());
        if (location.getBlock().isSolid()) {
            caster.sendActionbar("<red>Kein Platz!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        CylinderEffect effectTarget = new CylinderEffect(effectManager);
        effectTarget.setLocation(location);
        effectTarget.particle = Particle.REDSTONE;
        effectTarget.particleSize = 0.4f;
        effectTarget.color = Color.BLACK;
        effectTarget.duration = 500;
        effectTarget.height = 1.5f;
        effectTarget.start();
        CylinderEffect effectCaster = new CylinderEffect(effectManager);
        effectCaster.setEntity(caster.getEntity());
        effectCaster.particle = Particle.REDSTONE;
        effectCaster.particleSize = 0.4f;
        effectCaster.color = Color.BLACK;
        effectCaster.duration = 500;
        effectCaster.height = 1.0f;
        effectCaster.start();
        keepAliveTicks = 5;
        return true;
    }

    @Override
    protected void onTickFinish() {
        float yaw = target.getLocation().getYaw();
        float pitch = caster.getEntity().getLocation().getPitch();
        location.setYaw(yaw);
        location.setPitch(pitch);
        caster.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
        caster.getEntity().teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        caster.getEntity().attack(target);
    }
}

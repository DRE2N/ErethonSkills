package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AssassinBackstab extends AssassinBaseSpell {

    Entity target;
    Location location = null;

    public AssassinBackstab(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    protected boolean onPrecast() {
        target = caster.getTargetEntity(data.getInt("range", 10));
        if (target == null) {
            caster.sendActionbar("<red>Kein Target!");
            return false;
        }
        location = target.getLocation().clone().toVector().subtract(target.getLocation().getDirection().multiply(1.5)).toLocation(target.getWorld());
        if (location.getBlock().isSolid()) {
            caster.sendActionbar("<red>Kein Platz!");
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        EffectManager manager = Spellbook.getInstance().getEffectManager();
        CylinderEffect effectTarget = new CylinderEffect(manager);
        effectTarget.setLocation(location);
        effectTarget.particle = Particle.REDSTONE;
        effectTarget.particleSize = 0.4f;
        effectTarget.color = Color.BLACK;
        effectTarget.duration = 500;
        effectTarget.height = 1.5f;
        effectTarget.start();
        CylinderEffect effectCaster = new CylinderEffect(manager);
        effectCaster.setEntity(caster);
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
        float pitch = caster.getLocation().getPitch();
        location.setYaw(yaw);
        location.setPitch(pitch);
        caster.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
        caster.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        caster.attack(target);
    }
}

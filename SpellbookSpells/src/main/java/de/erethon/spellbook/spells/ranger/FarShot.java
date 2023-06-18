package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class FarShot extends ProjectileRelatedSkill {

    private Location startLocation;

    public FarShot(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        trailColor = Color.GREEN;
    }

    @Override
    protected void onShoot(EntityShootBowEvent event) {
        startLocation = event.getEntity().getLocation();
    }

    @Override
    protected void onDamage(EntityDamageByEntityEvent event, Projectile projectile) {
        double distance = projectile.getLocation().distance(startLocation);
        double damage = distance * (Spellbook.getScaledValue(data, caster, (LivingEntity) event.getEntity(), Attribute.ADV_MAGIC));
        ((LivingEntity) event.getEntity()).damage(damage, DamageType.MAGIC);
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_STRONG, Sound.Source.RECORD, 1, 0));
    }
}

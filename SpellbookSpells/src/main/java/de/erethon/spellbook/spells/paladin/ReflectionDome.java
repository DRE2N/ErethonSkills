package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;

import java.util.List;

public class ReflectionDome extends PaladinBaseSpell {

    private final double radius = data.getDouble("radius", 2);

    private SphereEffect sphereEffect;

    public ReflectionDome(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        channelDuration = duration * 20;
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        sphereEffect = new SphereEffect(Spellbook.getInstance().getEffectManager());
        sphereEffect.color = Color.BLUE;
        sphereEffect.radius = radius - 1;
        sphereEffect.particles = 50;
        sphereEffect.particle = Particle.DUST;
        sphereEffect.setEntity(caster);
        sphereEffect.duration = keepAliveTicks * 50;
        sphereEffect.start();
        return super.onCast();
    }

    @Override
    protected void onTick() {
        Location casterLoc = caster.getLocation();
        for (Entity entity : caster.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Projectile projectile) {
                Location loc = projectile.getLocation();
                // Don't reflect projectiles shot from inside the dome
                if (projectile.getShooter() != caster && loc.distanceSquared(casterLoc) < radius * radius && loc.distanceSquared(casterLoc) > radius - 1 * radius - 1)  {
                    projectile.setVelocity(projectile.getVelocity().multiply(-1));
                }
            }
        }
    }

    @Override
    protected void cleanup() {
        sphereEffect.cancel();
    }

    @Override
    protected void onTickFinish() {
        triggerTraits(); // Maybe some finish effects would be cool
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(radius, VALUE_COLOR));
        placeholderNames.add("radius");
        return super.getPlaceholders(c);
    }
}

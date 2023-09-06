package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.slikey.effectlib.effect.SphereEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;

public class ArrowShield extends AoEBaseSpell {

    private SphereEffect sphereEffect;

    public ArrowShield(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("keepAliveTicks", 200);
    }

    @Override
    protected boolean onCast() {
        sphereEffect = new SphereEffect(Spellbook.getInstance().getEffectManager());
        sphereEffect.color = Color.ORANGE;
        sphereEffect.radius = size - 1;
        sphereEffect.particles = 50;
        sphereEffect.particle = Particle.REDSTONE;
        sphereEffect.setEntity(caster);
        sphereEffect.duration = keepAliveTicks * 50;
        sphereEffect.start();
        return super.onCast();
    }

    @Override
    protected void onTick() {
        target.getNearbyEntitiesByType(Projectile.class, size, size, size).forEach(Entity::remove);
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        sphereEffect.cancel();
    }
}
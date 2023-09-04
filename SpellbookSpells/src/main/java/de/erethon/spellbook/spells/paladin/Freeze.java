package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class Freeze extends PaladinBaseSpell {

    private final AttributeModifier modifier = new AttributeModifier("Freeze", -10000, AttributeModifier.Operation.ADD_NUMBER);
    private final PotionEffect noJump = new PotionEffect(PotionEffectType.JUMP, PotionEffect.INFINITE_DURATION, 128, false, false, false);

    private final Set<LivingEntity> frozen = new HashSet<>();
    private final Set<CircleEffect> circles = new HashSet<>();

    public Freeze(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("keepAliveTicks", 200);
    }

    @Override
    protected boolean onCast() {
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(data.getDouble("radius", 8))) {
            if (!Spellbook.canAttack(caster, living)) continue;
            frozen.add(living);
            living.setVelocity(new Vector());
            living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
            living.addPotionEffect(noJump);
            CircleEffect circle = new CircleEffect(Spellbook.getInstance().getEffectManager());
            circle.radius = 1.2f;
            circle.particle = Particle.SNOWFLAKE;
            circle.particleCount = 8;
            circle.duration = keepAliveTicks * 50;
            circle.iterations = -1;
            circle.setEntity(living);
            circle.start();
            circles.add(circle);
        }
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        for (LivingEntity living : frozen) {
            living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
            living.removePotionEffect(PotionEffectType.JUMP);
        }
        for (CircleEffect circle : circles) {
            circle.cancel();
        }
    }
}


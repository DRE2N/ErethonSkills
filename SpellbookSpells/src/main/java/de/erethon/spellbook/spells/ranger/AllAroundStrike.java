package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllAroundStrike extends RangerBaseSpell {

    private final NamespacedKey key = new NamespacedKey("spellbook", "allaroundstrike");
    private double radius = data.getDouble("startRadius", 2);
    private final double radiusPerTick = data.getDouble("radiusPerTick", 0.2);
    private final AttributeModifier attackBaseMod = new AttributeModifier(key, data.getDouble("attackModifier", -5), AttributeModifier.Operation.ADD_NUMBER);

    private CircleEffect circle = new CircleEffect(Spellbook.getInstance().getEffectManager());
    private CircleEffect attackMarker = new CircleEffect(Spellbook.getInstance().getEffectManager());

    public AllAroundStrike(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration;
    }

    @Override
    public boolean onCast() {
        Location casterLoc = caster.getLocation();
        Location location = new Location(caster.getWorld(), casterLoc.getX(), casterLoc.getY() + 1, casterLoc.getZ(), casterLoc.getYaw(), 0f);
        location.setPitch(0f);
        location.setYaw(casterLoc.getYaw());
        circle.particle = Particle.DUST;
        circle.color = Color.GREEN;
        circle.particleSize = 0.3f;
        circle.particles = 20;
        circle.duration = keepAliveTicks * 50;
        circle.iterations = -1;
        circle.period = 2;
        circle.wholeCircle = true;
        circle.resetCircle = true;
        circle.enableRotation = true;
        circle.maxAngle = Math.PI;
        circle.radius = (float) radius;
        circle.setLocation(location);
        circle.start();
        attackMarker.particle = Particle.SWEEP_ATTACK;
        attackMarker.particles = 5;
        attackMarker.wholeCircle = true;
        attackMarker.resetCircle = true;
        attackMarker.maxAngle = Math.PI;
        attackMarker.enableRotation = true;
        attackMarker.iterations = -1;
        attackMarker.period = 10;
        attackMarker.duration = keepAliveTicks * 50;
        attackMarker.setLocation(location);
        attackMarker.maxAngle = Math.PI;
        attackMarker.start();
        caster.getAttribute(Attribute.ATTACK_DAMAGE).addTransientModifier(attackBaseMod);
        return true;
    }

    @Override
    protected void onTick() {
        Location casterLoc = caster.getLocation();
        Location location = new Location(caster.getWorld(), casterLoc.getX(), casterLoc.getY() + 1, casterLoc.getZ(), casterLoc.getYaw(), 0f);
        circle.setLocation(location);
        attackMarker.setLocation(location);
        attackMarker.radius = (float) radius; // Inside the other particles
        caster.swingMainHand();
        radius += radiusPerTick;
        circle.radius = (float) radius;
        Set<LivingEntity> entities = new HashSet<>();
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(radius)) {
            if (living == caster || !Spellbook.canAttack(caster, living)) {
                continue;
            }
            living.setNoDamageTicks(0);
            NamespacedKey perEntityKey = new NamespacedKey("spellbook", "allaroundstrike" + living.getEntityId());
            AttributeModifier attackBonus = new AttributeModifier(perEntityKey, Spellbook.getScaledValue(data, caster, living, Attribute.ADVANTAGE_PHYSICAL), AttributeModifier.Operation.ADD_NUMBER);
            caster.getAttribute(Attribute.ATTACK_DAMAGE).addTransientModifier(attackBonus);
            caster.attack(living);
            caster.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(attackBonus);
            entities.add(living);
        }
        triggerTraits(entities);
    }

    @Override
    protected void cleanup() {
        caster.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(attackBaseMod);
        circle.cancel();
        attackMarker.cancel();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(radius, VALUE_COLOR));
        placeholderNames.add("radius");
        spellAddedPlaceholders.add(Component.text(Spellbook.getScaledValue(data, caster, caster, Attribute.ATTACK_DAMAGE), ATTR_PHYSICAL_COLOR));
        placeholderNames.add("damage");
        return super.getPlaceholders(c);
    }
}
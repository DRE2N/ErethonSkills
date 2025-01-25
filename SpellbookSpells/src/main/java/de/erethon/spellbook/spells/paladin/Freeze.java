package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Freeze extends PaladinBaseSpell {

    private final int duration = data.getInt("duration", 10);
    private final int radius = data.getInt("radius", 8);
    private final NamespacedKey key = new NamespacedKey("spellbook", "freeze");
    private final AttributeModifier modifier = new AttributeModifier(key, -10000, AttributeModifier.Operation.ADD_NUMBER);

    private final Set<LivingEntity> frozen = new HashSet<>();
    private final Set<CircleEffect> circles = new HashSet<>();

    public Freeze(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(radius)) {
            if (!Spellbook.canAttack(caster, living)) continue;
            frozen.add(living);
            living.setVelocity(new Vector());
            living.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(modifier);
            living.getAttribute(Attribute.JUMP_STRENGTH).addTransientModifier(modifier);
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
            living.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(modifier);
            living.getAttribute(Attribute.JUMP_STRENGTH).removeModifier(modifier);
        }
        for (CircleEffect circle : circles) {
            circle.cancel();
        }
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(radius, VALUE_COLOR));
        placeholderNames.add("radius");
        return super.getPlaceholders(c);
    }
}


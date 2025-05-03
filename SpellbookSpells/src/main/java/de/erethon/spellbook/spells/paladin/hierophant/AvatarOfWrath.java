package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class AvatarOfWrath extends PaladinBaseSpell {

    // The Hierophant becomes an avatar of wrath, gaining increased damage and attack range, cleaving attacks and emits a pulse of holy fire on every attack.
    // The holy fire deals magic damage, burns and weakens nearby enemies.
    // Normal attacks cleave and deal 50% increased damage. ALl other spells are disabled.

    private final int duration = data.getInt("duration", 15);
    private final double magicDamageBonus = data.getDouble("magicDamageBonus", 1.5f);
    private final double attackRangeBonus = data.getDouble("attackRangeBonus", 4.0f);
    private final double cleaveDamageMultiplier = data.getDouble("cleaveDamageMultiplier", 1.5f);
    private final double cleaveRadius = data.getDouble("cleaveRadius", 3.0);
    private final int weaknessDurationMin = data.getInt("weaknessDurationMin", 40);
    private final int weaknessDurationMax = data.getInt("weaknessDurationMax", 200);
    private final int burningDurationMin = data.getInt("burningDurationMin", 40);
    private final int burningDurationMax = data.getInt("burningDurationMax", 200);
    private final int burningStacksMin = data.getInt("burningStacksMin", 1);
    private final int burningStacksMax = data.getInt("burningStacksMax", 3);
    private final double sizeIncrease = data.getDouble("sizeIncrease", 1.2f);

    private final AttributeModifier bonusDamageModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:avatar_wrath"), magicDamageBonus, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    private final AttributeModifier sizeModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:avatar_wrath_size"), sizeIncrease, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    private final AttributeModifier attackRangeModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:avatar_wrath_range"), attackRangeBonus, AttributeModifier.Operation.ADD_NUMBER);
    private final EffectData weaknessEffect = Spellbook.getEffectData("Weakness");
    private final EffectData burningEffect = Spellbook.getEffectData("Burning");

    public AvatarOfWrath(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).addTransientModifier(bonusDamageModifier);
        caster.getAttribute(Attribute.SCALE).addTransientModifier(sizeModifier);
        caster.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).addTransientModifier(attackRangeModifier);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        caster.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, caster.getLocation(), 2, 0.5, 0.5, 0.5);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        CircleEffect circleEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circleEffect.setLocation(caster.getLocation());
        circleEffect.radius = (float) cleaveRadius;
        circleEffect.particle = Particle.SOUL_FIRE_FLAME;
        circleEffect.particleCount = 20;
        circleEffect.particleOffsetY = 0.5f;
        circleEffect.duration = 40;
        circleEffect.start();
        for (LivingEntity livingEntity : caster.getLocation().getNearbyLivingEntities(cleaveRadius)) {
            if (Spellbook.canAttack(caster, livingEntity)) {
                double cleaveDamage = damage * cleaveDamageMultiplier;
                livingEntity.damage(cleaveDamage, caster, PDamageType.MAGIC);
                int weaknessDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration");
                livingEntity.addEffect(livingEntity, weaknessEffect, weaknessDuration, 1);
                int burningDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, burningDurationMin, burningDurationMax, "burningDuration");
                int burningStacks = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, burningStacksMin, burningStacksMax, "burningStacks");
                livingEntity.addEffect(livingEntity, burningEffect, burningDuration, burningStacks);
            }
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    public boolean onCast(SpellbookSpell spell) {
        return false;
    }

    @Override
    protected void cleanup() {
        caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).removeModifier(bonusDamageModifier);
        caster.getAttribute(Attribute.SCALE).removeModifier(sizeModifier);
        caster.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).removeModifier(attackRangeModifier);
        super.cleanup();
    }
}

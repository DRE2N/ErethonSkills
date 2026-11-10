package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;

import de.erethon.spellbook.spells.vfx.TerrainRing;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class AvatarOfWrath extends HierophantBaseSpell {

    // The Hierophant becomes an avatar of wrath, gaining increased damage and attack range, cleaving attacks and emits a pulse of holy fire on every attack.
    // The holy fire deals magic damage, burns and weakens nearby enemies.
    // Normal attacks cleave in a cone and deal 50% increased damage. All other spells are disabled.
    // Creates a burning ground effect around the caster that moves with them.

    private final int duration = data.getInt("duration", 8);
    private final double magicDamageBonus = data.getDouble("magicDamageBonus", 0.35);
    private final double attackRangeBonus = data.getDouble("attackRangeBonus", 1.0);
    private final double cleaveDamageMultiplier = data.getDouble("cleaveDamageMultiplier", 0.7);
    private final double cleaveRadius = data.getDouble("cleaveRadius", 3.0);
    private final double cleaveConeAngle = data.getDouble("cleaveConeAngle", 90.0);
    private final double cleaveConeRange = data.getDouble("cleaveConeRange", 4.0);
    private final int weaknessDurationMin = data.getInt("weaknessDurationMin", 4) * 20;
    private final int weaknessDurationMax = data.getInt("weaknessDurationMax", 20) * 20;
    private final int burningDurationMin = data.getInt("burningDurationMin", 4) * 20;
    private final int burningDurationMax = data.getInt("burningDurationMax", 20) * 20;
    private final int burningStacksMin = data.getInt("burningStacksMin", 1);
    private final int burningStacksMax = data.getInt("burningStacksMax", 3);
    private final double sizeIncrease = data.getDouble("sizeIncrease", 0.1);

    private final AttributeModifier bonusDamageModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:avatar_wrath"), magicDamageBonus, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    private final AttributeModifier sizeModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:avatar_wrath_size"), sizeIncrease, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    private final AttributeModifier attackRangeModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:avatar_wrath_range"), attackRangeBonus, AttributeModifier.Operation.ADD_NUMBER);
    private final EffectData weaknessEffect = Spellbook.getEffectData("Weakness");
    private final EffectData burningEffect = Spellbook.getEffectData("Burning");

    private AoE burningGroundAoE;
    private boolean cleanedUp;
    private TerrainRing avatarRing;

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

        burningGroundAoE = createCircularAoE(caster.getLocation(), 3, 1, keepAliveTicks)
                .followEntity(caster)
                .onEnter((aoe, entity) -> {
                    if (Spellbook.canAttack(caster, entity)) {
                        int burningDuration = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, burningDurationMin, burningDurationMax, "burningDuration");
                        int burningStacks = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, burningStacksMin, burningStacksMax, "burningStacks");
                        entity.addEffect(entity, burningEffect, burningDuration, burningStacks);
                    }
                })
                .onTick(aoe -> {
                    // The moving solar sigil carries the persistent area telegraph.
                });

        if (SkillVfx.availableTo(caster)) avatarRing = new TerrainRing(caster.getLocation(), 2.8,
                "terrain_band_hierophant", 40, 0.7f);

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);

        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (avatarRing != null && currentTicks % 3 == 0) avatarRing.move(caster.getLocation());
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_MAGICAL);

        double finalDamage = damage;
        createConeAoE(caster.getLocation(), cleaveConeRange, cleaveConeAngle, 2,
                target.getLocation().subtract(caster.getLocation()).toVector(), 40)
                .onEnter((aoe, entity) -> {
                    if (Spellbook.canAttack(caster, entity) && entity != target) {
                        double cleaveDamage = finalDamage * cleaveDamageMultiplier;
                        entity.damage(cleaveDamage, caster, PDamageType.MAGIC);

                        int weaknessDuration = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration");
                        entity.addEffect(entity, weaknessEffect, weaknessDuration, 1);

                        int burningDuration = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, burningDurationMin, burningDurationMax, "burningDuration");
                        int burningStacks = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, burningStacksMin, burningStacksMax, "burningStacks");
                        entity.addEffect(entity, burningEffect, burningDuration, burningStacks);
                    }
                });

        new BukkitRunnable() {
            @Override
            public void run() {
                caster.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0),
                        SkillVfx.availableTo(caster) ? 0 : 1);
                caster.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 3L);

        return super.onAttack(target, damage, type);
    }

    @Override
    public boolean onCast(SpellbookSpell spell) {
        return false;
    }

    @Override
    protected void onTickFinish() {
        cleanup();
    }

    @Override
    protected void cleanup() {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        if (burningGroundAoE != null) {
            burningGroundAoE.end();
            burningGroundAoE = null;
        }
        if (avatarRing != null) avatarRing.close();
        caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).removeModifier(bonusDamageModifier);
        caster.getAttribute(Attribute.SCALE).removeModifier(sizeModifier);
        caster.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).removeModifier(attackRangeModifier);
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration") / 20, VALUE_COLOR));
        placeholderNames.add("weaknessDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, burningDurationMin, burningDurationMax, "burningDuration") / 20, VALUE_COLOR));
        placeholderNames.add("burningDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, burningStacksMin, burningStacksMax, "burningStacks"), VALUE_COLOR));
        placeholderNames.add("burningStacks");
    }
}

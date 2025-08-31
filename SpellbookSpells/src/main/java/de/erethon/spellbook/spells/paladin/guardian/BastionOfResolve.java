package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class BastionOfResolve extends PaladinBaseSpell {

    // The Guardian creates a protective bastion that follows them, granting nearby allies increased resistance, stability and power.
    // The bastion creates pillars of light that strengthen allies and weaken enemies who dare approach.

    private final int range = data.getInt("range", 8);
    private final int powerDurationMin = data.getInt("powerDurationMin", 8) * 20;
    private final int powerDurationMax = data.getInt("powerDurationMax", 20) * 20;
    private final int resistanceDurationMin = data.getInt("resistanceDurationMin", 8) * 20;
    private final int resistanceDurationMax = data.getInt("resistanceDurationMax", 20) * 20;
    private final int stabilityDurationMin = data.getInt("stabilityDurationMin", 8) * 20;
    private final int stabilityDurationMax = data.getInt("stabilityDurationMax", 20) * 20;
    private final int bastionDuration = data.getInt("bastionDuration", 200);
    private final double weaknessDamage = data.getDouble("weaknessDamage", 5.0);

    private final EffectData resistanceEffect = Spellbook.getEffectData("Resistance");
    private final EffectData powerEffect = Spellbook.getEffectData("Power");
    private final EffectData stabilityEffect = Spellbook.getEffectData("Stability");
    private final EffectData weaknessEffect = Spellbook.getEffectData("Weakness");

    private AoE bastionAura;
    private int applyTicks = 20;

    public BastionOfResolve(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.RECORDS, 1.0f, 0.5f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 0.8f, 0.7f);

        bastionAura = createCircularAoE(caster.getLocation(), range, 1, bastionDuration)
                .followEntity(caster)
                .onTick(aoe -> {
                    if (caster.getTicksLived() % 15 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            aoe.getCenter().add(0, 2, 0),
                            12, range * 0.6, 1.5, range * 0.6, 0.02);
                    }
                    if (caster.getTicksLived() % 25 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.END_ROD,
                            aoe.getCenter().add(0, 0.5, 0),
                            8, range * 0.7, 0.8, range * 0.7, 0.03);
                    }
                })
                .onEnter((aoe, entity) -> {
                    if (entity instanceof LivingEntity t) {
                        if (Spellbook.canAttack(caster, t)) {
                            t.addEffect(caster, weaknessEffect, 60, 1);
                            t.damage(weaknessDamage, caster);
                            t.getWorld().spawnParticle(Particle.WITCH,
                                t.getLocation().add(0, 1, 0),
                                5, 0.5, 1, 0.5, 0.02);
                        }
                    }
                })
                .addBlockChange(Material.BLUE_STAINED_GLASS, Material.CYAN_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS)
                .sendBlockChanges();

        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        applyTicks--;
        if (applyTicks <= 0) {
            applyTicks = 20;

            for (LivingEntity ally : caster.getLocation().getNearbyLivingEntities(range)) {
                if (Spellbook.canAttack(caster, ally)) continue;

                if (!ally.hasEffect(resistanceEffect)) {
                    int duration = (int) Spellbook.getRangedValue(data, caster, ally, Attribute.RESISTANCE_MAGICAL, resistanceDurationMin, resistanceDurationMax, "resistanceDuration") + (caster.getEnergy() / 2);
                    ally.addEffect(caster, resistanceEffect, duration, 1);

                    ally.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                        ally.getLocation().add(0, 1.5, 0),
                        3, 0.3, 0.5, 0.3, 0.02);
                }

                if (!ally.hasEffect(powerEffect)) {
                    int duration = (int) Spellbook.getRangedValue(data, caster, ally, Attribute.ADVANTAGE_MAGICAL, powerDurationMin, powerDurationMax, "powerDuration") + (caster.getEnergy() / 2);
                    ally.addEffect(caster, powerEffect, duration, 1);
                }

                if (!ally.hasEffect(stabilityEffect)) {
                    int duration = (int) Spellbook.getRangedValue(data, caster, ally, Attribute.RESISTANCE_MAGICAL, stabilityDurationMin, stabilityDurationMax, "stabilityDuration") + (caster.getEnergy() / 2);
                    ally.addEffect(caster, stabilityEffect, duration, 1);
                }
            }

            if (caster.getTicksLived() % 60 == 0) {
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_AMBIENT, SoundCategory.RECORDS, 0.6f, 1.2f);
            }
        }
    }

    @Override
    protected void cleanup() {
        if (bastionAura != null) {
            bastionAura.revertBlockChanges();
        }
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, resistanceDurationMin, resistanceDurationMax, "resistanceDuration") / 20, VALUE_COLOR));
        placeholderNames.add("resistanceDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, powerDurationMin, powerDurationMax, "powerDuration") / 20, VALUE_COLOR));
        placeholderNames.add("powerDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, stabilityDurationMin, stabilityDurationMax, "stabilityDuration") / 20, VALUE_COLOR));
        placeholderNames.add("stabilityDuration");
    }
}

package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class BastionOfResolve extends PaladinBaseSpell {

    // The Guardian creates a protective barrier, granting nearby allies increased resistance, stability and power.

    private final int range = data.getInt("range", 10);
    private final int powerDurationMin = data.getInt("powerDurationMin", 6) * 20;
    private final int powerDurationMax = data.getInt("powerDurationMax", 24) * 20;
    private final int resistanceDurationMin = data.getInt("resistanceDurationMin", 6) * 20;
    private final int resistanceDurationMax = data.getInt("resistanceDurationMax", 24) * 20;
    private final int stabilityDurationMin = data.getInt("stabilityDurationMin", 6) * 20;
    private final int stabilityDurationMax = data.getInt("stabilityDurationMax", 24) * 20;

    private final EffectData resistanceEffect = Spellbook.getEffectData("Resistance");
    private final EffectData powerEffect = Spellbook.getEffectData("Power");
    private final EffectData stabilityEffect = Spellbook.getEffectData("Stability");

    private int applyTicks = 20;

    public BastionOfResolve(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data); // 40
    }

    @Override
    public boolean onCast() {
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        applyTicks--;
        if (applyTicks <= 0) {
            applyTicks = 20;
            for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
                if (!Spellbook.canAttack(caster, living)) {
                    if (!living.hasEffect(resistanceEffect)) {
                        living.addEffect(living, resistanceEffect, (int) Spellbook.getRangedValue(data, caster, living, Attribute.RESISTANCE_MAGICAL, resistanceDurationMin, resistanceDurationMax, "resistanceDuration"), 1);
                    }
                    if (!living.hasEffect(powerEffect)) {
                        living.addEffect(living, powerEffect, (int) Spellbook.getRangedValue(data, caster, living, Attribute.ADVANTAGE_MAGICAL, powerDurationMin, powerDurationMax, "powerDuration"), 1);
                    }
                    if (!living.hasEffect(stabilityEffect)) {
                        living.addEffect(living, stabilityEffect, (int) Spellbook.getRangedValue(data, caster, living, Attribute.RESISTANCE_MAGICAL, stabilityDurationMin, stabilityDurationMax, "stabilityDuration"), 1);
                    }
                }
            }
        }
        CircleEffect circleEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circleEffect.particle = org.bukkit.Particle.SOUL_FIRE_FLAME;
        circleEffect.particleCount = 32;
        circleEffect.setEntity(caster);
        circleEffect.radius = range;
        circleEffect.duration = duration * 20;
        circleEffect.iterations = -1;
        circleEffect.period = 20;
        circleEffect.wholeCircle = true;
        circleEffect.enableRotation = false;
        circleEffect.start();
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

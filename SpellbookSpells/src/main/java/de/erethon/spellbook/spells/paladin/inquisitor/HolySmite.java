package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class HolySmite extends InquisitorBaseSpell {

    // Deliver a powerful overhead blow to a single target. Deals moderate physical damage.
    // Consumes all 'Judgement' stacks on the target for significantly increased magic damage per stack.
    // Grants Fury per stack consumed.

    private final int range = data.getInt("range", 10);
    private final int bonusDamagePerStack = data.getInt("bonusDamagePerStack", 25);
    private final int furyStacksPerStack = data.getInt("furyStacksPerStack", 1);
    private final int furyDurationMin = data.getInt("furyDurationMin", 5);
    private final int furyDurationMax = data.getInt("furyDurationMax", 15);

    private final EffectManager effectManager = Spellbook.getInstance().getEffectManager();
    private final EffectData fury = Spellbook.getEffectData("Fury");

    public HolySmite(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        LineEffect lineEffect = new LineEffect(effectManager);
        lineEffect.particle = Particle.WHITE_ASH;
        lineEffect.particleCount = 32;
        lineEffect.setEntity(caster);
        lineEffect.setTargetEntity(target);
        lineEffect.duration = 30;
        lineEffect.isZigZag = true;
        lineEffect.start();
        caster.getWorld().playSound(target, Sound.BLOCK_BELL_RESONATE, 1, 0.5f);
        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        double magicalDamage = 0;
        int judgementStacks = getJudgementStacksOnTarget(target);
        for (int i = 0; i <= judgementStacks; i++) {
            magicalDamage += bonusDamagePerStack;
            removeJudgement(target);
        }
        target.damage(physicalDamage, caster, PDamageType.PHYSICAL);
        target.damage(magicalDamage, caster, PDamageType.MAGIC);
        triggerTraits(1);
        if (judgementStacks > 0) {
            int furyDuration = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, furyDurationMin, furyDurationMax, "furyDuration") * 20;
            caster.addEffect(caster, fury, furyDuration, judgementStacks * furyStacksPerStack);
            target.getWorld().playSound(target, Sound.BLOCK_CONDUIT_ACTIVATE, 1, 0.5f);
            triggerTraits(2);
        }
        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, furyDurationMin, furyDurationMax, "furyDuration"), VALUE_COLOR));
        placeholderNames.add("furyDuration");
    }
}

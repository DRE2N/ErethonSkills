package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class PreciseStab extends AssassinBaseSpell {

    // Perform a quick, piercing stab on the target. Deals moderate physical damage.
    // If the target has 3 or more Bleed stacks, this attack consumes the bleeding to grant you Fury.

    private final int range = data.getInt("range", 3);
    private final int bleedStacksRequired = data.getInt("bleedStacksRequired", 3);
    private final int furyMinDuration = data.getInt("furyMinDuration", 6) * 20;
    private final int furyMaxDuration = data.getInt("furyMaxDuration", 12) * 20;

    private final EffectData bleedingEffectData = Spellbook.getEffectData("Bleeding");
    private final EffectData furyEffectData = Spellbook.getEffectData("Fury");

    public PreciseStab(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        int bleedingOnTarget = 0;
        SpellEffect bleedingEffect = null;
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == bleedingEffectData) {
                bleedingOnTarget += effect.getStacks();
                bleedingEffect = effect;
            }
        }
        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        target.damage(physicalDamage, caster, PDamageType.PHYSICAL);
        if (bleedingOnTarget >= bleedStacksRequired && bleedingEffect != null) {
            int stacks = bleedingOnTarget - bleedStacksRequired;
            if (stacks <= 0) {
                target.removeEffect(bleedingEffectData);
            } else {
                bleedingEffect.setStacks(bleedingOnTarget - bleedStacksRequired);
            }
            caster.addEffect(caster, furyEffectData, (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, furyMinDuration, furyMaxDuration, "furyDuration"), 1);
            caster.getWorld().playSound(target, Sound.BLOCK_CONDUIT_ATTACK_TARGET, 0.6f, 1.0f);
            caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation(), 3, 0.5, 0.5, 0.5);
        }
        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, furyMinDuration, furyMaxDuration, "furyDuration") / 20, VALUE_COLOR));
        placeholderNames.add("furyDuration");
    }
}

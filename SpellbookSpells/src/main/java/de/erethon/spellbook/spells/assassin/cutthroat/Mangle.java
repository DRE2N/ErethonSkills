package de.erethon.spellbook.spells.assassin.cutthroat;

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

public class Mangle extends AssassinBaseSpell {

    // Powerful attack that silences and weakens a target if they are bleeding. Additionally, physical damage.

    private final int range = data.getInt("range", 3);
    private final int bleedStacksRequired = data.getInt("bleedStacksRequired", 3);
    private final int silenceMinDuration = data.getInt("silenceMinDuration", 8) * 20;
    private final int silenceMaxDuration = data.getInt("silenceMaxDuration", 16) * 20;
    private final int weaknessMinDuration = data.getInt("weaknessMinDuration", 10) * 20;
    private final int weaknessMaxDuration = data.getInt("weaknessMaxDuration", 25) * 20;

    private final EffectData bleedingEffectData = Spellbook.getEffectData("Bleeding");
    private final EffectData silenceEffectData = Spellbook.getEffectData("Silence");
    private final EffectData weaknessEffectData = Spellbook.getEffectData("Weakness");

    public Mangle(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        lookForTarget(range);
        int bleedingOnTarget = 0;
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == bleedingEffectData) {
                bleedingOnTarget += effect.getStacks();
            }
        }
        if (bleedingOnTarget < bleedStacksRequired) {
            caster.sendParsedActionBar("<color:#ff0000>Das Ziel blutet nicht genug!");
            return false;
        }
        return super.onPrecast();
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
        if (bleedingOnTarget >= bleedStacksRequired && bleedingEffect != null) {
            int stacks = bleedingOnTarget - bleedStacksRequired;
            if (stacks <= 0) {
                target.removeEffect(bleedingEffectData);
            } else {
                bleedingEffect.setStacks(bleedingOnTarget - bleedStacksRequired);
            }
            caster.addEffect(caster, silenceEffectData, (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, silenceMinDuration, silenceMaxDuration, "silenceDuration"), 1);
            target.addEffect(caster, weaknessEffectData, (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, weaknessMinDuration, weaknessMaxDuration, "weaknessDuration"), 1);
            target.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL), caster);
            caster.getWorld().playSound(target, Sound.BLOCK_BAMBOO_BREAK, 1.0f, 1.2f);
            caster.getWorld().spawnParticle(Particle.ITEM_SLIME, target.getLocation(), 3, 0.5, 0.5, 0.5);
        }
        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, silenceMinDuration, silenceMaxDuration, "silenceDuration") / 20, VALUE_COLOR));
        placeholderNames.add("silenceDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, weaknessMinDuration, weaknessMaxDuration, "weaknessDuration") / 20, VALUE_COLOR));
        placeholderNames.add("weaknessDuration");
        super.addSpellPlaceholders();
    }
}

package de.erethon.spellbook.traits.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InquisitorBasicAttack extends SpellTrait {

    // The Inquisitor's basic attack applies a "Judgement" effect to the target, stacking up to a maximum number of stacks.

    private final int maximumJudgementStacks = data.getInt("maximumJudgementStacks", 7);
    private final int soundCooldown = data.getInt("soundCooldown", 5000); // milliseconds

    protected final EffectData judgementData = Spellbook.getEffectData("Judgement");
    private final Map<UUID, Long> soundCooldowns = new HashMap<>();

    public InquisitorBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
        if (judgementData == null) {
            throw new IllegalArgumentException("Judgement effect not found. Make sure it is defined in the Spellbook configs.");
        }
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        int judgementStacks = getJudgementStacksOnTarget(target);
        if (judgementStacks < maximumJudgementStacks) {
            target.addEffect(caster, judgementData, 120, 1);
        }
        if (judgementStacks == maximumJudgementStacks) {
            long now = System.currentTimeMillis();
            long lastPlayed = soundCooldowns.getOrDefault(target.getUniqueId(), 0L);
            if (now - lastPlayed >= soundCooldown) {
                target.getWorld().playSound(target, Sound.BLOCK_CONDUIT_ACTIVATE, 0.7f, 0.5f);
                soundCooldowns.put(target.getUniqueId(), now);
            }
        }
        return super.onAttack(target, damage, type);
    }

    protected int getJudgementStacksOnTarget(LivingEntity target) {
        if (target == null) return 0;
        SpellEffect judgementEffect = null;
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == null) continue;
            if (effect.data.equals(judgementData)) {
                judgementEffect = effect;
                break;
            }
        }
        if (judgementEffect == null) return 0;
        return judgementEffect.getStacks();
    }
}

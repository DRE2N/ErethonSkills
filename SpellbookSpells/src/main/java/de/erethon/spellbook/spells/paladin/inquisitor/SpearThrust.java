package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinSpearSpell;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.LivingEntity;

public class SpearThrust extends InquisitorBaseSpell {

    // Pushes the target away and applies weakness as well as judgement.
    // If the target has more than 3 stacks of judgement, they are additionally slowed and burnt.

    private final double velocity = data.getDouble("velocity", 1.5);
    private final int weaknessDuration = data.getInt("weaknessDuration", 120);
    private final int weaknessStacks = data.getInt("weaknessStacks", 1);
    private final int slowDuration = data.getInt("slowDuration", 120);
    private final int burnDuration = data.getInt("burnDuration", 120);
    private final int burnStacks = data.getInt("burnStacks", 1);
    public int minimumJudgementStacks = data.getInt("minimumJudgementStacks", 3); // Trait: Not yet

    private final EffectData weaknessEffect = Spellbook.getEffectData("Weakness");
    private final EffectData burnEffect = Spellbook.getEffectData("Burning");
    private final EffectData slowEffect = Spellbook.getEffectData("Slow");

    public SpearThrust(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(3);
    }

    @Override
    public boolean onCast() {
        target.setVelocity(caster.getLocation().getDirection().multiply(velocity));
        target.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.RECORD, 0.8f, 1));
        target.addEffect(caster, weaknessEffect, weaknessDuration, weaknessStacks);
        addJudgement(target);
        if (getJudgementStacksOnTarget(target) > minimumJudgementStacks) {
            target.addEffect(caster, slowEffect, slowDuration, 1);
            target.addEffect(caster, burnEffect, burnDuration, burnStacks);
            target.playSound(Sound.sound(org.bukkit.Sound.ENTITY_BLAZE_SHOOT, Sound.Source.RECORD, 1, 1));
        }
        triggerTraits(target);
        return super.onCast();
    }
}

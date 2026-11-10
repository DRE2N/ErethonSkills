package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** A committed spear interrupt. Judgement strengthens one control hit and is spent on contact. */
public class PurgingStrike extends InquisitorBaseSpell {

    private final int range = data.getInt("range", 3);
    private final EffectData stun = Spellbook.getEffectData("Stun");

    public PurgingStrike(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        if (target == null || !target.isValid() || !Spellbook.canAttack(caster, target)
                || !caster.getWorld().equals(target.getWorld())
                || !caster.hasLineOfSight(target)
                || caster.getLocation().distanceSquared(target.getLocation()) > range * range) {
            return true;
        }
        int stacks = Math.min(3, getJudgementStacksOnTarget(target));
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true,
                Attribute.ADVANTAGE_PHYSICAL);
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) return true;
        if (stacks > 0) removeJudgement(target);

        // Bosses use the Papyrus/Aether bar. Players get only a brief interrupt.
        if (!BossControl.apply(caster, target, 35 + 7 * stacks) && stun != null) {
            target.addEffect(caster, stun, target instanceof Player ? 4 : 10, 1);
        }
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data != null && effect.data.isPositive()) {
                target.removeEffect(effect.data);
                break;
            }
        }
        SkillSignatureVfx.accent(target.getLocation().add(0, 1.15, 0), getClass(),
                stacks > 0 ? "main" : "wake", 2.5f, 14, false);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE,
                0.9f, stacks > 0 ? 0.75f : 1.1f);
        return true;
    }
}

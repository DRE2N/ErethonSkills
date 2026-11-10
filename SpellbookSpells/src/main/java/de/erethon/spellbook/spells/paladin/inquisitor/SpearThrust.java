package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/** Short spear contact that starts Judgement without scattering the target. */
public class SpearThrust extends InquisitorBaseSpell {

    private final double range = data.getDouble("range", 3.5);

    public SpearThrust(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget((int) Math.ceil(range));
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        if (target == null || !target.isValid() || !Spellbook.canAttack(caster, target)
                || target.getWorld() != caster.getWorld() || !caster.hasLineOfSight(target)
                || caster.getLocation().distanceSquared(target.getLocation()) > range * range) return true;
        int stacksBefore = getJudgementStacksOnTarget(target);
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true,
                Attribute.ADVANTAGE_PHYSICAL);
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) return true;
        addJudgement(target);
        BossControl.apply(caster, target, 25);
        if (stacksBefore >= 2) {
            target.addEffect(caster, Spellbook.getEffectData("Weakness"), 40, 1);
        }
        Vector push = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0);
        if (push.lengthSquared() > 0.01) target.setVelocity(target.getVelocity().add(push.normalize().multiply(0.14)));
        SkillSignatureVfx.accent(target.getLocation().add(0, 1.1, 0), getClass(),
                "wake", 2.2f, 11, false);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.1f);
        return true;
    }
}

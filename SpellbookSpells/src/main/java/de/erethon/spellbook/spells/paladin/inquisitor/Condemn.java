package de.erethon.spellbook.spells.paladin.inquisitor;


import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;

public class Condemn extends InquisitorBaseSpell {

    // RMB. Slam down the spear, dealing damage and weakness to all enemies in a cone in front of you. Consumes all judgements on enemies and
    // applies burning for each stack of judgement consumed. Heals allies in a small radius per judgement consumed.

    private final int burningStacksPerJudgement = data.getInt("burningStacksPerJudgement", 1);
    private final int burningDuration = data.getInt("burningDuration", 40);
    private final int weaknessDuration = data.getInt("weaknessDuration", 120);
    private final int weaknessStacks = data.getInt("weaknessStacks", 1);
    private final int healRadius = data.getInt("healRadius", 3);
    private final int healingPerJudgement = data.getInt("healingPerJudgement", 20);

    private final EffectData burning = Spellbook.getEffectData("Burning");
    private final EffectData weakness = Spellbook.getEffectData("Weakness");

    public Condemn(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        BoundingBox cone = new BoundingBox(caster.getLocation().getX(), caster.getLocation().getY(), caster.getLocation().getZ(),
                caster.getLocation().getX() + 5, caster.getLocation().getY() + 5, caster.getLocation().getZ() + 5);
        double totalJudgement = 0;
        for (Entity entity : caster.getWorld().getNearbyEntities(cone)) {
            if (entity instanceof LivingEntity living && Spellbook.canAttack(caster, living)) {
                int judgementStacks = getJudgementStacksOnTarget(living);
                totalJudgement += judgementStacks;
                int stacksToApply = judgementStacks * burningStacksPerJudgement;
                living.addEffect(caster, burning, burningDuration, stacksToApply);
                living.addEffect(caster, weakness, weaknessDuration, weaknessStacks);
                removeJudgement(living);
            }
        }
        if (totalJudgement > 0) {
            caster.getWorld().playSound(caster, Sound.BLOCK_AMETHYST_BLOCK_STEP, 1, 1);
            caster.getWorld().spawnParticle(Particle.FLAME, caster.getLocation(), 10, 2, 0.5, 2);
            caster.getWorld().spawnParticle(Particle.WITCH, caster.getLocation(), 10, 2, 0.5, 2);
            for (LivingEntity living : caster.getWorld().getNearbyLivingEntities(caster.getLocation(), healRadius)) {
                if (living != caster && Spellbook.canAttack(caster, living)) {
                    double healAmount = totalJudgement * healingPerJudgement;
                    living.heal(healAmount);
                    living.getWorld().spawnParticle(Particle.HEART, living.getLocation(), 4, 0.5, 0.5, 0.5);
                    living.getWorld().playSound(living, Sound.BLOCK_BEEHIVE_DRIP, 1, 1);
                }
            }
            caster.heal(totalJudgement * healingPerJudgement);
            caster.getWorld().spawnParticle(Particle.HEART, caster.getLocation(), 4, 0.5, 0.5, 0.5);
            caster.getWorld().playSound(caster, Sound.BLOCK_BEEHIVE_DRIP, 1, 1);
        }
        return super.onCast();
    }
}

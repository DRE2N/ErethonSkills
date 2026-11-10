package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ItemDisplay;

import java.util.Comparator;

/** A fixed, announced sentence. Three marks make it frightening; moving or cover denies it. */
public class HolySmite extends InquisitorBaseSpell {

    private final int range = data.getInt("range", 10);
    private final double impactRadius = data.getDouble("impactRadius", 1.1);
    private final double bonusPerStack = data.getDouble("bonusPerStack", 0.22);

    public HolySmite(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        Location point = target.getLocation().clone();
        if (!super.onCast()) return false;
        TerrainRing ring = new TerrainRing(point, impactRadius,
                "terrain_band_inquisitor", 24, 0.35f);
        SkillSignatureVfx.accent(point.clone().add(0, 2.6, 0), getClass(), "lead", 3.4f, 8, false);
        point.getWorld().playSound(point, Sound.BLOCK_BELL_USE, 0.95f, 0.75f);
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), () -> {
            ring.close();
            if (!caster.isValid() || caster.isDead() || caster.getWorld() != point.getWorld()) return;
            SkillSignatureVfx.accent(point, getClass(), "main", 3.4f, 18, true);
            var enemy = point.getWorld().getNearbyLivingEntities(point, impactRadius, 2, impactRadius)
                    .stream()
                    .filter(entity -> Spellbook.canAttack(caster, entity)
                            && entity.getLocation().distanceSquared(point) <= impactRadius * impactRadius
                            && caster.hasLineOfSight(entity))
                    .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(point)))
                    .orElse(null);
            if (enemy == null) return;
            int stacks = Math.min(3, getJudgementStacksOnTarget(enemy));
            double base = Spellbook.getVariedAttributeBasedDamage(data, caster, enemy, true,
                    Attribute.ADVANTAGE_MAGICAL);
            double damage = base * (1 + bonusPerStack * stacks);
            double before = enemy.getHealth() + enemy.getAbsorptionAmount();
            CombatSourceContext.runSpell(this, () -> enemy.damage(damage, caster, PDamageType.MAGIC));
            if (enemy.getHealth() + enemy.getAbsorptionAmount() >= before) return;
            if (stacks > 0) removeJudgement(enemy);
            BossControl.apply(caster, enemy, stacks > 0 ? 15 : 5);
            enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
                    1.0f, stacks > 0 ? 0.75f : 1.1f);
            SkillSignatureVfx.accent(enemy.getLocation().add(0, 1.1, 0), getClass(),
                    "wake", 2.4f, 17, false);
            triggerTraits(enemy);
        }, 8L);
        return true;
    }
}

package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.Comparator;
import java.util.HashSet;

/** A placed, announced area verdict. Marked targets take the spectacular hit. */
public class JudgementOfGod extends InquisitorBaseSpell {

    private final int range = data.getInt("range", 16);
    public double radius = data.getDouble("radius", 4.5);
    public double markBonusPerStack = 0.18;
    private final int warmupTicks = data.getInt("warmupTicks", 20);
    private Location center;
    private boolean resolved;
    private TerrainRing boundary;

    public JudgementOfGod(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = warmupTicks + 2;
    }

    @Override
    protected boolean onPrecast() {
        if (!super.onPrecast() || !lookForTarget(range)) return false;
        center = target.getLocation().clone();
        return true;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        boundary = new TerrainRing(center, radius,
                "terrain_band_inquisitor", 44, 0.45f);
        SkillSignatureVfx.accent(center, getClass(), "lead", (float) (radius * 1.8),
                warmupTicks, true);
        center.getWorld().playSound(center, Sound.BLOCK_BELL_USE, 1.1f, 0.65f);
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (resolved) return;
        if (currentTicks % 5 == 0 && currentTicks < warmupTicks) {
            center.getWorld().playSound(center, Sound.BLOCK_BELL_USE, 0.4f,
                    0.7f + currentTicks / (float) warmupTicks);
        }
        if (currentTicks >= warmupTicks) {
            resolved = true;
            strike();
        }
    }

    private void strike() {
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.4f, 0.75f);
        SkillSignatureVfx.accent(center, getClass(), "main", (float) (radius * 2), 24, true);
        SkillSignatureVfx.accent(center.clone().add(0, 2, 0), getClass(), "wake", 6.0f, 24, false);
        var enemies = center.getWorld().getNearbyLivingEntities(center, radius, 3, radius).stream()
                .filter(entity -> Spellbook.canAttack(caster, entity)
                        && entity.getLocation().distanceSquared(center) <= radius * radius
                        && caster.hasLineOfSight(entity))
                .sorted(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)))
                .limit(8).toList();
        for (LivingEntity enemy : enemies) {
            int stacks = Math.min(3, getJudgementStacksOnTarget(enemy));
            double base = Spellbook.getVariedAttributeBasedDamage(data, caster, enemy, true,
                    Attribute.ADVANTAGE_MAGICAL);
            double damage = base * (1 + markBonusPerStack * stacks);
            double before = enemy.getHealth() + enemy.getAbsorptionAmount();
            CombatSourceContext.runSpell(this, () -> enemy.damage(damage, caster, PDamageType.MAGIC));
            if (enemy.getHealth() + enemy.getAbsorptionAmount() >= before) continue;
            if (stacks > 0) removeJudgement(enemy);
            BossControl.apply(caster, enemy, stacks > 0 ? 35 : 15);
            SkillSignatureVfx.accent(enemy.getLocation().add(0, 1.2, 0), getClass(),
                    "wake", 2.5f, 18, false);
        }
        if (!enemies.isEmpty()) triggerTraits(new HashSet<>(enemies));
    }

    @Override
    protected void cleanup() {
        if (boundary != null) boundary.close();
        super.cleanup();
    }
}

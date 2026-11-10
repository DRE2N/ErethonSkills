package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;

import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;

import java.util.ArrayList;
import java.util.List;

/** A shown radial pack burst. Spending Wrath empowers the impact. */
public class WrathfulBurst extends HierophantBaseSpell {

    private final double radius = data.getDouble("radius", 4);
    private final int empoweredCost = data.getInt("empoweredCost", 40);
    private final double empoweredMultiplier = data.getDouble("burstDamageMultiplier", 1.35);

    public WrathfulBurst(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        boolean empowered = caster.getEnergy() >= empoweredCost;
        if (!super.onCast()) return false;
        if (empowered) caster.setEnergy(Math.max(0, caster.getEnergy() - empoweredCost));
        Location center = caster.getLocation().clone();
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.75f, 0.8f);
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), () -> {
            if (!caster.isValid() || caster.isDead() || caster.getWorld() != center.getWorld()) return;
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f,
                    empowered ? 0.7f : 1.1f);
            center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 1, 0), 1, Color.WHITE);
            for (LivingEntity enemy : center.getWorld().getNearbyLivingEntities(center, radius, 2.5, radius)) {
                if (!Spellbook.canAttack(caster, enemy)
                        || enemy.getLocation().distanceSquared(center) > radius * radius
                        || !caster.hasLineOfSight(enemy)) continue;
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, enemy, true,
                        Attribute.ADVANTAGE_MAGICAL) * (empowered ? empoweredMultiplier : 1.0);
                double before = enemy.getHealth() + enemy.getAbsorptionAmount();
                CombatSourceContext.runSpell(this, () -> enemy.damage(damage, caster, PDamageType.MAGIC));
                if (enemy.getHealth() + enemy.getAbsorptionAmount() >= before) continue;
                BossControl.apply(caster, enemy, empowered ? 25 : 15);
                if (empowered) enemy.addEffect(caster, Spellbook.getEffectData("Burning"), 60, 1);
            }
        }, 8L);
        return true;
    }
}

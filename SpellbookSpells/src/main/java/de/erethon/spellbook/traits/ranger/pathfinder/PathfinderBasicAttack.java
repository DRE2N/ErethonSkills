package de.erethon.spellbook.traits.ranger.pathfinder;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class PathfinderBasicAttack extends SpellTrait {

    // Grants a chance to evade damage after a certain number of hits. The evasion lasts for a certain duration.

    private final double evasionChance = data.getDouble("evasionChance", 0.1);
    private final double hitsForEvasion = data.getInt("hitsForEvasion", 3);
    private final double evasionDuration = data.getInt("evasionDuration", 200);

    private int hits = 0;
    private long evasionAddedTick = 0;
    private int flowTick = 0;

    public PathfinderBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (hits >= hitsForEvasion) {
            hits = 0;
            caster.getTags().add("ranger.pathfinder.evasion");
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 2);
            caster.getWorld().spawnParticle(Particle.POOF, caster.getLocation(), 2, 0.5, 0.5, 0.5);
            evasionAddedTick = Bukkit.getCurrentTick();
        } else {
            hits++;
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (evasionAddedTick != 0 && Bukkit.getCurrentTick() > evasionAddedTick + evasionDuration) {
            caster.getTags().remove("ranger.pathfinder.evasion");
        }
        flowTick++;
        if (flowTick >= 20) {
            flowTick = 0;
            if (caster.getTags().contains("spellbook.ranger.flow")) {
                caster.getLocation().getWorld().spawnParticle(Particle.TINTED_LEAVES, caster.getLocation(), 5, 0.5, 0.5, 0.5);
            }
        }
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (evasionAddedTick != 0 && attacker.getTags().contains("ranger.pathfinder.evasion")) {
            if (Math.random() < evasionChance) {
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.6f, 2);
                caster.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, caster.getLocation(), 2, 0.5, 0.5, 0.5);
                return 0;
            }
        }
        return super.onDamage(attacker, damage, type);
    }
}

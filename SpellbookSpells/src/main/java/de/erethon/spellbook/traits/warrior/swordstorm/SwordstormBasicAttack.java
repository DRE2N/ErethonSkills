package de.erethon.spellbook.traits.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SwordstormBasicAttack extends SpellTrait {

    // The Swordstorm gains rage with every attack, and after a certain number of consecutive attacks, it gains a Fury effect that increases attack speed.
    // If the caster is in the air, the damage from an attack is increased.

    private final int ragePerAttack = data.getInt("ragePerAttack", 1);
    private final int consecutiveAttacksForBonus = data.getInt("consecutiveAttacksForBonus", 3);
    private final int maxTicksBetweenAttacks = data.getInt("maxTicksBetweenAttacks", 20);
    private final int furyDuration = data.getInt("furyDuration", 40);
    private final double airDamageMultiplier = data.getDouble("airDamageMultiplier", 1.1);
    private final EffectData fury = Spellbook.getEffectData("Fury");

    private int attackCounter = 0;
    private int lastAttackTick = 0;

    public SwordstormBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (lastAttackTick > 0 && Bukkit.getCurrentTick() - lastAttackTick > maxTicksBetweenAttacks) {
            attackCounter = 0;
        }
        attackCounter++;
        lastAttackTick = Bukkit.getCurrentTick();
        caster.setEnergy(caster.getEnergy() + ragePerAttack);
        if (attackCounter >= consecutiveAttacksForBonus) {
            attackCounter = 0;
            caster.addEffect(caster, fury, furyDuration, 1);
            World world = caster.getWorld();
            Location location = caster.getLocation();
            world.spawnParticle(Particle.GLOW, caster.getLocation(), 4, 0.5, 0.5, 0.5);
            world.playSound(location, Sound.BLOCK_LODESTONE_STEP, SoundCategory.RECORDS,0.5f, 1.0f);
        }
        if (caster instanceof Player player && !player.isOnGround()) { // Let's hope nobody is crazy enough to make an Erethon PvP cheat client.
            damage *= airDamageMultiplier;
        }
        return super.onAttack(target, damage, type);
    }
}

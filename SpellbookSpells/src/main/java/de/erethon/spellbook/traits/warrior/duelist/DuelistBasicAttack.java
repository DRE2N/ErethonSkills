package de.erethon.spellbook.traits.warrior.duelist;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;

public class DuelistBasicAttack extends SpellTrait {

    private static final Color DUELIST_PRIMARY = Color.fromRGB(58, 124, 201);
    private static final String PARRY_TAG = "duelist.parry";

    private final int energyPerHit = data.getInt("energyPerHit", 10);
    private final int energyParryBonus = data.getInt("energyParryBonus", 30);
    private final int consecutiveAttacksForBonus = data.getInt("consecutiveAttacksForBonus", 3);
    private final int maxTicksBetweenAttacks = data.getInt("maxTicksBetweenAttacks", 30);
    private final int furyDuration = data.getInt("furyDuration", 4) * 20;
    private final double parryDamageMultiplier = data.getDouble("parryDamageMultiplier", 1.6);
    private final EffectData fury = Spellbook.getEffectData("Fury");

    private int attackCounter = 0;
    private int lastAttackTick = 0;

    public DuelistBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        caster.setMaxEnergy(100);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (lastAttackTick > 0 && Bukkit.getCurrentTick() - lastAttackTick > maxTicksBetweenAttacks) {
            attackCounter = 0;
        }
        attackCounter++;
        lastAttackTick = Bukkit.getCurrentTick();
        caster.addEnergy(energyPerHit);

        if (caster.getTags().contains(PARRY_TAG)) {
            caster.getTags().remove(PARRY_TAG);
            caster.addEnergy(energyParryBonus);
            damage *= parryDamageMultiplier;
            playRiposteEffect(target);
        }

        if (attackCounter >= consecutiveAttacksForBonus) {
            attackCounter = 0;
            caster.addEffect(caster, fury, furyDuration, 1);
            playEnGardeEffect();
        }

        return super.onAttack(target, damage, type);
    }

    private void playRiposteEffect(LivingEntity target) {
        Location loc = target.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 12, 0.3, 0.3, 0.3, 0.2);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.3, 0.3, 0.3, 0,
            new Particle.DustOptions(DUELIST_PRIMARY, 1.5f));
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.RECORDS, 1f, 1.2f);
    }

    private void playEnGardeEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.GLOW, loc, 4, 0.5, 0.5, 0.5, 0);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.4, 0.4, 0.4, 0,
            new Particle.DustOptions(DUELIST_PRIMARY, 1.2f));
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.RECORDS, 0.6f, 2.0f);
    }
}

package de.erethon.spellbook.traits.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class VanguardBasicAttack extends SpellTrait {

    private final double weakAttackThreshold = data.getDouble("weakAttackThreshold", 0.3);
    private final double weakAttackMultiplier = data.getDouble("weakAttackMultiplier", 0.5);
    private final int momentumPerAttack = data.getInt("momentumPerAttack", 10);
    private final int momentumForPerfectTiming = data.getInt("momentumForPerfectTiming", 25);
    private final int perfectTimingWindow = data.getInt("perfectTimingWindowTicks", 20);

    private long lastAttackTick = 0;

    public VanguardBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (caster instanceof Player player) {
            float cooldown = player.getAttackCooldown();
            if (cooldown < weakAttackThreshold) {
                damage *= weakAttackMultiplier;
            } else if (cooldown == 1.0) {
                long currentTick = Bukkit.getCurrentTick();
                if (currentTick - lastAttackTick <= perfectTimingWindow) {
                    caster.addEnergy(momentumForPerfectTiming);
                    playPerfectHitEffect();
                } else {
                    caster.addEnergy(momentumPerAttack);
                }
            } else {
                caster.addEnergy(momentumPerAttack);
            }
            lastAttackTick = Bukkit.getCurrentTick();
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    protected void onAdd() {
        caster.setMaxEnergy(100);
    }

    private void playPerfectHitEffect() {
        Location loc = caster.getLocation().add(0, 1.2, 0);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.3, 0.3, 0.3, 0,
            new Particle.DustOptions(Color.fromRGB(80, 200, 80), 1.2f));
        loc.getWorld().spawnParticle(Particle.GLOW, loc, 2, 0.2, 0.2, 0.2, 0);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.RECORDS, 0.5f, 2.0f);
    }
}

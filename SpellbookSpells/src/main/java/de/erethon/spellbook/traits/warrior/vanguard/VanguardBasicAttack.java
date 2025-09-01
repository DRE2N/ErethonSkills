package de.erethon.spellbook.traits.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class VanguardBasicAttack extends SpellTrait {

    // Timing your basic attacks grants Momentum, empowering your next ability. Attacking too quickly results in a weak attack.
    // Perfect timing (e.g. just when the attack cooldown is fully reset) grants extra Momentum.

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
}

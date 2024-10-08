package de.erethon.spellbook.traits.warrior;

import com.destroystokyo.paper.ParticleBuilder;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CylinderEffect;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SlicingAttack extends SpellTrait {

    private final EffectManager manager = Spellbook.getInstance().getEffectManager();
    private final int requiredAttacks = data.getInt("requiredAttacks", 3);
    private final int maxTimeBetweenAttacks = data.getInt("maxTimeBetweenAttacks", 5000);
    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.2);
    private int attacks = 0;
    private LivingEntity lastTarget;
    private long lastAttackTime;

    public SlicingAttack(TraitData data, LivingEntity caster) {
        super(data, caster);

    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (lastTarget == null) {
            lastTarget = target;
            attacks = 0;
        }
        if (target != lastTarget) {
            attacks = 0;
        }
        if (target == lastTarget && System.currentTimeMillis() - lastAttackTime < maxTimeBetweenAttacks) {
            attacks++;
            lastAttackTime = System.currentTimeMillis();
            if (attacks >= requiredAttacks) {
                damage *= 1.2;
                attacks = 0;
                new ParticleBuilder(Particle.DUST)
                        .data(new Particle.DustOptions(Color.RED, 5)).count(4)
                        .offset(0.5, 0.5, 0.5)
                        .receivers((Player) caster)
                        .location(target.getLocation())
                        .spawn();
                caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, Sound.Source.RECORD, 1, 0.5f));
            }
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    protected void onTick() {
        if (System.currentTimeMillis() - lastAttackTime > maxTimeBetweenAttacks) {
            attacks = 0;
            lastTarget = null;
        }
    }
}

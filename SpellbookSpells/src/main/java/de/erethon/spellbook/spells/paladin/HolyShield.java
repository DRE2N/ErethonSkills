package de.erethon.spellbook.spells.paladin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.SphereEffect;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class HolyShield extends PaladinBaseSpell {

    private final double shieldPercent = data.getDouble("shieldPercent", 0.1);

    private double shield = 0;
    private double shieldMax;
    private SphereEffect sphere;

    public HolyShield(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("keepAliveTicks", 1200);
    }

    @Override
    public boolean onCast() {
        shield = caster.getMaxHealth() * shieldPercent;
        shieldMax = shield;
        sphere = new SphereEffect(Spellbook.getInstance().getEffectManager());
        sphere.radius = 1.5f;
        sphere.particleCount = 32;
        sphere.duration = keepAliveTicks * 50;
        sphere.iterations = -1;
        sphere.setEntity(caster);
        sphere.particle = Particle.REDSTONE;
        sphere.particleSize = 1f;
        sphere.color = org.bukkit.Color.WHITE;
        sphere.start();
        return super.onCast();
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        shield -= damage;
        if (shield <= 0) {
            cleanup();
            keepAliveTicks = 0;
            sphere.cancel();
            return damage;
        }
        double proportion = shield / shieldMax;
        sphere.color = org.bukkit.Color.fromRGB((int) (255 * proportion), (int) (255 * proportion), (int) (255 * proportion)); // Make the shield darker as it gets weaker
        return Math.max(0, shield - super.onDamage(attacker, damage, type));
    }

    @Override
    protected void cleanup() {
        shield = 0;
        sphere.cancel();
    }
}

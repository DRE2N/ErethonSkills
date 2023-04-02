package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CylinderEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BerserkMode extends SpellbookSpell {

    private final int duration = data.getInt("duration", 5);
    private final int baseDamagePerTick = data.getInt("damagePerTick", 10);

    private final WorldBorder fakeBorder = Bukkit.getServer().createWorldBorder();

    private final EffectManager manager = Spellbook.getInstance().getEffectManager();

    public BerserkMode(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 20;
        keepAliveTicks = duration * 20;
        fakeBorder.setCenter(0, 0);
        fakeBorder.setSize(1);
        fakeBorder.setWarningDistance(100000);
        fakeBorder.setWarningTime(1);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        if (caster instanceof Player player) {
            player.setWorldBorder(fakeBorder);
        }
        CylinderEffect effect = new CylinderEffect(manager);
        effect.setEntity(caster);
        effect.iterations = 50;
        effect.type = EffectType.REPEATING;
        effect.particle = Particle.REDSTONE;
        effect.particleSize = 0.4f;
        effect.color = Color.RED;
        effect.duration = duration * 20 * 50;
        effect.height = 1.3f;
        effect.start();
        return super.onCast();
    }

    @Override
    protected void onTick() {
        caster.setHealth(Math.max(1, caster.getHealth() - baseDamagePerTick)); // Let's not kill the caster completely
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        if (caster instanceof Player player) {
            player.setWorldBorder(player.getWorld().getWorldBorder());
        }
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        damage =+Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADV_PHYSICAL);
        return super.onAttack(target, damage, type);
    }
}

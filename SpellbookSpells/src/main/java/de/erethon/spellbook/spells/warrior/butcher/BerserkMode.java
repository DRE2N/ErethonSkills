package de.erethon.spellbook.spells.warrior.butcher;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CylinderEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class BerserkMode extends WarriorBaseSpell {

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
        fakeBorder.setWarningTimeTicks(1);
    }

    @Override
    public boolean onCast() {
        if (caster instanceof Player player) {
            player.setWorldBorder(fakeBorder);
        }
        CylinderEffect effect = new CylinderEffect(manager);
        effect.setEntity(caster);
        effect.iterations = 50;
        effect.type = EffectType.REPEATING;
        effect.particle = Particle.DUST;
        effect.particleSize = 0.4f;
        effect.color = Color.RED;
        effect.duration = duration * 20 * 50;
        effect.height = 1.3f;
        effect.start();
        caster.getUsedSpells().put(data, System.currentTimeMillis());
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
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        damage =+Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADVANTAGE_PHYSICAL);
        return super.onAttack(target, damage, type);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(baseDamagePerTick * 20, VALUE_COLOR));
        placeholderNames.add("damage per second");
        return super.getPlaceholders(c);
    }
}

package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class TripleHit extends WarriorBaseSpell {

    private final int hitInterval = data.getInt("hitInterval", 10);
    private final double thirdBonusDamage = data.getDouble("thirdBonusDamage", 1.5f);
    private final int powerStacks = data.getInt("powerStacks", 3);
    private final int powerDuration = data.getInt("powerDuration", 200);
    private final EffectData power = Spellbook.getEffectData("Power");
    private int hits = 0;
    private double lastHitDamage = 0;

    public TripleHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        BukkitRunnable attackTimer = new BukkitRunnable() {
            @Override
            public void run() {
                caster.attack(target);
            }
        };
        attackTimer.runTaskTimer(Spellbook.getInstance().getImplementer(), hitInterval, hitInterval);
        return super.onCast();
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        hits++;
        if (hits == 3 && damage > lastHitDamage) {
            caster.addEffect(caster, power, powerDuration, powerStacks);
            currentTicks = keepAliveTicks;
            return damage + thirdBonusDamage;
        }
        lastHitDamage = damage;
        return super.onAttack(target, damage, type);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(hitInterval, VALUE_COLOR));
        placeholderNames.add("hit interval");
        spellAddedPlaceholders.add(Component.text(thirdBonusDamage, VALUE_COLOR));
        placeholderNames.add("third bonus damage");
        spellAddedPlaceholders.add(Component.text(powerStacks, VALUE_COLOR));
        placeholderNames.add("power stacks");
        spellAddedPlaceholders.add(Component.text(powerDuration, VALUE_COLOR));
        placeholderNames.add("power duration");
        return super.getPlaceholders(c);
    }
}

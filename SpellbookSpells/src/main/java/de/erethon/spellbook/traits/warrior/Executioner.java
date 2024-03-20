package de.erethon.spellbook.traits.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class Executioner extends SpellTrait {

    private final int duration = data.getInt("duration", 100);
    private final int stacks = data.getInt("stacks", 1);
    private final long cooldown = data.getInt("cooldown", 100) * 50L;
    private final EffectData effectData = Spellbook.getEffectData("Weakness");
    private long lastAttack = 0;

    public Executioner(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (System.currentTimeMillis() - lastAttack < cooldown) return damage;
        lastAttack = System.currentTimeMillis();
        target.addEffect(caster, effectData, duration, stacks);
        return super.onAttack(target, damage, type);
    }
}

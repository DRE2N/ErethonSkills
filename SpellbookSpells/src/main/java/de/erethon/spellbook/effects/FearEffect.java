package de.erethon.spellbook.effects;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class FearEffect extends SpellEffect {

    Vector awayVector;

    public FearEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        awayVector = caster.getLocation().toVector().subtract(target.getLocation().toVector()).multiply(-1);
    }

    @Override
    public void onApply() {
        target.setRotation(awayVector.toLocation(target.getWorld()).getYaw(), target.getLocation().getPitch());
    }

    @Override
    public void onTick() {
        target.setVelocity(awayVector.multiply(data.getDouble("speed", 1.1)));
    }
}

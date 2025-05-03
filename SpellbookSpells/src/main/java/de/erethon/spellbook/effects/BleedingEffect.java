package de.erethon.spellbook.effects;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.EntityEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class BleedingEffect extends SpellEffect {

    private int tick = 0;

    public BleedingEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onTick() {
        tick++;
        if (tick >= 20) {
            tick = 0;
            target.damage(Spellbook.getScaledValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL), PDamageType.PHYSICAL);
        }
    }
}

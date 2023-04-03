package de.erethon.spellbook.traits;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class TestTrait extends SpellTrait {

    public TestTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity entity : trigger.getTargets()) {
            entity.damage(100);
            MessageUtil.broadcastMessage("TestTrait triggered on " + entity.getName());
        }
    }
}

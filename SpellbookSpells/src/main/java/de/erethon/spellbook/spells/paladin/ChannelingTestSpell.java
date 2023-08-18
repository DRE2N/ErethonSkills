package de.erethon.spellbook.spells.paladin;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class ChannelingTestSpell extends SpellbookSpell {
    public ChannelingTestSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        channelDuration = 100;
        keepAliveTicks = 110;
        isMovementInterrupted = false;
        tickInterval = 1;
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected boolean onCast() {
        MessageUtil.broadcastMessage("Test spell cast!");
        return true;
    }

    @Override
    protected void onChannelFinish() {
        MessageUtil.broadcastMessage("Test spell finished!");
    }
}

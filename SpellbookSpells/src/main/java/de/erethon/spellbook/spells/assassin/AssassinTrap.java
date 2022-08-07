package de.erethon.spellbook.spells.assassin;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class AssassinTrap extends AoEBaseSpell {

    public AssassinTrap(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    public boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        keepAliveTicks = 200;
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        target.getWorld().spawnParticle(Particle.SMOKE_LARGE, target, 1);
    }

    @Override
    protected void onEnter(LivingEntity entity) {
        MessageUtil.broadcastMessage("Enter");
    }

    @Override
    protected void onLeave(LivingEntity entity) {
        MessageUtil.broadcastMessage("Leave");
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }
}


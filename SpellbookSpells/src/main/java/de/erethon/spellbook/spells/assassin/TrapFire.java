package de.erethon.spellbook.spells.assassin;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class TrapFire extends AssassinBaseTrap {

    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Burning");
    private final int effectDuration = data.getInt("effectDuration", 5) * 20;
    private final int effectStacks = data.getInt("effectStacks", 1);

    public TrapFire(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        keepAliveTicks = data.getInt("duration", 200);
        tickInterval = 20;
        return super.onCast();
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }

    @Override
    public void onTick() {
        super.onTick();
        for (LivingEntity entity : getEntities()) {
            /*if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }*/
            MessageUtil.log("TrapFire: " + entity.getName() + " is burning");
            entity.addEffect(caster, effectData, effectDuration, (int) Math.round(effectStacks * damageMultiplier)); // TODO: Nothing happening here?
            triggerTraits(2);
        }
    }

    @Override
    protected void onEnter(LivingEntity entity) {
        triggerTraits(entity, 1);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }
}


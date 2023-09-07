package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class TrapIce extends AssassinBaseTrap {

    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");
    private final int effectDuration = data.getInt("effectDuration", 5) * 20;
    private final int effectStacks = data.getInt("effectStacks", 1);

    public TrapIce(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        keepAliveTicks = data.getInt("duration", 10);
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
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }
            if (!entity.hasEffect(effectData)) {
                entity.addEffect(caster, effectData, effectDuration, effectStacks);
                triggerTraits(entity, 1);
                triggerTraits(2);
            }
        }
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }
}


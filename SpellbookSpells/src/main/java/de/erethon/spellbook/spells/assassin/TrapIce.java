package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class TrapIce extends AssassinBaseTrap {

    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");
    private final int effectDuration = data.getInt("effectDuration", 5);
    private final int effectStacks = data.getInt("effectStacks", 1);

    public TrapIce(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        keepAliveTicks = duration;
        tickInterval = 20;
        return super.onCast();
    }

    @Override
    public void onTick() {
        super.onTick();
        for (LivingEntity entity : getEntities()) {
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }
            if (!entity.hasEffect(effectData)) {
                entity.addEffect(caster, effectData, effectDuration * 20, effectStacks);
                triggerTraits(entity, 1);
                triggerTraits(2);
            }
        }
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster caster) {
        spellAddedPlaceholders.add(Component.text(effectDuration, VALUE_COLOR));
        placeholderNames.add("effectDuration");
        spellAddedPlaceholders.add(Component.text(effectStacks, VALUE_COLOR));
        placeholderNames.add("effectStacks");
        return super.getPlaceholders(caster);
    }
}


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

public class TrapPoison extends AssassinBaseTrap {

    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Poison");
    private final int effectDuration = data.getInt("effectDuration", 5) * 20;
    private final int effectStacks = data.getInt("effectStacks", 1);

    public TrapPoison(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        keepAliveTicks = duration * 10;
        tickInterval = 10;
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
            //if (!entity.hasEffect(effectData)) {
                entity.addEffect(caster, effectData, effectDuration, (int) Math.round(effectStacks * damageMultiplier));
            //}
            triggerTraits(entity, 1);
            triggerTraits(2);
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
        spellAddedPlaceholders.add(Component.text(effectStacks * damageMultiplier, VALUE_COLOR));
        placeholderNames.add("effectStacks");
        return super.getPlaceholders(caster);
    }
}


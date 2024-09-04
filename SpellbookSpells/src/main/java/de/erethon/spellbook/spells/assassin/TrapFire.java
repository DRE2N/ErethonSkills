package de.erethon.spellbook.spells.assassin;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class TrapFire extends AssassinBaseTrap {

    private final int lifetime = data.getInt("lifetime", 10);

    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Burning");
    private final int effectDuration = data.getInt("effectDuration", 5);
    private final int effectStacks = data.getInt("effectStacks", 1);

    public TrapFire(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        keepAliveTicks = lifetime * 20;
        tickInterval = 20;
        return super.onCast();
    }


    @Override
    public void onTick() {
        super.onTick();
        for (LivingEntity entity : getEntities()) {
            /*if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }*/
            MessageUtil.log("TrapFire: " + entity.getName() + " is burning");
            entity.addEffect(caster, effectData, effectDuration * 20, (int) Math.round(effectStacks * damageMultiplier)); // TODO: Nothing happening here?
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

    @Override
    public List<Component> getPlaceholders(SpellCaster caster) {
        spellAddedPlaceholders.add(Component.text(lifetime, VALUE_COLOR));
        placeholderNames.add("lifetime");
        spellAddedPlaceholders.add(Component.text(effectDuration, VALUE_COLOR));
        placeholderNames.add("effectDuration");
        spellAddedPlaceholders.add(Component.text(effectStacks * damageMultiplier, VALUE_COLOR));
        placeholderNames.add("effectStacks");
        return super.getPlaceholders(caster);
    }
}


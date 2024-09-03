package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class TrapExplosion extends AssassinBaseTrap {

    private final int duration = data.getInt("duration", 10);

    public TrapExplosion(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        keepAliveTicks = duration * 20;
        return super.onCast();
    }


    @Override
    protected void onEnter(LivingEntity entity) {
        if (entity == caster || !Spellbook.canAttack(caster, entity)) {
            return;
        }
        target.createExplosion((float) ((float) data.getDouble("power", 3.0) * damageMultiplier), false, false);
        triggerTraits(entity, 1);
        triggerTraits(2);
        keepAliveTicks = 1;
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster caster) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        spellAddedPlaceholders.add(Component.text(damageMultiplier, VALUE_COLOR));
        return super.getPlaceholders(caster);
    }
}


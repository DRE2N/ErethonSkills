package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class HolyCage extends AoEBaseSpell {

    private final AttributeModifier slowModifier = new AttributeModifier("HolyCage", data.getDouble("slow", -0.5f), AttributeModifier.Operation.ADD_NUMBER);

    public HolyCage(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("keepAliveTicks", 200);
    }

    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        return super.onCast();
    }

    @Override
    protected void onEnter(LivingEntity entity) {
        entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(slowModifier);
    }

    @Override
    protected void onLeave(LivingEntity entity) {
        entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(slowModifier);
    }
}

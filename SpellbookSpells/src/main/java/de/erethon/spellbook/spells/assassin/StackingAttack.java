package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class StackingAttack extends AssassinBaseSpell {

    private final int stacks = data.getInt("stacks", 3);
    private int currentStacks = 0;

    public StackingAttack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (target != this.target) {
            return damage;
        }
        damage = damage + (Spellbook.getScaledValue(data, caster, target, Attribute.ADV_AIR) * stacks);
        if (currentStacks <= stacks) {
            currentStacks++;
        }
        triggerTraits(target);
        return super.onAttack(target, damage, type);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(stacks, VALUE_COLOR));
        placeholderNames.add("stacks");
        spellAddedPlaceholders.add(Component.text(Spellbook.getScaledValue(data, caster, caster, Attribute.ADV_AIR), ATTR_AIR_COLOR));
        placeholderNames.add("damage");
        return super.getPlaceholders(c);
    }
}

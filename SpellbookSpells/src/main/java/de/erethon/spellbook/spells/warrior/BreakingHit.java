package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class BreakingHit extends WarriorBaseSpell {

    private final AttributeModifier reduction = new AttributeModifier("BreakingHit", data.getDouble("reduction", 0.5), AttributeModifier.Operation.ADD_SCALAR);

    public BreakingHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("keepAliveTicks", 100);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(3);
    }

    @Override
    protected boolean onCast() {
        target.getAttribute(Attribute.RES_PHYSICAL).addModifier(reduction);
        target.playSound(Sound.sound(org.bukkit.Sound.ITEM_ARMOR_EQUIP_CHAIN, Sound.Source.RECORD, 1, 0));
        caster.playSound(Sound.sound(org.bukkit.Sound.ITEM_ARMOR_EQUIP_CHAIN, Sound.Source.RECORD, 0.8f, 0));
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        target.getAttribute(Attribute.RES_PHYSICAL).removeModifier(reduction);
        super.cleanup();
    }
}
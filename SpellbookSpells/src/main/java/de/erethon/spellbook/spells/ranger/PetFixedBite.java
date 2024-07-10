package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PetFixedBite extends RangerPetBaseSpell {

    private final NamespacedKey key = new NamespacedKey("spellbook", "petfixedbite");
    private final AttributeModifier modifier = new AttributeModifier(key, -10000, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    private LivingEntity target;

    public PetFixedBite(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("keepAliveTicks", 80);
    }

    @Override
    protected boolean onPrecast() {
        if (pet.getTarget() == null) {
            caster.sendParsedActionBar("<color:#ff0000>Pet hat kein Ziel!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        target = pet.getTarget().getBukkitLivingEntity();
        pet.makeAttack(target);
        target.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).addTransientModifier(modifier);
        target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
        target.playSound(Sound.sound(org.bukkit.Sound.ENTITY_WOLF_GROWL, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_WOLF_GROWL, Sound.Source.RECORD, 0.8f, 1));
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        target.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).removeModifier(modifier);
        target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        super.cleanup();
    }
}

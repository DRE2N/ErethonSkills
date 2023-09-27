package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PetFixedBite extends RangerPetBaseSpell {

    private final AttributeModifier modifier = new AttributeModifier("PetFixedBite", -10000, AttributeModifier.Operation.ADD_NUMBER);
    private final PotionEffect noJump = new PotionEffect(PotionEffectType.JUMP, PotionEffect.INFINITE_DURATION, 128, false, false, false);

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
        target.addPotionEffect(noJump);
        target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
        target.playSound(Sound.sound(org.bukkit.Sound.ENTITY_WOLF_GROWL, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_WOLF_GROWL, Sound.Source.RECORD, 0.8f, 1));
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        target.removePotionEffect(PotionEffectType.JUMP);
        target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        super.cleanup();
    }
}

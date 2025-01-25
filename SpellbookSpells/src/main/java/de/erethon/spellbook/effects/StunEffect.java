package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class StunEffect extends SpellEffect {

    private final NamespacedKey key = new NamespacedKey("spellbook", "stun");

    private double prevMoveSpeed = 0;
    private int particleCD = 5;
    private final Random random = new Random();

    private AttributeModifier modifier = new AttributeModifier(key, 20, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public StunEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onTick() {
        if (particleCD-- > 0) {
            return;
        }
        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getLocation().add(random.nextDouble(-0.5, 0.5), 1.9,random.nextDouble(-0.5, 0.5)), 1);
        particleCD = 5;
    }

    @Override
    public void onApply() {
        for (SpellbookSpell spell : target.getActiveSpells()) {
            spell.interrupt();
        }
        target.getAttribute(Attribute.GRAVITY).addTransientModifier(modifier);
        target.playSound(Sound.sound(Key.key("block.anvil.place"), Sound.Source.RECORD, 0.5f, 0));
        if (target instanceof Player player) {
            prevMoveSpeed = player.getWalkSpeed(); // Walk speed doesn't change FOV.
            player.setWalkSpeed(0);
            return;
        }
        prevMoveSpeed = target.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue();
        target.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0);
    }

    @Override
    public void onRemove() {
        target.getAttribute(Attribute.GRAVITY).removeModifier(modifier);
        if (target instanceof Player player) {
            player.setWalkSpeed((float) prevMoveSpeed);
            return;
        }
        target.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(prevMoveSpeed);
    }
}

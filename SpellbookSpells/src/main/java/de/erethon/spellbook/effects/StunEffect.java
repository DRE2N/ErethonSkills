package de.erethon.spellbook.effects;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Random;

public class StunEffect extends SpellEffect {

    private static final String STUNNED_TEXT = "Stunned";
    private static final TextColor STUN_ACTIVE_COLOR = TextColor.color(255, 170, 64);
    private static final TextColor STUN_ELAPSED_COLOR = TextColor.color(190, 190, 190);
    private static final Title.Times STUN_TITLE_TIMES = Title.Times.times(Duration.ZERO, Duration.ofSeconds(999999), Duration.ZERO);
    private static final float DEFAULT_PLAYER_WALK_SPEED = 0.2f;

    private final NamespacedKey key = new NamespacedKey("spellbook", "stun");

    private int particleCD = 5;
    private int maximumTicksLeft;
    private int displayedElapsedLetters = -1;
    private final Random random = new Random();

    private final AttributeModifier modifier = new AttributeModifier(key, 20, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
    private final AttributeModifier speedModifier = new AttributeModifier(key, -10.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public StunEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        maximumTicksLeft = Math.max(1, duration);
    }

    @Override
    public void onTick() {
        if (target instanceof Player player) {
            updateStunTitle(player);
        }
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
        AttributeInstance gravity = target.getAttribute(Attribute.GRAVITY);
        if (gravity != null && gravity.getModifier(key) == null) {
            gravity.addTransientModifier(modifier);
        }
        target.playSound(Sound.sound(Key.key("block.anvil.place"), Sound.Source.RECORD, 0.5f, 0));
        if (target instanceof Player player) {
            player.setWalkSpeed(0);
            updateStunTitle(player);
            return;
        }
        AttributeInstance movementSpeed = target.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed != null && movementSpeed.getModifier(key) == null) {
            movementSpeed.addTransientModifier(speedModifier);
        }
    }

    @Override
    public void onRemove() {
        removeModifier(Attribute.GRAVITY);
        removeModifier(Attribute.MOVEMENT_SPEED);
        if (target instanceof Player player) {
            restorePlayerMovement(player);
            player.showTitle(Title.title(Component.empty(), Component.empty()));
            return;
        }
    }

    private void removeModifier(Attribute attribute) {
        AttributeInstance instance = target.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier appliedModifier = instance.getModifier(key);
        if (appliedModifier != null) {
            instance.removeModifier(appliedModifier);
        }
    }

    private void restorePlayerMovement(Player player) {
        player.setWalkSpeed(DEFAULT_PLAYER_WALK_SPEED);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && !player.hasEffect(data)) {
                    player.setWalkSpeed(DEFAULT_PLAYER_WALK_SPEED);
                }
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 1L);
    }

    private void updateStunTitle(Player player) {
        maximumTicksLeft = Math.max(maximumTicksLeft, ticksLeft);
        int elapsedTicks = Math.max(0, maximumTicksLeft - Math.max(0, ticksLeft));
        int elapsedLetters = Math.min(STUNNED_TEXT.length(), elapsedTicks * STUNNED_TEXT.length() / maximumTicksLeft);
        if (elapsedLetters == displayedElapsedLetters) {
            return;
        }
        displayedElapsedLetters = elapsedLetters;

        Component title = Component.empty();
        for (int index = 0; index < STUNNED_TEXT.length(); index++) {
            TextColor color = index < elapsedLetters ? STUN_ELAPSED_COLOR : STUN_ACTIVE_COLOR;
            title = title.append(Component.text(STUNNED_TEXT.charAt(index), color));
        }
        player.showTitle(Title.title(title, Component.empty(), STUN_TITLE_TIMES));
    }
}

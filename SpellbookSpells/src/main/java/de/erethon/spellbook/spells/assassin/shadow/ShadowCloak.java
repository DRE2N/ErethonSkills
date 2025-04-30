package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ShadowCloak extends AssassinBaseSpell {

    // Cloaks the caster in shadows, making them invisible and increasing their movement speed.
    // Attacking will break the cloak and deal massive bonus damage.
    // Removes negative effects.
    // If the target is marked, apply blindness.
    // Blindness scales with advantage_magical.

    private final int bonusDamage = data.getInt("bonusDamage", 100);
    private final int blindnessMinDuration = data.getInt("blindnessMinDuration", 100);
    private final int blindnessMaxDuration = data.getInt("blindnessMaxDuration", 200);

    private final AttributeModifier speedBoost = new AttributeModifier(new NamespacedKey("spellbook", "shadow_cloak"), 0.2, AttributeModifier.Operation.ADD_NUMBER);
    private final EffectData blindness = Spellbook.getEffectData("Blindness");

    public ShadowCloak(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        caster.setInvisible(true);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(speedBoost);
        Set<EffectData> toRemove = caster.getEffects().stream().filter(effect -> !effect.data.isPositive()).map(effect -> effect.data).collect(Collectors.toSet());
        toRemove.forEach(caster::removeEffect);
        return super.onCast();
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        endCloak();
        if (target instanceof Player player) {
            player.playSound(caster, Sound.ITEM_SHIELD_BREAK, 1, 1);
        }
        if (caster instanceof Player casterPlayer) {
            casterPlayer.playSound(caster, Sound.ITEM_SHIELD_BREAK, 1, 1);
        }
        target.addEffect(caster, blindness, (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, blindnessMinDuration, blindnessMaxDuration, "blindness"), 1);
        return super.onAttack(target, damage + bonusDamage, type);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        endCloak();
    }

    private void endCloak() {
        caster.setInvisible(false);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(speedBoost);
    }
}

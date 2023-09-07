package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PowerfulJump extends SpellTrait {

    private final int jumpEffectStrength = data.getInt("jumpEffectStrength", 1);

    public PowerfulJump(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, jumpEffectStrength, true, false, false));
    }

    @Override
    protected void onRemove() {
        caster.removePotionEffect(PotionEffectType.JUMP);
    }
}

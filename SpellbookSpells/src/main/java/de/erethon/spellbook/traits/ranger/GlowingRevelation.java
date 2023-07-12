package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GlowingRevelation extends SpellTrait {

    private final int glowingDuration = data.getInt("glowingDuration", 100);

    public GlowingRevelation(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTargets()) {
            living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowingDuration, 1));
        }
    }
}

package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class ShoutDemoralize extends AbstractWarriorShout {
    public ShoutDemoralize(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        Set<EffectData> toRemove = new HashSet<>();
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data.isPositive()) {
                toRemove.add(effect.data);
            }
        }
        for (EffectData data : toRemove) {
            target.removeEffect(data);
        }
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.RECORDS, 1, 0.5f);
        return super.onCast();
    }
}

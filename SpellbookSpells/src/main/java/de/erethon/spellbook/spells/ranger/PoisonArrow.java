package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Collections;

public class PoisonArrow extends ProjectileRelatedSkill {

    private final EffectData poison = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Poison");

    private final int poisonDuration = data.getInt("poisonDuration", 100);
    private final int poisonStacks = data.getInt("poisonStacks", 1);

    public PoisonArrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        trailColor = Color.GREEN;
        affectedArrows = spellData.getInt("affectedArrows", 1);
    }

    @Override
    protected void onHit(ProjectileHitEvent event, LivingEntity living) {
        living.addEffect(caster, poison, poisonDuration, poisonStacks);
        triggerTraits(Collections.singleton(living));
    }
}

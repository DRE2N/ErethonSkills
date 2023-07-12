package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Collections;

public class FireArrow extends ProjectileRelatedSkill {

    private final EffectData fire = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Burning");

    private final int burningDuration = data.getInt("burningDuration", 100);
    private final int burningStacks = data.getInt("burningStacks", 1);

    public FireArrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        trailColor = Color.RED;
        affectedArrows = spellData.getInt("affectedArrows", 1);
    }

    @Override
    protected void onShoot(EntityShootBowEvent event) {
        event.getProjectile().setVisualFire(true);
    }

    @Override
    protected void onHit(ProjectileHitEvent event, LivingEntity living) {
        living.addEffect(caster, fire, burningDuration, burningStacks);
        triggerTraits(Collections.singleton(living));
    }
}

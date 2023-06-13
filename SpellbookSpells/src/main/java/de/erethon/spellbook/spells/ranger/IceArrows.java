package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class IceArrows extends ProjectileRelatedSkill {

    private final int effectDuration = data.getInt("effectDuration", 20);
    private final int stacks = data.getInt("effectStacks", 1);

    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");

    public IceArrows(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        trailColor = Color.AQUA;
        keepAliveTicks = spellData.getInt("duration", 20 * 15);
    }

    @Override
    protected void onDamage(EntityDamageByEntityEvent event, Projectile projectile) {
        if (event.getEntity() == caster) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity living) {
            living.addEffect(caster, effectData, effectDuration, stacks);
        }
        super.onDamage(event, projectile);
    }
}

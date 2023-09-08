package de.erethon.spellbook.spells.ranger;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.HashSet;
import java.util.Set;

public class ExplosiveArrow extends ProjectileRelatedSkill {

    private final float explosionPower = (float) data.getDouble("explosionPower", 2.0f);

    public ExplosiveArrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        return true;
    }

    @Override
    protected void onHit(ProjectileHitEvent event, LivingEntity living) {
        Set<LivingEntity> affected = new HashSet<>();
        if (event.getHitEntity() != null) {
            event.getHitEntity().getLocation().createExplosion(caster, explosionPower, false, false);
            for (LivingEntity livingEntity : event.getHitEntity().getLocation().getNearbyLivingEntities(explosionPower)) {
                if (Spellbook.canAttack(caster, livingEntity)) {
                    affected.add(livingEntity);
                }
            }
            return;
        }
        if (event.getHitBlock() != null) {
            event.getHitBlock().getLocation().createExplosion(caster, explosionPower, false, false);
            for (LivingEntity livingEntity : event.getHitBlock().getLocation().getNearbyLivingEntities(explosionPower)) {
                if (Spellbook.canAttack(caster, livingEntity)) {
                    affected.add(livingEntity);
                }
            }
        }
        triggerTraits(affected);
    }
}

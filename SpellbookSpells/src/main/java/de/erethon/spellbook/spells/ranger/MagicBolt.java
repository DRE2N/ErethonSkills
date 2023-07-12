package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.utils.RangerUtils;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;

public class MagicBolt extends ProjectileRelatedSkill {

    private final int range = data.getInt("range", 32);
    private final int maxTargets = data.getInt("maxTargets", 3);

    private int targets = 0;

    public MagicBolt(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        trailColor = Color.TEAL;
    }

    @Override
    protected boolean onCast() {
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            if (living == caster || !Spellbook.canAttack(caster, living)) {
                continue;
            }
            targets++;
            if (targets > maxTargets) {
                break;
            }
            Projectile projectile = RangerUtils.sendProjectile(caster, living, caster, 2, Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADV_MAGIC), DamageType.MAGIC);
            EntityShootBowEvent event = new EntityShootBowEvent(caster, caster.getEquipment().getItemInMainHand(), projectile, 2);
            Bukkit.getPluginManager().callEvent(event);

        }
        if (targets == 0) {
            caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_VILLAGER_NO, Sound.Source.RECORD, 1, 1));
        }
        return super.onCast();
    }
}

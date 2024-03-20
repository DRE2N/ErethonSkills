package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.PDamageType;;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.events.ItemProjectileHitEvent;
import de.erethon.spellbook.utils.AssassinUtils;
import de.erethon.spellbook.utils.ItemProjectile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class DaggerStorm extends AssassinBaseSpell implements Listener {

    private final int daggers = data.getInt("daggers", 5);
    private final int divergence = data.getInt("divergence", 5);
    private final float speed = (float) data.getDouble("speed", 3.0);

    public double damageMultiplier = 1.0;

    public DaggerStorm(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 2;
        keepAliveTicks = data.getInt("duration", 500);
        Bukkit.getServer().getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }


    @Override
    protected void onTick() {
        Vector dir = caster.getEyeLocation().getDirection();
        for (int i = 0; i <= daggers; i++) {
            ItemProjectile projectile = new ItemProjectile(new ItemStack(Material.IRON_SWORD), caster.getEyeLocation().getX(), caster.getEyeLocation().getY(), caster.getEyeLocation().getZ(), caster.getWorld(), this);
            projectile.shoot(dir.getX(), dir.getY(), dir.getZ(), speed, divergence);
        }
    }

    @EventHandler
    private void onHit(ItemProjectileHitEvent event) {
        if (event.getSpell() == this) {
            if (event.getHitEntity() instanceof LivingEntity entity) {
                if (entity == caster || !Spellbook.canAttack(caster, entity)) {
                    return;
                }
                entity.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADV_PHYSICAL) * damageMultiplier, caster, PDamageType.PHYSICAL);
                triggerTraits(entity);
            }
        }
    }
}

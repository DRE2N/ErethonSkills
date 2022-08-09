package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.events.ItemProjectileHitEvent;
import de.erethon.spellbook.utils.ItemProjectile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Random;

public class DaggerStorm extends SpellbookSpell implements Listener {

    private final int daggers = data.getInt("daggers", 5);
    private final int divergence = data.getInt("divergence", 5);
    private final double damage = data.getDouble("damage", 3.0);
    private final float speed = (float) data.getDouble("speed", 3.0);

    public DaggerStorm(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 2;
        keepAliveTicks = data.getInt("duration", 500);
        Bukkit.getServer().getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }


    @Override
    protected boolean onPrecast() {
        return AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 80));
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
                if (entity == caster) {
                    return;
                }
                entity.damage(damage, caster, DamageType.PHYSICAL);
            }
        }
    }
}

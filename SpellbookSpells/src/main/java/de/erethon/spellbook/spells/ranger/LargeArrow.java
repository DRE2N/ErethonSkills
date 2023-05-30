package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.ChannelingSpell;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.events.ItemProjectileHitEvent;
import de.erethon.spellbook.utils.ItemProjectile;
import de.erethon.spellbook.utils.NMSUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class LargeArrow extends ChannelingSpell implements Listener {

    private double arrowBoxSize;
    private double damage;
    private DamageType damageType;
    private ItemStack itemStack;

    public LargeArrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        arrowBoxSize = spellData.getDouble("arrowBoxSize", 3.0);
        itemStack = NMSUtils.getItemStackWithModelData(Material.ARROW, spellData.getInt("modelData", 1));
        damage = spellData.getDouble("damage", 1.0);
        damageType = DamageType.valueOf(spellData.getString("damageType", "PHYSICAL").toUpperCase());
    }

    @Override
    protected void onInterrupt() {
        super.onInterrupt();
    }

    @Override
    protected void onChannelFinish() {
        Location location = caster.getEyeLocation();
        Vector direction = location.getDirection();
        ItemProjectile projectile = new ItemProjectile(itemStack, location.getX(), location.getY(), location.getZ(), location.getWorld(), this);
        BoundingBox box = new BoundingBox(0, 0, 0, arrowBoxSize, arrowBoxSize, arrowBoxSize);
        NMSUtils.setEntityBoundingBox(projectile.getBukkitEntity(), box);
        projectile.setNoGravity(true);
        projectile.shoot(direction.getX(), direction.getY(), direction.getZ(), 1.0F, 0.0F);
    }

    @EventHandler
    private void onHit(ItemProjectileHitEvent event) {
        if (event.getSpell() == this && event.getHitEntity() instanceof LivingEntity living) {
            living.damage(damage, caster, damageType);
        }
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }

    @Override
    protected boolean onPrecast() {
        return RangerUtils.hasMana(caster, data);
    }

    @Override
    protected boolean onCast() {
        return super.onCast();
    }
}

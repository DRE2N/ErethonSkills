package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.ChannelingSpell;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.utils.NMSUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

public class OvergrownShield extends ChannelingSpell {

    int modelData = data.getInt("modelData", 1);
    int duration = data.getInt("duration", 400);
    BoundingBox aabb;
    ArmorStand armorStand;

    public OvergrownShield(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration;
    }


    @Override
    public boolean onCast() {
        Location location = caster.getLocation();
        ItemStack item = NMSUtils.getItemStackWithModelData(Material.OAK_SAPLING, modelData);
        armorStand = NMSUtils.spawnItemArmorstand(item, caster.getLocation());
        armorStand.setSmall(true);
        caster.addPassenger(armorStand);
        aabb = BoundingBox.of(location, 2, 2, 2);
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (interrupted) {
            keepAliveTicks = 0;
            return;
        }
        caster.getWorld().getNearbyEntities(aabb).forEach(entity -> {
            if (entity instanceof Projectile) {
                entity.remove();
            }
        });
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        armorStand.remove();
    }
}

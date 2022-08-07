package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * @author Fyreum
 */
public class DaggerThrow extends AssassinBaseSpell {

    double damage;
    Vector direction;
    ArmorStand armorStand;

    public DaggerThrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        damage = data.getDouble("damage", 30.0);
        tickInterval = 1;
        keepAliveTicks = -1;
    }

    @Override
    protected boolean onCast() {
        direction = caster.getEyeLocation().getDirection();
        armorStand = caster.getWorld().spawn(caster.getLocation().add(direction), ArmorStand.class);
        armorStand.getEquipment().setHelmet(new ItemStack(Material.IRON_SWORD));
        armorStand.setVisible(false);
        armorStand.setCanPickupItems(false);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        return true;
    }

    @Override
    protected void onTick() {
        Location updatedLocation = armorStand.getLocation().add(direction);
        armorStand.teleport(updatedLocation);

        if (updatedLocation.getBlock().getType().isCollidable()) {
            armorStand.remove();
            keepAliveTicks = 0;
            caster.sendActionbar("<red>Verfehlt!");
            return;
        }
        for (LivingEntity entity : updatedLocation.getNearbyLivingEntities(1D)) {
            entity.damage(damage, caster, DamageType.PHYSICAL);
            EffectData effect = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("shock");
            if (effect != null) {
                entity.addEffect(caster, effect, 1, 5);
            }
            armorStand.remove();
            keepAliveTicks = 0;
            caster.sendActionbar("<green>Getroffen!");
            return;
        }
    }
}

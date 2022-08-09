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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * @author Fyreum
 */
public class DaggerThrow extends SpellbookSpell implements Listener {

    Vector direction;
    ItemProjectile arrow;
    private final float speed = (float) data.getDouble("speed", 2.0);
    private final double damage = data.getDouble("damage", 30.0);
    private final int divergence = data.getInt("divergence", 1);

    public DaggerThrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 1;
        keepAliveTicks = 200;
        Bukkit.getServer().getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected boolean onPrecast() {
        return AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        direction = caster.getEyeLocation().getDirection();
        arrow = new ItemProjectile(new ItemStack(Material.IRON_SWORD), caster.getEyeLocation().getX(), caster.getEyeLocation().getY(), caster.getEyeLocation().getZ(), caster.getWorld(), this);
        arrow.shoot(direction.getX(), direction.getY(), direction.getZ(), speed, divergence);
        return true;
    }

    @EventHandler
    private void onHit(ItemProjectileHitEvent event) {
        if (event.getSpell() == this) {
           if (event.getHitEntity() instanceof LivingEntity entity) {
                if (entity == caster) {
                    return;
                }
                entity.damage(damage, caster, DamageType.PHYSICAL);
                EffectData effect = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Shock");
                if (effect != null) {
                    entity.addEffect(caster, effect, 1, 5);
                }
                keepAliveTicks = 0;
                caster.sendActionbar("<green>Getroffen!");
                event.getArrow().remove();
            }
        }
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }

    @Override
    protected void onTickFinish() {
        HandlerList.unregisterAll(this);
    }
}

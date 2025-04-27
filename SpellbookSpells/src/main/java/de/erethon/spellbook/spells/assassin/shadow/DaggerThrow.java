package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.events.ItemProjectileHitEvent;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.utils.ItemProjectile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * @author Fyreum
 */
public class DaggerThrow extends AssassinBaseSpell implements Listener {

    // Throws a dagger at the target, dealing physical damage and applying a slow effect. Marks the target for a short duration.

    private final float speed = (float) data.getDouble("speed", 2.0);
    private final int divergence = data.getInt("divergence", 1);
    private final int effectDuration = data.getInt("effectDuration", 30);
    private final int effectStacks = data.getInt("effectStacks", 5);

    private LivingEntity hitEntity = null;

    public DaggerThrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 1;
        keepAliveTicks = duration * 20;
        Bukkit.getServer().getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    public boolean onCast() {
        Vector direction = caster.getEyeLocation().getDirection();
        ItemProjectile arrow = new ItemProjectile(new ItemStack(Material.IRON_SWORD), caster.getEyeLocation().getX(), caster.getEyeLocation().getY(), caster.getEyeLocation().getZ(), caster.getWorld(), this);
        arrow.shoot(direction.getX(), direction.getY(), direction.getZ(), speed, divergence);
        return super.onCast();
    }

    @EventHandler
    private void onHit(ItemProjectileHitEvent event) {
        if (event.getSpell() == this) {
           if (event.getHitEntity() instanceof LivingEntity entity) {
                if (entity == caster || !Spellbook.canAttack(caster, entity)) {
                    return;
                }
                entity.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_PHYSICAL), caster, PDamageType.PHYSICAL);
                triggerTraits(target);
                EffectData effect = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");
                if (effect != null) {
                    entity.addEffect(caster, effect, effectDuration, effectStacks);
                }
                keepAliveTicks = 0;
                caster.playSound(Sound.sound(Key.key("entity.arrow.hit_player"), Sound.Source.RECORD, 1, 0.9f));
                event.getArrow().remove();
                entity.getTags().add("assassin.daggerthrow.marked");
                hitEntity = entity;
            }
        }
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        if (hitEntity != null) {
            hitEntity.getTags().remove("assassin.daggerthrow.marked");
            hitEntity = null;
        }
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(speed, VALUE_COLOR));
        placeholderNames.add("speed");
        spellAddedPlaceholders.add(Component.text(divergence, VALUE_COLOR));
        placeholderNames.add("divergence");
        spellAddedPlaceholders.add(Component.text(Spellbook.getVariedAttributeBasedDamage(data, caster, caster, false, Attribute.ADVANTAGE_PHYSICAL), VALUE_COLOR));
        placeholderNames.add("damage");
        return super.getPlaceholders(caster);
    }
}

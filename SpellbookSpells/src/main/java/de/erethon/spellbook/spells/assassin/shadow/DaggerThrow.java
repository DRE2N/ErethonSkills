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
    // Slow scales with advantage_magical.

    private final float speed = (float) data.getDouble("speed", 2.0);
    private final int divergence = data.getInt("divergence", 1);
    private final int effectDurationMin = data.getInt("slowDurationMin", 3) * 20;
    private final int effectDurationMax = data.getInt("slowDurationMax", 6) * 20;
    private final int effectStacksMin = data.getInt("slowStacksMin", 5);
    private final int effectStacksMax = data.getInt("slowStacksMax", 8);

    private final EffectData slowEffect = Spellbook.getEffectData("Slow");

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

                double slowDuration = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, effectDurationMin, effectDurationMax, "slowDuration");
                double slowStacks = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, effectStacksMin, effectStacksMax, "slowStacks");
                entity.addEffect(caster, slowEffect, (int) slowDuration, (int) slowStacks);

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
    public void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, effectDurationMin, effectDurationMax, "slowDuration") / 20, VALUE_COLOR));
        placeholderNames.add("slowDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, effectStacksMin, effectStacksMax, "slowStacks"), VALUE_COLOR));
        placeholderNames.add("slowStacks");
    }
}

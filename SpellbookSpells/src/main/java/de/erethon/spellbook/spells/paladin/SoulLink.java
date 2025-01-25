package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.List;

public class SoulLink extends PaladinBaseSpell implements Listener {

    private final double damagePercentage = data.getDouble("damagePercentage", 0.5);
    private final double maxDistance = data.getDouble("maxDistance", 4);
    private boolean linkActive = false;
    private LineEffect effect;

    public SoulLink(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(true);
    }

    @Override
    public boolean onCast() {
        linkActive = true;
        effect = new LineEffect(Spellbook.getInstance().getEffectManager());
        effect.setLocation(caster.getLocation().add(0, 1.5, 0));
        effect.setTargetLocation(target.getLocation().add(0, 1.5, 0));
        effect.particle = Particle.DUST;
        effect.color = Color.PURPLE;
        effect.particleSize = 0.3f;
        effect.start();
        if (caster.getLocation().distanceSquared(target.getLocation()) > maxDistance * maxDistance) {
            // Push them together
            Vector vector = target.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(-0.5);
            caster.setVelocity(vector);
        }
        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (caster.getLocation().distanceSquared(target.getLocation()) > maxDistance * maxDistance) {
            linkActive = false;
            interrupt();
            return;
        }
        effect.setLocation(caster.getLocation().add(0, 1.5, 0));
        effect.setTargetLocation(target.getLocation().add(0, 1.5, 0));
    }



    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() != target) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        if (!linkActive) {
            return;
        }
        double paladinDamage = event.getDamage() * damagePercentage;
        event.setDamage(event.getDamage() - paladinDamage);
        //missing method caster.damage(paladinDamage, event.getDamageType());
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDERMAN_STARE, Sound.Source.RECORD, 1, 1));
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        if (effect == null) return;
        effect.cancel();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(damagePercentage * 100, VALUE_COLOR));
        placeholderNames.add("damage percentage");
        spellAddedPlaceholders.add(Component.text(maxDistance, VALUE_COLOR));
        placeholderNames.add("max distance");
        return super.getPlaceholders(c);
    }
}

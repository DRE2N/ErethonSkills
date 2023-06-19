package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SoulLink extends PaladinBaseSpell implements Listener {

    private final double damagePercentage = data.getDouble("damagePercentage", 0.5);
    private boolean linkActive = false;
    private LineEffect effect;

    public SoulLink(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        keepAliveTicks = spellData.getInt("duration", 600);
    }

    @Override
    protected boolean onPrecast() {
        if (!lookForTarget()) {
            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        linkActive = true;
        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (caster.getLocation().distanceSquared(target.getLocation()) > 4 * 4) {
            linkActive = false;
            interrupt();
            return;
        }
        effect = new LineEffect(Spellbook.getInstance().getEffectManager());
        effect.setEntity(caster);
        effect.setTargetEntity(target);
        effect.particle = Particle.REDSTONE;
        effect.color = Color.PURPLE;
        effect.start();
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
        caster.damage(paladinDamage, event.getDamageType());
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDERMAN_STARE, Sound.Source.RECORD, 1, 1));
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        effect.cancel();
    }
}
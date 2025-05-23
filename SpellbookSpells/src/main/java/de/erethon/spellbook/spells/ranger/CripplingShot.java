package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.List;

public class CripplingShot extends ProjectileRelatedSkill {

    private int effectDuration = data.getInt("effectDuration", 20);
    private int stacks = data.getInt("effectStacks", 1);
    private final int projectileSpeed = data.getInt("projectileSpeed", 2);
    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");

    public CripplingShot(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        trailColor = Color.BLACK;
    }

    @Override
    protected boolean onPrecast() {
        if (!lookForTarget(true)) {
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        Vector from = caster.getLocation().toVector();
        Vector to = target.getLocation().toVector();
        Vector direction = to.subtract(from);
        direction = direction.normalize();
        direction.multiply(projectileSpeed);
        Projectile projectile = caster.launchProjectile(Arrow.class, direction);
        projectile.setGravity(false);
        addEffect(projectile);
        return super.onCast();
    }

    @Override
    protected void onDamage(EntityDamageByEntityEvent event, Projectile projectile) {
        if (event.getEntity() instanceof LivingEntity living) {
            if (inFlowState()) {
                effectDuration *= 2;
                stacks *= 2;
                removeFlow();
            }
            living.addEffect(caster, effectData, effectDuration, stacks);
        }
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(effectDuration, VALUE_COLOR));
        placeholderNames.add("effect duration");
        spellAddedPlaceholders.add(Component.text(stacks, VALUE_COLOR));
        placeholderNames.add("effect stacks");
        spellAddedPlaceholders.add(Component.text(projectileSpeed, VALUE_COLOR));
        placeholderNames.add("projectile speed");
        return super.getPlaceholders(c);
    }
}

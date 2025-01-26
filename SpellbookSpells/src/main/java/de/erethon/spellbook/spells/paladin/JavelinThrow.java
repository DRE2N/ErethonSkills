package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.List;

public class JavelinThrow extends PaladinSpearSpell implements Listener {

    private final double throwSpeed = data.getDouble("throwSpeed", 1);

    private Trident trident;

    public JavelinThrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        keepAliveTicks = 600;
    }

    @Override
    public boolean onCast() {
        caster.launchProjectile(Trident.class, caster.getLocation().getDirection().multiply(throwSpeed), e -> {
            e.setDamage(0);
            trident = e;
        });
        return super.onCast();
    }

    @EventHandler
    public void onTridentHit(ProjectileHitEvent event) {
        if (event.getEntity() != trident) return;
        if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity living) {
            if (!Spellbook.canAttack(caster, living)) return;
            living.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, damageAttribute), caster, damageType);
            triggerTraits(target);
            caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ARROW_HIT_PLAYER, Sound.Source.RECORD, 1, 1));
        }
        trident.remove();
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        trident.remove();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(Spellbook.getVariedAttributeBasedDamage(data, caster, caster, true, damageAttribute), VALUE_COLOR));
        placeholderNames.add("damage");
        return super.getPlaceholders(c);
    }
}

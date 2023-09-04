package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class JavelinThrow extends PaladinBaseSpell implements Listener {

    private final double throwSpeed = data.getDouble("throwSpeed", 1);

    private Trident trident;

    public JavelinThrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        keepAliveTicks = 600;
    }

    @Override
    protected boolean onCast() {
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
            living.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADV_PHYSICAL));
            caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ARROW_HIT_PLAYER, Sound.Source.RECORD, 1, 1));
        }
        trident.remove();
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        trident.remove();
    }
}

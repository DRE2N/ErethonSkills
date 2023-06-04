package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class Headbutt extends EntityTargetSpell implements Listener {

    private final int stunDuration = data.getInt("stunDuration", 60);
    private final EffectData stun = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Stun");
    private final double distanceToTarget = data.getDouble("distanceToTarget", 1.2);

    private boolean waitingForImpact = false;

    public Headbutt(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        keepAliveTicks = 120;
    }

    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        Vector move = targetEntity.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(2).setY(0.5);
        caster.setVelocity(move);
        waitingForImpact = true;
        return super.onCast();
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (event.getPlayer() != caster) return;
        if (!waitingForImpact) return;
        if (event.getTo().distanceSquared(targetEntity.getLocation()) < distanceToTarget * distanceToTarget) {
            caster.setVelocity(new Vector(0, 0, 0));
            targetEntity.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, targetEntity, false, Attribute.ADV_PHYSICAL), caster, DamageType.PHYSICAL);
            targetEntity.addEffect(caster, stun, stunDuration, 1);
            caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_LAND, Sound.Source.RECORD, 0.5f, 1));
            targetEntity.playSound(Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_LAND, Sound.Source.RECORD, 0.5f, 1));
            onTickFinish();
        }
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
    }
}

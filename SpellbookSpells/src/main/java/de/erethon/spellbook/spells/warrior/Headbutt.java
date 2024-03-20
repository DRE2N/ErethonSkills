package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class Headbutt extends WarriorBaseSpell implements Listener {

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
    protected boolean onPrecast() {
        return lookForTarget();
    }

    @Override
    public boolean onCast() {
        Vector move = target.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(2).setY(0.5);
        caster.setVelocity(move);
        waitingForImpact = true;
        return super.onCast();
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (event.getPlayer() != caster) return;
        if (!waitingForImpact) return;
        if (event.getTo().distanceSquared(target.getLocation()) < distanceToTarget * distanceToTarget) {
            caster.setVelocity(new Vector(0, 0, 0));
            target.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADV_PHYSICAL), caster, PDamageType.PHYSICAL);
            target.addEffect(caster, stun, stunDuration, 1);
            caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_LAND, Sound.Source.RECORD, 0.5f, 1));
            target.playSound(Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_LAND, Sound.Source.RECORD, 0.5f, 1));
            triggerTraits(target);
            onTickFinish();
        }
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
    }
}

package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class IceSpear extends PaladinSpearSpell implements Listener {

    private final EffectData slowness = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");
    public boolean shouldSlow = true;

    public IceSpear(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 200;
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(3);
    }

    @Override
    protected boolean onCast() {
        target.playSound(Sound.sound(org.bukkit.Sound.BLOCK_GLASS_BREAK, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_GLASS_BREAK, Sound.Source.RECORD, 0.8f, 1));
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (player != target) return;
        int duration = (int) (data.getInt("baseDuration", 20) + Math.round(Spellbook.getScaledValue(data, caster, target, Attribute.ADV_PHYSICAL)));
        triggerTraits(target);
        if (shouldSlow) {
            player.addEffect(caster, slowness, duration, data.getInt("stacks", 1));
        }
        player.setFreezeTicks(duration);
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
    }
}

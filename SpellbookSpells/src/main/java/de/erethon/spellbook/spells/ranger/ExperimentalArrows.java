package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.HashSet;
import java.util.Set;

public class ExperimentalArrows extends SpellTrait implements Listener {

    private final int duration = data.getInt("duration", 100);
    private final Set<EffectData> effects = new HashSet<>();

    public ExperimentalArrows(TraitData data, LivingEntity caster) {
        super(data, caster);
        effects.add(Spellbook.getEffectData("Weakness"));
        effects.add(Spellbook.getEffectData("Slow"));
        effects.add(Spellbook.getEffectData("Poison"));
        effects.add(Spellbook.getEffectData("Blindness"));
        effects.add(Spellbook.getEffectData("Burning"));
        effects.add(Spellbook.getEffectData("Bleeding"));
        effects.add(Spellbook.getEffectData("Confusion"));
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() != caster || event.getHitEntity() == null || !(event.getHitEntity() instanceof LivingEntity target)) return;
        EffectData effect = effects.stream().skip((int) (effects.size() * Math.random())).findFirst().orElse(null);
        if (effect == null) return;
        target.addEffect(caster, effect, duration, 1);
    }

    @Override
    protected void onAdd() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
    }
}

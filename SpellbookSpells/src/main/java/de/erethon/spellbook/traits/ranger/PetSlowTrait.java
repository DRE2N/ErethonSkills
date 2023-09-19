package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.events.PetAttackEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class PetSlowTrait extends SpellTrait implements Listener {

    private final int duration = data.getInt("duration", 100);
    private final int stacks = data.getInt("stacks", 1);
    private final int attacksToSlow = data.getInt("attacksToSlow", 3);
    private final EffectData slow = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");

    private int currentAttacks = 0;

    public PetSlowTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onPetAttack(PetAttackEvent event) {
        if (!(event.getPet().getBukkitOwner() == caster)) return;
        if (!Spellbook.canAttack(caster, event.getTarget())) return;
        currentAttacks++;
        if (currentAttacks < attacksToSlow) return;
        event.getTarget().addEffect(caster, slow,  duration, stacks);
        currentAttacks = 0;
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

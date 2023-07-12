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

public class PetBleedingTrait extends SpellTrait implements Listener {

    private final int duration = data.getInt("duration", 100);
    private final int stacks = data.getInt("stacks", 1);
    private final int attacksToBleed = data.getInt("attacksToBleed", 3);
    private final EffectData bleeding = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("bleeding");

    private int currentAttacks = 0;

    public PetBleedingTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onPetAttack(PetAttackEvent event) {
        if (!(event.getPet().getBukkitOwner() == caster)) return;
        if (!Spellbook.canAttack(caster, event.getTarget())) return;
        currentAttacks++;
        if (currentAttacks < attacksToBleed) return;
        event.getTarget().addEffect(caster, bleeding,  duration, stacks);
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

package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.events.PetAttackEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class PetFangs extends SpellTrait implements Listener {

    private final int bonusDamage = data.getInt("bonusDamage", 5);

    public PetFangs(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onPetAttack(PetAttackEvent event) {
        if (!(event.getPet().getBukkitOwner() == caster)) return;
        if (!Spellbook.canAttack(caster, event.getTarget())) return;
        event.getTarget().damage(bonusDamage, caster);
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

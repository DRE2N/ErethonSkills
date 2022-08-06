package de.erethon.spellbook.spells.assassin.passive;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityToggleSwimEvent;

public class SwimSpeed extends PassiveSpell {

    public SwimSpeed(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @EventHandler
    public void onSwimToggle(EntityToggleSwimEvent event) {
        if (event.getEntity() != caster) {
            return;
        }
        double baseValue = caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(event.isSwimming() ? baseValue + data.getDouble("speed", 2.0) : baseValue - data.getDouble("speed", 2.0));
    }
}

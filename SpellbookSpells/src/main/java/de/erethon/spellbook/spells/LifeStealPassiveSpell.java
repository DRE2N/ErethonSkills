package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * @author Fyreum
 */
public class LifeStealPassiveSpell extends PassiveSpell {

    double percentage;

    public LifeStealPassiveSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        percentage = data.getDouble("percentage", 0.1D);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().equals(caster)) {
            double damage = event.getFinalDamage();
            if (damage <= 0) {
                return;
            }
            double maxHealth = caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double health = caster.getHealth();
            if (health >= maxHealth) {
                return;
            }
            caster.setHealth(Math.min(health + damage * percentage, maxHealth));
        }
    }
}

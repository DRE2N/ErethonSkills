package de.erethon.spellbook.spells;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
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

    public LifeStealPassiveSpell(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
        percentage = data.getDouble("percentage", 0.1D);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        LivingEntity entity = caster.getEntity();
        if (event.getDamager().equals(entity)) {
            double damage = event.getFinalDamage();
            if (damage <= 0) {
                return;
            }
            double maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double health = entity.getHealth();
            if (health >= maxHealth) {
                return;
            }
            entity.setHealth(Math.min(health + damage * percentage, maxHealth));
        }
    }
}

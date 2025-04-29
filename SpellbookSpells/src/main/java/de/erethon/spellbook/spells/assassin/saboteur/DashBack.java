package de.erethon.spellbook.spells.assassin.saboteur;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DashBack extends AssassinBaseSpell {

    private final double dashMultiplier = data.getDouble("dashMultiplier", 2.0);
    private final double sideDashStrength = data.getDouble("sideDashStrength", 1.5);

    public DashBack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Location location = caster.getLocation();
        location.setPitch(-10);
        Vector direction = location.getDirection().normalize();
        Vector inputOffset = new Vector();
        // Add dash to the left/right based on player input
        if (caster instanceof Player player ) {
            Input input = player.getCurrentInput();
            Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            if (input.isLeft()) {
                inputOffset.add(right.clone().multiply(sideDashStrength));
            }
            if (input.isRight()) {
                inputOffset.add(right.clone().multiply(-sideDashStrength));
            }
        }
        Vector forwardDash = direction.multiply(dashMultiplier);
        // Combine forward dash and side dash (inputOffset)
        Vector dashVector = forwardDash.add(inputOffset);
        caster.setVelocity(dashVector);
        return super.onCast();
    }

}


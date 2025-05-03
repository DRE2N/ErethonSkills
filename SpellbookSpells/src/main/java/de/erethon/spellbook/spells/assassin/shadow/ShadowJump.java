package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ShadowJump extends AssassinBaseSpell {

    // RMB. Dashes forward, becoming invisible for a short time if passing through an entity.
    private final double dashMultiplier = data.getDouble("dashMultiplier", 2.0);
    private final double sideDashStrength = data.getDouble("sideDashStrength", 1.5);

    public ShadowJump(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
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
                inputOffset.add(right.clone().multiply(-sideDashStrength));
            }
            if (input.isRight()) {
                inputOffset.add(right.clone().multiply(sideDashStrength));
            }
        }
        Vector forwardDash = direction.multiply(dashMultiplier);
        // Combine forward dash and side dash (inputOffset)
        Vector dashVector = forwardDash.add(inputOffset);
        caster.setVelocity(dashVector);
        triggerTraits(0);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        caster.getWorld().spawnParticle(Particle.WHITE_ASH, caster.getLocation(), 1);
        for (LivingEntity entity : caster.getLocation().getNearbyLivingEntities(2)) {
            if (entity == caster) {
                continue;
            }
            if (entity.getBoundingBox().overlaps(caster.getBoundingBox())) {
                caster.setInvisible(true);
                break;
            }
        }
    }

    @Override
    protected void onTickFinish() {
        caster.setInvisible(false);
    }
}

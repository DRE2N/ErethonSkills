package de.erethon.spellbook.spells.ranger.beastmaster;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class PullToPet extends RangerPetBaseSpell {

    private final int range = data.getInt("range", 4);
    private final double pullStrength = data.getDouble("pullStrength", 0.5);

    public PullToPet(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Location loc = pet.getLocation();
        CircleEffect effect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        effect.radius = range;
        effect.particle = Particle.DUST.builder().data(new Particle.DustOptions(org.bukkit.Color.RED, 1)).particle();
        effect.wholeCircle = true;
        effect.iterations = 1;
        effect.setLocation(loc);
        for (LivingEntity entity : loc.getNearbyLivingEntities(range)) {
            if (entity == caster) continue;
            if (!Spellbook.canAttack(caster, entity)) continue;
            if (Spellbook.getInstance().isCCImmune(target)) continue;
            entity.setVelocity(loc.toVector().subtract(entity.getLocation().toVector()).multiply(pullStrength));
            entity.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, Sound.Source.PLAYER, 1, 1));
        }
        return true;
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(range, VALUE_COLOR));
        placeholderNames.add("range");
        return super.getPlaceholders(c);
    }
}
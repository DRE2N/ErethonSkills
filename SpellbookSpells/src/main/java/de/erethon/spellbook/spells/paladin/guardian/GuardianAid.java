package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.erethon.spellbook.spells.paladin.inquisitor.InquisitorBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuardianAid extends PaladinBaseSpell {

    private final int range = data.getInt("range", 10);
    private final int resistanceDuration = data.getInt("resistanceDuration", 120);
    private final int stabilityDuration = data.getInt("stabilityDuration", 120);
    private final int resistanceStacks = data.getInt("resistanceStacks", 1);
    private final int stabilityStacks = data.getInt("stabilityStacks", 1);

    private final EffectData resistance = Spellbook.getEffectData("Resistance");
    private final EffectData stability = Spellbook.getEffectData("Stability");

    private final Set<CircleEffect> circleEffects = new HashSet<>();


    public GuardianAid(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Set<LivingEntity> entities = new HashSet<>();
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            if (Spellbook.canAttack(caster, living)) continue;
            entities.add(living);
            living.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_CURSE, Sound.Source.RECORD, 1, 1));
            living.getEffects().forEach(e -> {
                if (!e.data.isPositive()) {
                    living.removeEffect(e.data);
                }
            });
            Location livingLocation = living.getLocation().clone();
            livingLocation.setPitch(0);
            CircleEffect circle = new CircleEffect(Spellbook.getInstance().getEffectManager());
            circle.particle = Particle.END_ROD;
            circle.radius = 1.5f;
            circle.particleCount = 16;
            circle.duration = 60 * 50;
            circle.iterations = -1;
            circle.setLocation(livingLocation);
            circle.start();
            circleEffects.add(circle);
        }
        triggerTraits(entities);
        BukkitRunnable moveCircles = new BukkitRunnable() {
            @Override
            public void run() {
                for (CircleEffect circle : circleEffects) {
                    circle.setLocation(circle.getLocation().add(0, 0.1, 0));
                }
            }
        };
        for (LivingEntity living : entities) {
            living.addEffect(caster, resistance, resistanceDuration, resistanceStacks);
            living.addEffect(caster, stability, stabilityDuration, stabilityStacks);
        }
        moveCircles.runTaskTimer(Spellbook.getInstance().getImplementer(), 0, 1);
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(range, VALUE_COLOR));
        placeholderNames.add("range");
        return super.getPlaceholders(c);
    }
}

package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
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

public class ComprehensiveBlessing extends PaladinBaseSpell {

    private final int range = data.getInt("range", 10);
    private final Set<CircleEffect> circleEffects = new HashSet<>();

    public ComprehensiveBlessing(LivingEntity caster, SpellData spellData) {
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
                    MessageUtil.log("Removed negative " + e.data.getName() + " from " + living.getName());
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

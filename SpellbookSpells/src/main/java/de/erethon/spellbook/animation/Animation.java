package de.erethon.spellbook.animation;

import de.erethon.spellbook.Spellbook;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Animation extends BukkitRunnable {

    private List<AnimationStage> stages = new ArrayList<>();
    private Location location;
    private LivingEntity living;

    private int passedTicks;
    private int delay;
    private int currentStage = 0;
    private AnimationStage activeStage;

    public Animation(AnimationStage... s) {
        stages.addAll(List.of(s));
    }

    public Animation stages(AnimationStage... s) {
        stages.addAll(List.of(s));
        return this;
    }

    public void run(LivingEntity living) {
        this.living = living;
        runTaskTimer(Spellbook.getInstance().getImplementer(), 0, 1);
    }

    public void run(Location location) {
        this.location = location;
        runTaskTimer(Spellbook.getInstance().getImplementer(), 0, 1);
    }

    public void continueAnimation() {
        if (currentStage >= stages.size()) {
            return;
        }
        AnimationStage stage = stages.get(currentStage);
        activeStage = stage;
        if (location != null) {
            stage.run(location);
        } else {
            stage.run(living);
        }
        delay = stage.getStageDelay();
        passedTicks = 0;
        currentStage++;
    }

    @Override
    public void run() {
        if ((passedTicks) >= delay) {
            continueAnimation();
        }
        passedTicks++;
    }

    public void cancel() {
        for (AnimationStage stage : stages) {
            stage.cancel();
        }
    }

    public AnimationStage getActiveStage() {
        return activeStage;
    }
}

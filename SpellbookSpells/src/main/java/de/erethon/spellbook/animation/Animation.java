package de.erethon.spellbook.animation;

import de.erethon.spellbook.Spellbook;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Animation extends BukkitRunnable {

    private List<AnimationStage> stages = new ArrayList<>();
    private final Location location;

    private int passedTicks;
    private int delay;
    private int currentStage = 0;
    private AnimationStage activeStage;

    public Animation(Location location) {
        this.location = location;
        runTaskTimer(Spellbook.getInstance().getImplementer(), 0, 1);
    }

    public void continueAnimation() {
        if (currentStage >= stages.size()) {
            return;
        }
        AnimationStage stage = stages.get(currentStage);
        activeStage = stage;
        stage.run(location);
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

    public AnimationStage getActiveStage() {
        return activeStage;
    }
}

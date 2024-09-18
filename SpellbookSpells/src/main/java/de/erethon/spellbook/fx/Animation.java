package de.erethon.spellbook.fx;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.fx.cues.FXCue;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Animation {

    private final Map<Integer, Set<FXCue>> cues = new HashMap<>();
    private final Map<Integer, Set<FXCue>> runAgain = new HashMap<>();
    private BukkitTask task;

    public Animation addCue(int tick, FXCue... cueList) {
        if (!cues.containsKey(tick)) {
            cues.put(tick, Set.of(cueList));
            return this;
        }
        Set<FXCue> set = cues.get(tick);
        set.addAll(Set.of(cueList));
        cues.put(tick, set);
        return this;
    }

    public void play(LivingEntity trigger) {
        task = new BukkitRunnable() {
            private int currentTick = 0;
            @Override
            public void run() {
                if (cues.containsKey(currentTick)) {
                    Set<FXCue> cueSet = cues.get(currentTick);
                    cueSet.forEach(cue -> cue.run(trigger));
                    for (FXCue cue : cueSet) {
                        if (!cue.isOnceAndDone()) {
                            if (cue.getDelay() > 0) {
                                int nextTick = currentTick + cue.getDelay();
                                runAgain.computeIfAbsent(nextTick, k -> new HashSet<>()).add(cue);
                            }
                        } else {
                            cue.onRemove(trigger);
                        }
                    }
                }
                if (runAgain.containsKey(currentTick)) {
                    Set<FXCue> cueSet = runAgain.get(currentTick);
                    cueSet.forEach(cue -> cue.run(trigger));
                    runAgain.remove(currentTick);
                }
                currentTick++;
                if (currentTick > cues.keySet().size() && runAgain.isEmpty()) {
                    this.cancel();
                    for (Set<FXCue> cueSet : cues.values()) {
                        cueSet.forEach(cue -> cue.onRemove(trigger));
                    }
                }
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1);
    }

    public void stop(LivingEntity livingEntity) {
        task.cancel();
        for (Set<FXCue> cueSet : cues.values()) {
            cueSet.forEach(cue -> cue.onRemove(livingEntity));
        }
        for (Set<FXCue> cueSet : runAgain.values()) {
            cueSet.forEach(cue -> cue.onRemove(livingEntity));
        }
    }
}
package de.erethon.spellbook.animation;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class AnimationStage {

    private Set<AnimationPart> parts = new HashSet<>();
    private int stageDelay = 0;

    public AnimationStage(AnimationPart... p) {
        parts.addAll(Set.of(p));
    }

    public AnimationStage parts(AnimationPart... p) {
        parts.addAll(Set.of(p));
        return this;
    }

    public AnimationStage delay(int delay) {
        stageDelay = delay;
        return this;
    }

    public void run(Location location) {
        if (Spellbook.getInstance().isDebug()) {
            MessageUtil.log("Running animation stage at location with " + parts.size() + " parts.");
        }
        for (AnimationPart part : parts) {
            part.run(location);
        }
    }

    public void run(LivingEntity living) {
        if (Spellbook.getInstance().isDebug()) {
            MessageUtil.log("Running animation stage on entity with " + parts.size() + " parts.");
        }
        for (AnimationPart part : parts) {
            part.run(living);
        }
    }

    public void cancel() {
        for (AnimationPart part : parts) {
            part.removeDisplay();
        }
    }

    public int getStageDelay() {
        return stageDelay;
    }
}

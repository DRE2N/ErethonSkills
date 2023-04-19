package de.erethon.spellbook.animation;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

public class AnimationStage {

    private Set<AnimationPart> parts = new HashSet<>();
    private int stageDelay = 0;

    public AnimationStage addPart(AnimationPart part) {
        parts.add(part);
        return this;
    }

    public AnimationStage setStageDelay(int delay) {
        stageDelay = delay;
        return this;
    }

    public void run(Location location) {
        if (Spellbook.getInstance().isDebug()) {
            MessageUtil.log("Running animation stage with " + parts.size() + " parts.");
        }
        for (AnimationPart part : parts) {
            part.run(location);
        }
    }

    public int getStageDelay() {
        return stageDelay;
    }
}

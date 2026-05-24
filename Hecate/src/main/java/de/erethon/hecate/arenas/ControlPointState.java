package de.erethon.hecate.arenas;

public class ControlPointState {

    private final CapturePointDefinition definition;
    private int owner = -1;
    private int progressTeam = -1;
    private int progressTicks;

    public ControlPointState(CapturePointDefinition definition) {
        this.definition = definition;
    }

    public CapturePointDefinition getDefinition() {
        return definition;
    }

    public int getOwner() {
        return owner;
    }

    public int getProgressTeam() {
        return progressTeam;
    }

    public float getProgress() {
        int requiredTicks = getRequiredTicks();
        if (requiredTicks <= 0) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, progressTicks / (float) requiredTicks));
    }

    public void contest(int team) {
        if (team < 0) {
            progressTicks = Math.max(0, progressTicks - 5);
            return;
        }
        if (progressTeam != team) {
            progressTeam = team;
            progressTicks = 0;
        }
        int requiredTicks = (owner == -1 ? definition.captureSeconds() : definition.neutralizeSeconds()) * 20;
        progressTicks += 5;
        if (progressTicks >= requiredTicks) {
            owner = team;
            progressTicks = 0;
        }
    }

    private int getRequiredTicks() {
        return (owner == -1 ? definition.captureSeconds() : definition.neutralizeSeconds()) * 20;
    }
}

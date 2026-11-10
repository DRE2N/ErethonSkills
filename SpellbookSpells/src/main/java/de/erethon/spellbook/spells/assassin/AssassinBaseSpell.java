package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.utils.SpellbookFeedback;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import de.erethon.spellbook.utils.Targeted;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Display;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class AssassinBaseSpell extends SpellbookBaseSpell implements Targeted {

    private final Set<Display> trackedVfxDisplays = new HashSet<>();
    private final Set<BukkitTask> trackedVfxTasks = new HashSet<>();

    public int duration = data.getInt("duration", 10);
    public int energyCost = data.getInt("energyCost", 0);

    public AssassinBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    protected <T extends Display> T trackVfx(T display) {
        trackedVfxDisplays.add(display);
        return display;
    }

    protected void trackVfx(Iterable<? extends Display> displays) {
        displays.forEach(trackedVfxDisplays::add);
    }

    protected BukkitTask trackVfxTask(BukkitTask task) {
        trackedVfxTasks.add(task);
        return task;
    }

    @Override
    protected void cleanup() {
        trackedVfxTasks.forEach(BukkitTask::cancel);
        trackedVfxTasks.clear();
        trackedVfxDisplays.forEach(display -> {
            if (display != null && display.isValid()) display.remove();
        });
        trackedVfxDisplays.clear();
        cleanupAnimations();
        super.cleanup();
    }

    @Override
    protected boolean onPrecast() {
        return hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        caster.setEnergy(Math.max(0, caster.getEnergy() - energyCost));
        return super.onCast();
    }

    public boolean hasEnergy(LivingEntity caster, SpellData data) {
        boolean canCast = energyCost <= caster.getEnergy();
        if (Spellbook.getInstance().isDebug()) {
            return true;
        }
        if (!canCast) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NO_ENERGY);
        }
        return canCast;
    }

    @Override
    public LivingEntity getTarget() {
        return target;
    }

    @Override
    public void setTarget(LivingEntity target) {
        this.target = target;
    }

}

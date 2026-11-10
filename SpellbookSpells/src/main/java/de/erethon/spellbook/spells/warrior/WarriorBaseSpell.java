package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.utils.SpellbookFeedback;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import de.erethon.spellbook.utils.Targeted;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WarriorBaseSpell extends SpellbookBaseSpell implements Targeted {

    protected final int duration = data.getInt("duration", 0);
    public int rageCost = data.getInt("energyCost", data.getInt("rageCost", 0));
    private final Set<Display> trackedVfxDisplays = new HashSet<>();
    private final Set<BukkitTask> trackedVfxTasks = new HashSet<>();

    public WarriorBaseSpell(LivingEntity caster, SpellData spellData) {
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
            if (display != null && display.isValid()) {
                display.remove();
            }
        });
        trackedVfxDisplays.clear();
        super.cleanup();
    }


    @Override
    protected boolean onPrecast() {
        return isOffCooldown() && hasEnergy(caster, data) && super.onPrecast();
    }

    protected boolean isOffCooldown() {
        Long lastCast = caster.getUsedSpells().get(data);
        if (lastCast != null && System.currentTimeMillis() - lastCast < data.getCooldown() * 1000L) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.ON_COOLDOWN);
            return false;
        }
        return true;
    }

    @Override
    public boolean onCast() {
        caster.setEnergy(Math.max(0, caster.getEnergy() - rageCost));
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        return super.onCast();
    }

    public boolean hasEnergy(LivingEntity caster, SpellData data) {
        boolean canCast = rageCost <= caster.getEnergy();
        if (Spellbook.getInstance().isDebug()) {
            return true;
        }
        if (!canCast) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NO_ENERGY);
        }
        return canCast;
    }

    protected boolean lookForTarget() {
        return super.lookForTarget(data.getInt("range", 32));
    }

    protected boolean lookForTarget(int range) {
        return super.lookForTarget(range);
    }

    protected boolean lookForTarget(boolean friendly) {
        return lookForTarget(friendly, 32);
    }

    protected boolean lookForTarget(boolean friendly, int range) {
        Entity target = caster.getTargetEntity(range);
        if (target == null) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        if (!(target instanceof LivingEntity)) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        if (friendly && Spellbook.canAttack(caster, (LivingEntity) target)) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NOT_FRIENDLY);
            return false;
        }
        this.target = (LivingEntity) target;
        return true;
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

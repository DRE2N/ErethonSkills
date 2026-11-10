package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.utils.SpellbookFeedback;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PaladinBaseSpell extends SpellbookBaseSpell {

    private final Set<Display> trackedVfxDisplays = new HashSet<>();
    private final Set<BukkitTask> trackedVfxTasks = new HashSet<>();

    protected final int duration = data.getInt("duration", 10);
    protected final int energyCost = data.getInt("energyCost", 0);
    protected final int cooldown = data.getInt("cooldown", 5) * 1000;

    public PaladinBaseSpell(LivingEntity caster, SpellData spellData) {
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
        cleanupAnimations();
        super.cleanup();
    }

    @Override
    protected boolean onPrecast() {
        if (caster.getUsedSpells().containsKey(data)) {
            long lastCast = caster.getUsedSpells().get(data);
            if (System.currentTimeMillis() - lastCast < cooldown) {
                SpellbookFeedback.send(caster, SpellbookCommonMessages.ON_COOLDOWN);
                return false;
            }
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        caster.setEnergy(Math.max(0, caster.getEnergy() - energyCost));
        return super.onCast();
    }

    protected boolean lookForTarget() {
        return super.lookForTarget(32);
    }

    protected boolean lookForTarget(int range) {
        return super.lookForTarget(range);
    }

    protected boolean lookForTarget(boolean friendly) {
        return lookForTarget(friendly, 32);
    }

    protected boolean lookForTarget(boolean friendly, int range) {
        Entity target = caster.getTargetEntity(range, true);
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
        if (!caster.hasLineOfSight(target)) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        this.target = (LivingEntity) target;
        return true;
    }

    public boolean hasEnergy(LivingEntity caster, SpellData data) {
        if (Spellbook.getInstance().isDebug()) {
            return true;
        }
        boolean canCast = energyCost <= caster.getEnergy();
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

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        placeholderNames.add("duration");
        spellAddedPlaceholders.add(Component.text(data.getInt("range", 0), VALUE_COLOR));
        placeholderNames.add("range");
        return super.getPlaceholders(c);
    }
}

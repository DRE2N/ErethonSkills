package de.erethon.hecate.ui;

import de.erethon.hecate.Hecate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class EntityStatusDisplay {

    private static final Color NAMEPLATE_BACKGROUND = Color.fromARGB(155, 18, 17, 23);
    private static final float HEALTH_BAR_CLEARANCE = 0.18f;
    private static final TextReplacementConfig MOB_LEVEL = TextReplacementConfig.builder()
            .match("^Lv\\. ([0-9]+) $").once()
            .replacement((match, text) -> Component.translatable("hecate.combat.mob_level", "Lv.")
                    .append(Component.space())
                    .append(Component.text(match.group(1), TextColor.color(0xF0C984), TextDecoration.BOLD))
                    .append(Component.space()))
            .build();

    private final EntityStatusDisplayManager manager = Hecate.getInstance().getStatusDisplayManager();

    public LivingEntity holder;
    public TextDisplay entityNameTag;
    public TextDisplay healthDisplay;
    public TextDisplay statusDisplay;

    private boolean removed = false;
    private float healthBarOffset = Float.NaN;
    private Component lastHealthBar;

    public EntityStatusDisplay(LivingEntity holder) {
        this.holder = holder;
        Location locWithOutRotation = holder.getLocation().clone().setDirection(new Vector());
        locWithOutRotation.setPitch(0);
        locWithOutRotation.setYaw(0);
        holder.getWorld().spawn(locWithOutRotation, TextDisplay.class, textDisplay -> {
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.text(displayName());
            textDisplay.setDefaultBackground(false);
            textDisplay.setBackgroundColor(NAMEPLATE_BACKGROUND);
            textDisplay.getPersistentDataContainer().set(EntityStatusDisplayManager.ENTITY_STATUS_KEY, PersistentDataType.BYTE, (byte) 0);
            textDisplay.setPersistent(false);
            textDisplay.setViewRange(0.1f);
            textDisplay.setDisplayHeight(0.1f);
            textDisplay.setDisplayWidth(0.1f);
            textDisplay.setShadowed(false);
            holder.addPassenger(textDisplay);
            entityNameTag = textDisplay;
        });
        holder.getWorld().spawn(locWithOutRotation, TextDisplay.class, textDisplay -> {
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            lastHealthBar = getHealth(holder.getHealth(), holder.getMaxHealth());
            textDisplay.text(lastHealthBar);
            textDisplay.setBackgroundColor(Color.fromARGB(0, 1,1,1));
            textDisplay.getPersistentDataContainer().set(EntityStatusDisplayManager.ENTITY_STATUS_KEY, PersistentDataType.BYTE, (byte) 1);
            textDisplay.setPersistent(false);
            textDisplay.setViewRange(0.1f);
            textDisplay.setDisplayHeight(0.1f);
            textDisplay.setDisplayWidth(0.1f);
            textDisplay.setShadowed(false);
            holder.addPassenger(textDisplay);
            healthDisplay = textDisplay;
        });
        holder.getWorld().spawn(locWithOutRotation, TextDisplay.class, textDisplay -> {
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setBackgroundColor(Color.fromARGB(0, 1,1,1));
            textDisplay.text(Component.empty());
            textDisplay.getPersistentDataContainer().set(EntityStatusDisplayManager.ENTITY_STATUS_KEY, PersistentDataType.BYTE, (byte) 1);
            textDisplay.setPersistent(false);
            textDisplay.setViewRange(0.1f);
            textDisplay.setDisplayHeight(0.1f);
            textDisplay.setDisplayWidth(0.1f);
            textDisplay.setShadowed(false);
            holder.addPassenger(textDisplay);
            statusDisplay = textDisplay;
        });
        updateLayout();
        if (holder instanceof Player player) {
            player.hideEntity(Hecate.getInstance(), statusDisplay);
            player.hideEntity(Hecate.getInstance(), healthDisplay);
            player.hideEntity(Hecate.getInstance(), entityNameTag);
        } else {
            holder.setCustomNameVisible(false);
        }
    }

    void updateLayout() {
        // Aether can detach the displays and supply an anchor above a custom model.
        float offset = 0f;
        if (holder.equals(healthDisplay.getVehicle())) {
            var entity = ((CraftEntity) holder).getHandle();
            var rider = ((CraftEntity) healthDisplay).getHandle();
            double ridingY = entity.getPassengerRidingPosition(rider).y - rider.getVehicleAttachmentPoint(entity).y;
            offset = (float) (entity.getBoundingBox().maxY - ridingY) + HEALTH_BAR_CLEARANCE;
        }
        if (Float.compare(healthBarOffset, offset) == 0) {
            return;
        }
        healthBarOffset = offset;
        position(healthDisplay, offset, 0.65f);
        position(statusDisplay, offset + 0.17f, 0.4f);
        position(entityNameTag, offset + 0.31f, 0.5f);
    }

    private void position(TextDisplay display, float y, float scale) {
        display.setTransformation(new Transformation(new Vector3f(0, y, 0),
                new AxisAngle4f(), new Vector3f(scale), new AxisAngle4f()));
    }

    public void updateDisplayName() {
        entityNameTag.text(displayName());
    }

    private Component displayName() {
        Component name = holder.teamDisplayName();
        return holder instanceof Player ? name : name.replaceText(MOB_LEVEL);
    }

    void updateHealthDisplay() {
        if (removed || !holder.isValid()) {
            return;
        }
        Component bar = getHealth(holder.getHealth(), holder.getMaxHealth());
        if (lastHealthBar != bar) {
            healthDisplay.text(bar);
            lastHealthBar = bar;
        }
    }

    public void updateStatusDisplay() {
        if (holder.getEffects().isEmpty()) {
            statusDisplay.text(Component.empty());
            manager.unmarkForTicking(this);
            return;
        }
        if (holder.isChanneling()) {
            return;
        }
        Component[] formatted = EffectDisplayFormatter.formatEffects(holder.getEffects());
        statusDisplay.text(EffectDisplayFormatter.combineEffects(formatted[0], formatted[1]));
        manager.markForTicking(this);
    }

    public void showText(Component component, int duration) {
        statusDisplay.text(component);
        new BukkitRunnable() {
            @Override
            public void run() {
                updateStatusDisplay();
            }
        }.runTaskLater(Hecate.getInstance(), duration);
    }

    public void onTick() {
        if (removed || holder.isDead() || !holder.isValid()) {
            manager.unmarkForTicking(this);
            manager.removeStatusDisplay(holder);
            return;
        }
        updateStatusDisplay();
    }

    public Component getHealth(double currentHealth, double maxHealth) {
        double healthPercentage = maxHealth > 0 ? currentHealth / maxHealth : 0.0;
        return PrecomputedHealthDisplay.getComponentAt(healthPercentage);
    }

    public void updateChannelProgress(int current, int max) {
        if (!holder.isChanneling()) {
            return;
        }
        Component channelText = PrecomputedChannelDisplay.getComponentAt(current, max);
        statusDisplay.text(channelText);
    }

    public void remove() {
        statusDisplay.remove();
        entityNameTag.remove();
        healthDisplay.remove();
        removed = true;
    }
}

package de.erethon.hecate.ui;

import de.erethon.hecate.Hecate;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
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

    private final EntityStatusDisplayManager manager = Hecate.getInstance().getStatusDisplayManager();

    public LivingEntity holder;
    public TextDisplay entityNameTag;
    public TextDisplay healthDisplay;
    public TextDisplay statusDisplay;

    private boolean removed = false;

    public EntityStatusDisplay(LivingEntity holder) {
        this.holder = holder;
        Location locWithOutRotation = holder.getLocation().clone().setDirection(new Vector());
        locWithOutRotation.setPitch(0);
        locWithOutRotation.setYaw(0);
        holder.getWorld().spawn(locWithOutRotation, TextDisplay.class, textDisplay -> {
            Transformation nameTagTransform = new Transformation(new Vector3f(0, (float) Math.max(0.4f, holder.getHeight() - 1.6f), 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(0.9f, 0.9f, 0.9f), new AxisAngle4f(0, 0, 0, 0));
            textDisplay.setTransformation(nameTagTransform);
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.text(holder.teamDisplayName());
            textDisplay.setBackgroundColor(Color.fromARGB(0, 1,1,1));
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
            Transformation healthDisplayTransformation = new Transformation(new Vector3f(0, (float) Math.max(0.0f, holder.getHeight() - 2f), 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(0.2f, 3f, 0.4f), new AxisAngle4f(0, 0, 0, 0));
            textDisplay.setTransformation(healthDisplayTransformation);
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.text(getHealth(holder.getHealth(), holder.getMaxHealth()));
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
            Transformation statusDisplayTransform = new Transformation(new Vector3f(0, (float) Math.max(0.2f, holder.getHeight() - 2f), 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f), new AxisAngle4f(0, 0, 0, 0));
            textDisplay.setTransformation(statusDisplayTransform);
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
        if (holder instanceof Player player) {
            player.hideEntity(Hecate.getInstance(), statusDisplay);
            player.hideEntity(Hecate.getInstance(), healthDisplay);
            player.hideEntity(Hecate.getInstance(), entityNameTag);
        }
    }

    public void updateDisplayName() {
        entityNameTag.text(holder.teamDisplayName());
    }

    public void updateHealthDisplay(double damage) {
        healthDisplay.text(getHealth(Math.max(0, holder.getHealth() - damage), holder.getMaxHealth()));
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
        healthDisplay.text(getHealth(Math.max(0, holder.getHealth()), holder.getMaxHealth()));
        updateStatusDisplay();
    }

    public Component getHealth(double currentHealth, double maxHealth) {
        double healthPercentage = currentHealth / maxHealth;
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

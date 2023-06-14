package de.erethon.hecate.ui;

import de.erethon.spellbook.api.SpellEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

public class EntityStatusDisplay {

    LivingEntity holder;
    TextDisplay entityNameTag;
    TextDisplay healthDisplay;
    TextDisplay statusDisplay;

    public EntityStatusDisplay(LivingEntity holder) {
        this.holder = holder;
        holder.getWorld().spawn(holder.getLocation(), TextDisplay.class, textDisplay -> {
            Transformation nameTagTransform = new Transformation(new Vector3f(0, (float) Math.max(0.8f, holder.getHeight() - 1.2f), 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(0.9f, 0.9f, 0.9f), new AxisAngle4f(0, 0, 0, 0));
            textDisplay.setTransformation(nameTagTransform);
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.text(holder.teamDisplayName());
            textDisplay.setBackgroundColor(Color.fromARGB(0, 1,1,1));
            textDisplay.getPersistentDataContainer().set(EntityStatusDisplayManager.ENTITY_STATUS_KEY, PersistentDataType.BYTE, (byte) 0);
            holder.addPassenger(textDisplay);
            entityNameTag = textDisplay;
        });
        holder.getWorld().spawn(holder.getLocation(), TextDisplay.class, textDisplay -> {
            Transformation healthDisplayTransformation = new Transformation(new Vector3f(0, (float) Math.max(0.3f, holder.getHeight() - 2f), 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(0.2f, 3f, 0.4f), new AxisAngle4f(0, 0, 0, 0));
            textDisplay.setTransformation(healthDisplayTransformation);
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.text(getHealth(holder.getHealth(), holder.getMaxHealth()));
            textDisplay.setBackgroundColor(Color.fromARGB(0, 1,1,1));
            textDisplay.getPersistentDataContainer().set(EntityStatusDisplayManager.ENTITY_STATUS_KEY, PersistentDataType.BYTE, (byte) 1);
            holder.addPassenger(textDisplay);
            healthDisplay = textDisplay;
        });
        holder.getWorld().spawn(holder.getLocation(), TextDisplay.class, textDisplay -> {
            Transformation statusDisplayTransform = new Transformation(new Vector3f(0, (float) Math.max(0.6f, holder.getHeight() - 1.4f), 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f), new AxisAngle4f(0, 0, 0, 0));
            textDisplay.setTransformation(statusDisplayTransform);
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setBackgroundColor(Color.fromARGB(0, 1,1,1));
            textDisplay.text(Component.empty());
            textDisplay.getPersistentDataContainer().set(EntityStatusDisplayManager.ENTITY_STATUS_KEY, PersistentDataType.BYTE, (byte) 1);
            holder.addPassenger(textDisplay);
            statusDisplay = textDisplay;
        });
    }

    public void updateDisplayName() {
        entityNameTag.text(holder.teamDisplayName());
    }

    public void updateHealthDisplay() {
        healthDisplay.text(getHealth(holder.getHealth(), holder.getMaxHealth()));
    }

    public void updateStatusDisplay() {
        List<SpellEffect> positiveEffects = holder.getEffects().stream().filter(effect -> effect.data.isPositive()).toList();
        List<SpellEffect> negativeEffects = holder.getEffects().stream().filter(effect -> !effect.data.isPositive()).toList();
        StringBuilder positiveString = new StringBuilder();
        StringBuilder negativeString = new StringBuilder();
        for (SpellEffect effect : positiveEffects) {
            positiveString.append(effect.data.getIcon()).append(" ");
        }
        for (SpellEffect effect : negativeEffects) {
            negativeString.append(effect.data.getIcon()).append(" ");
        }
        Component positives = Component.text(positiveString.toString()).color(NamedTextColor.GREEN);
        if (positiveEffects.isEmpty()) {
            positives = Component.empty();
        }
        Component negatives = Component.text(negativeString.toString()).color(NamedTextColor.RED);
        if (negativeEffects.isEmpty()) {
            negatives = Component.empty();
        }
        Component spacer = Component.text(" | ").color(NamedTextColor.DARK_GRAY);
        if (positiveEffects.isEmpty() && negativeEffects.isEmpty()) {
            statusDisplay.text(Component.empty());
            return;
        }
        statusDisplay.text(positives.append(spacer).append(negatives));
    }

    public Component getHealth(double currentHealth, double maxHealth) {
        double healthPercentage = currentHealth / maxHealth;
        return PrecomputedHealthDisplay.getComponentAt(healthPercentage);
    }

    public void remove() {
        statusDisplay.remove();
        entityNameTag.remove();
        healthDisplay.remove();
    }
}

package de.erethon.hecate.ui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.SpellEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;

public class EntityStatusDisplay {

    LivingEntity holder;
    TextDisplay entityNameTag;
    TextDisplay statusDisplay;

    public EntityStatusDisplay(LivingEntity holder) {
        this.holder = holder;
        holder.getWorld().spawn(holder.getLocation(), TextDisplay.class, textDisplay -> {
            Transformation nameTagTransform = new Transformation(new Vector3f(0, (float) Math.max(0.8f, holder.getHeight() - 1.2f), 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(1f, 1f, 1f), new AxisAngle4f(0, 0, 0, 0));
            textDisplay.setTransformation(nameTagTransform);
            textDisplay.setAlignment(TextDisplay.TextAligment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.text(holder.teamDisplayName());
            textDisplay.getPersistentDataContainer().set(EntityStatusDisplayManager.statusKey, PersistentDataType.BYTE, (byte) 0);
            holder.addPassenger(textDisplay);
            entityNameTag = textDisplay;
        });
        holder.getWorld().spawn(holder.getLocation(), TextDisplay.class, textDisplay -> {
            Transformation statusDisplayTransform = new Transformation(new Vector3f(0, (float) Math.max(0.6f, holder.getHeight() - 1.4f), 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f), new AxisAngle4f(0, 0, 0, 0));
            textDisplay.setTransformation(statusDisplayTransform);
            textDisplay.setAlignment(TextDisplay.TextAligment.CENTER);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setTextOpacity((byte) 64);
            textDisplay.text(Component.empty());
            textDisplay.getPersistentDataContainer().set(EntityStatusDisplayManager.statusKey, PersistentDataType.BYTE, (byte) 1);
            holder.addPassenger(textDisplay);
            statusDisplay = textDisplay;
        });
    }

    public void updateDisplayName() {
        entityNameTag.text(holder.teamDisplayName());
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
            return;
        }
        statusDisplay.text(positives.append(spacer).append(negatives));
    }

    public void remove() {
        statusDisplay.remove();
        entityNameTag.remove();
    }
}

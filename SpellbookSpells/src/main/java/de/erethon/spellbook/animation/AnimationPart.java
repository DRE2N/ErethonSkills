package de.erethon.spellbook.animation;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.slikey.effectlib.Effect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnimationPart {

    private ItemDisplay display;
    private ItemStack itemStack;
    private Set<Effect> effects = new HashSet<>();
    private int duration = 20;
    private int customModelData = -1;

    public AnimationPart(int model, int duration) {
        this.duration = duration;
        this.customModelData = model;
        itemStack = new ItemStack(Material.POTION);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setCustomModelData(model);
        itemStack.setItemMeta(meta);
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), this::removeDisplay, duration);
    }

    public AnimationPart(int duration, Effect... effect) {
        effects.addAll(Set.of(effect));
    }

    public AnimationPart effects(Effect... effect) {
        effects.addAll(Set.of(effect));
        return this;
    }

    public AnimationPart model(int model) {
        this.customModelData = model;
        itemStack = new ItemStack(Material.POTION);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setCustomModelData(model);
        itemStack.setItemMeta(meta);
        return this;
    }

    public void run(Location location) {
        for (Effect effect : effects) {
            effect.setLocation(location);
            effect.duration = duration * 50;
            effect.start();
        }
        if (customModelData == -1) {
            if (Spellbook.getInstance().isDebug()) {
                MessageUtil.log("No model set for animation part.");
            }
            return;
        }
        display = location.getWorld().spawn(location, ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(itemStack);
            itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            itemDisplay.setBillboard(Display.Billboard.VERTICAL);
        });
        if (Spellbook.getInstance().isDebug()) {
            MessageUtil.log("Spawned new item display with model " + customModelData + " at " + location);
        }
    }

    public void run(LivingEntity living) {
        for (Effect effect : effects) {
            effect.setEntity(living);
            effect.duration = duration * 50;
            effect.start();
        }
        if (customModelData == -1) {
            if (Spellbook.getInstance().isDebug()) {
                MessageUtil.log("No model set for animation part.");
            }
            return;
        }
        display = living.getWorld().spawn(living.getLocation(), ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(itemStack);
            itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            itemDisplay.setBillboard(Display.Billboard.VERTICAL);
            living.addPassenger(itemDisplay);
        });
        if (Spellbook.getInstance().isDebug()) {
            MessageUtil.log("Spawned new item display with model " + customModelData + " on entity " + living.getName());
        }
    }

    public void removeDisplay() {
        if (display == null) {
            return;
        }
        display.remove();
    }
}

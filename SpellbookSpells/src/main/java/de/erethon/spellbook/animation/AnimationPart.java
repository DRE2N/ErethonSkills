package de.erethon.spellbook.animation;

import de.erethon.spellbook.Spellbook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class AnimationPart {

    private ItemDisplay display;
    private PotionData potionData = new PotionData(PotionType.AWKWARD);
    private int duration = 20;
    private int customModelData = 1;

    public AnimationPart(int model, int duration, Location location) {
        this.customModelData = model;
        this.duration = duration;
        run(location);
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), this::removeDisplay, duration);
    }

    public void run(Location location) {
        display = location.getWorld().spawn(location, ItemDisplay.class, itemDisplay -> {
            ItemStack potionItem = new ItemStack(Material.POTION);
            PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();
            potionMeta.setBasePotionData(potionData);
            itemDisplay.setItemStack(potionItem);
        });
    }

    private void removeDisplay() {
        display.remove();
    }
}

package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.user.LoadableUser;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import de.erethon.spellbook.spells.SpellData;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HPlayer extends EConfig implements LoadableUser {

    public static final int CONFIG_VERSION = 1;

    private Player player;
    private final SpellCaster caster;
    private Set<SpellData> unlockedSpellData = new HashSet<>();
    private final SpellData[] assignedSlots = new SpellData[8];

    private int level;
    private double xp;

    private HClass hClass;

    private boolean isInCastmode = false;

    private final List<ItemStack> hotbarItems = new ArrayList<>();
    private int hotbarSlot = 0;



    public HPlayer(Spellbook spellbook, Player player) {
        super(HPlayerCache.getPlayerFile(player), CONFIG_VERSION);
        this.player = player;
        this.caster = new SpellCaster(spellbook, player);
    }

    public void levelUp(double overflowXp) {
        level++;
        xp = overflowXp > 0 ? overflowXp : 0;
        if (hClass.getAttributesPerLevel(level) != null) {
            for (Map.Entry<Attribute, Double> entry : hClass.getAttributesPerLevel(level).entrySet()) {
                if (player.getAttribute(entry.getKey()) == null) {
                    player.registerAttribute(entry.getKey());
                }
                player.getAttribute(entry.getKey()).setBaseValue(player.getAttribute(entry.getKey()).getBaseValue() + entry.getValue());
            }
        }
        if (hClass.getSpellsUnlockedAtLevel(level) != null) {
            // Set spells instead of adding them, so that it is possible to replace spells with more powerful versions.
            unlockedSpellData = hClass.getSpellsUnlockedAtLevel(level);
            updateSlots();
        }

    }

    private void checkLevel() {
        if (level < hClass.getMaxLevel()) {
            if (xp >= hClass.getXpForLevel(level + 1)) {
                levelUp(xp - hClass.getXpForLevel(level + 1));
            }
        }
    }

    public void updateSlots() {
        // Do something about spells getting replaced due to levelups
    }

    public void switchMode() {
        MessageUtil.sendMessage(player, "Castmode: " + isInCastmode);
        if (!isInCastmode) {
            hotbarItems.clear();
            PlayerInventory inventory = player.getInventory();
            hotbarSlot = inventory.getHeldItemSlot();
            inventory.setHeldItemSlot(0);
            for (int slot = 0; slot < 8; slot++) {
                hotbarItems.add(slot, inventory.getItem(slot)); // TODO: Put this into PDC or something so it never gets lost, ever.
            }
            // Cooldowns are per material for some reason.
            inventory.setItem(0, new ItemStack(Material.BLACK_DYE));
            inventory.setItem(1, new ItemStack(Material.BLUE_DYE));
            inventory.setItem(2, new ItemStack(Material.BROWN_DYE));
            inventory.setItem(3, new ItemStack(Material.CYAN_DYE));
            inventory.setItem(4, new ItemStack(Material.GRAY_DYE));
            inventory.setItem(5, new ItemStack(Material.GREEN_DYE));
            inventory.setItem(6, new ItemStack(Material.LIGHT_BLUE_DYE));
            inventory.setItem(7, new ItemStack(Material.LIGHT_GRAY_DYE));
            isInCastmode = true;
        } else {
            PlayerInventory inventory = player.getInventory();
            inventory.setHeldItemSlot(hotbarSlot);
            for (int slot = 0; slot < 8; slot++) {
                inventory.setItem(slot, hotbarItems.get(slot));
            }
            isInCastmode = false;
        }
        MessageUtil.sendMessage(player, "Switched to castmode: " + isInCastmode);
    }

    public void update() {
        if (!isInCastmode) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 1; slot < 8; slot++) { // Start at 1, 0 is weapon
            if (inventory.getItem(slot) == null || getSpellAt(slot) == null) {
                continue;
            }
            SpellData spellData = getSpellAt(slot);
            inventory.getItem(slot).setAmount(caster.calculateCooldown(spellData));
            if (spellData.getCooldown() == caster.calculateCooldown(spellData)) {
                player.setCooldown(inventory.getItem(slot).getType(), (caster.calculateCooldown(getSpellAt(slot)) * 20) - 15); // its slightly offset visually
            }
        }
    }

    public boolean isInCastmode() {
        return isInCastmode;
    }

    /* EConfig methods */

    @Override
    public void load() {
        Spellbook spellbook = Hecate.getInstance().getSpellbook();
        for (String spellId : config.getStringList("unlockedSpells")) {
            SpellData spellData = spellbook.getLibrary().getSpellByID(spellId);
            if (spellData == null) {
                MessageUtil.log("Unknown spell '" + spellId + "' found under 'unlockedSlots'");
                continue;
            }
            unlockedSpellData.add(spellData);
        }
        List<String> slotList = config.getStringList("assignedSlots");
        if (slotList.size() > 8) {
            MessageUtil.log("Illegal amount of slots assigned");
            return;
        }
        for (int i = 0; i < slotList.size(); i++) {
            String spellId = slotList.get(i);
            SpellData spellData = spellbook.getLibrary().getSpellByID(spellId);
            if (spellData == null) {
                MessageUtil.log("Unknown spell '" + spellId + "' found under 'assignedSlots'");
                continue;
            }
            assignedSlots[i] = spellData;
        }
        xp = config.getDouble("xp", 0);
        level = config.getInt("level", 1);
    }

    /* LoadableUser methods */

    @Override
    public void updatePlayer(Player player) {
        this.player = player;
    }

    @Override
    public void saveUser() {
        config.set("unlockedSpells", unlockedSpellData.stream().map(SpellData::getId).collect(Collectors.toList()));
        config.set("assignedSlots", Arrays.stream(assignedSlots).map(s -> s == null ? "empty" : s.getId()).collect(Collectors.toList()));
        config.set("level", level);
        config.set("xp", xp);
        save();
    }

    /* getter and setter */

    public Player getPlayer() {
        return player;
    }

    public SpellCaster getCaster() {
        return caster;
    }

    public Set<SpellData> getUnlockedSpells() {
        return unlockedSpellData;
    }

    public SpellData getSpellAt(int slot) {
        return assignedSlots[slot];
    }

    public SpellData[] getAssignedSlots() {
        return assignedSlots;
    }

    public int getLevel() {
        return level;
    }

    public double getXp() {
        return xp;
    }

    public void learnSpell(SpellData spellData) {
        unlockedSpellData.add(spellData);
        updateSlots();
    }

    public void learnSpell(SpellData spellData, int slot) {
        unlockedSpellData.add(spellData);
        assignedSlots[slot] = spellData;
        updateSlots();
    }


    public void addXp(double amount) {
        xp += amount;
        checkLevel();
    }
}

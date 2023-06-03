package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.user.LoadableUser;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.events.CombatModeEnterEvent;
import de.erethon.hecate.events.CombatModeLeaveEvent;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookAPI;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import io.netty.util.concurrent.CompleteFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HPlayer extends EConfig implements LoadableUser {

    public static final int CONFIG_VERSION = 1;

    private Player player;

    private int level = 1;
    private double xp = 0;

    private HClass hClass;
    private final SpellData[] assignedSlots = new SpellData[8];
    private final Set<TraitData> combatOnlyTraits = new HashSet<>();

    private boolean isInCastmode = false;
    private int hotbarSlot = 0;


    MiniMessage miniMessage = MiniMessage.miniMessage();

    public HPlayer(Player player) {
        super(HPlayerCache.getPlayerFile(player), CONFIG_VERSION);
        this.player = player;
        load();
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
            player.getUnlockedSpells().addAll(hClass.getSpellsUnlockedAtLevel(level));
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
        updateDescriptions();
        // Do something about spells getting replaced due to levelups
    }

    public void switchMode(CombatModeReason reason) {
        if (!isInCastmode) {
            PlayerInventory inventory = player.getInventory();
            hotbarSlot = inventory.getHeldItemSlot();
            inventory.setHeldItemSlot(0);
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
            updateDescriptions();
            for (TraitData trait : combatOnlyTraits) {
                player.addTrait(trait);
            }
            CombatModeEnterEvent event = new CombatModeEnterEvent(player, this, reason);
            Bukkit.getPluginManager().callEvent(event);
        } else {
            PlayerInventory inventory = player.getInventory();
            inventory.setHeldItemSlot(hotbarSlot);
            isInCastmode = false;
            for (TraitData trait : combatOnlyTraits) {
                player.removeTrait(trait);
            }
            CombatModeLeaveEvent event = new CombatModeLeaveEvent(player, this, reason);
            Bukkit.getPluginManager().callEvent(event);
        }
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
            inventory.getItem(slot).setAmount(player.calculateCooldown(spellData));
            if (spellData.getCooldown() == player.calculateCooldown(spellData)) {
                player.setCooldown(inventory.getItem(slot).getType(), (player.calculateCooldown(getSpellAt(slot)) * 20) - 15); // its slightly offset visually
            }
        }
        StringBuilder positive = new StringBuilder("<green>");
        StringBuilder negative = new StringBuilder("<red>");
        int health = (int) player.getHealth();
        int maxHealth = (int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        for (SpellEffect effect : player.getEffects()) {
            if (effect.data.isPositive()) {
                positive.append(effect.data.getIcon()).append(" ");
            } else {
                negative.append(effect.data.getIcon()).append(" ");
            }
        }
        Component component = miniMessage.deserialize("<dark_red>" + health + "<dark_gray>/<dark_red>" + maxHealth + " <<<    <green>+" + positive + " <dark_gray>| <red>-" + negative + "  <yellow>>>> " + player.getEnergy() + "<dark_gray>/<yellow>" + player.getMaxEnergy());
        player.sendActionBar(component);
    }

    public void updateDescriptions() {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 1; slot < 8; slot++) { // Start at 1, 0 is weapon
            if (inventory.getItem(slot) == null || getSpellAt(slot) == null) {
                continue;
            }
            ItemStack item = inventory.getItem(slot);
            ItemMeta meta = item.getItemMeta();
            SpellData spellData = getSpellAt(slot);
            meta.lore(spellData.getDescription());
            meta.displayName(spellData.getDisplayName());
            item.setItemMeta(meta);
        }
    }

    public boolean isInCastmode() {
        return isInCastmode;
    }

    public CompletableFuture<Boolean> saveInventory() {
        Base64.Encoder encoder = Base64.getEncoder();
        return CompletableFuture.supplyAsync(() -> {
            try { // Futures swallow exceptions, we don't want that
                File file = new File(Hecate.getInstance().getDataFolder(), "inventories/" + player.getUniqueId() + ".yml");
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                PlayerInventory inventory = player.getInventory();
                List<String> inventoryContents = new ArrayList<>();
                for (ItemStack item : inventory.getContents()) {
                    if (item == null) {
                        inventoryContents.add("empty");
                        continue;
                    }
                    byte[] bytes = item.serializeAsBytes();
                    inventoryContents.add(encoder.encodeToString(bytes));
                }
                config.set("active", true);
                config.set("inventory", inventoryContents);
                try {
                    config.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                inventory.clear();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> loadInventory() {
        ItemStack[] newInventory = new ItemStack[player.getInventory().getSize()];
        Base64.Decoder decoder = Base64.getDecoder();
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = new File(Hecate.getInstance().getDataFolder(), "inventories/" + player.getUniqueId() + ".yml");
                if (!file.exists()) {
                    return false;
                }
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.getBoolean("active")) {
                    return false;
                }
                List<String> inventoryContents = config.getStringList("inventory");
                int i = 0;
                for (String string : inventoryContents) {
                    if (string.equals("empty")) {
                        newInventory[i] = new ItemStack(Material.AIR);
                        i++;
                        continue;
                    }
                    byte[] bytes = decoder.decode(string);
                    newInventory[i] = ItemStack.deserializeBytes(bytes);
                    i++;
                }
                player.getInventory().setContents(newInventory);
                config.set("active", false);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /* EConfig methods */

    @Override
    public void load() {
        SpellbookAPI spellbook = Bukkit.getServer().getSpellbookAPI();
        for (String spellId : config.getStringList("unlockedSpells")) {
            SpellData spellData = spellbook.getLibrary().getSpellByID(spellId);
            if (spellData == null) {
                if (!spellId.equals("empty")) MessageUtil.log("Unknown spell '" + spellId + "' found under 'unlockedSlots'");
                continue;
            }
            player.addSpell(spellData);
        }
        for (String spellId : config.getStringList("passiveSpells")) {
            SpellData spellData = spellbook.getLibrary().getSpellByID(spellId);
            if (spellData == null) {
                if (!spellId.equals("empty")) MessageUtil.log("Unknown spell '" + spellId + "' found under 'passiveSpells'");
                continue;
            }
            player.addPassiveSpell(spellData.getActiveSpell(player.getPlayer()));
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
                if (!spellId.equals("empty")) MessageUtil.log("Unknown spell '" + spellId + "' found under 'assignedSlots'");
                continue;
            }
            assignedSlots[i] = spellData;
        }
        xp = config.getDouble("xp", 0);
        level = config.getInt("level", 1);
        player.setMaxEnergy(config.getInt("maxEnergy", 100));
        player.setEnergy(config.getInt("energy", 50));
    }

    /* LoadableUser methods */

    @Override
    public void updatePlayer(Player player) {
        this.player = player;
    }

    @Override
    public void saveUser() {
        config.set("unlockedSpells", player.getUnlockedSpells().stream().map(SpellData::getId).collect(Collectors.toList()));
        config.set("passiveSpells", player.getPassiveSpells().stream().map(SpellbookSpell::getId).collect(Collectors.toList()));
        config.set("assignedSlots", Arrays.stream(assignedSlots).map(s -> s == null ? "empty" : s.getId()).collect(Collectors.toList()));
        config.set("level", level);
        config.set("xp", xp);
        config.set("maxEnergy", player.getMaxEnergy());
        config.set("energy", player.getEnergy());
        save();
        MessageUtil.log("User " + player.getName() + " saved!");
    }

    /* getter and setter */

    public Player getPlayer() {
        return player;
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

    public HClass gethClass() {
        return hClass;
    }

    public void sethClass(HClass hClass) {
        this.hClass = hClass;
    }

    public boolean hasTrait(TraitData data) {
        return player.hasTrait(data);
    }

    public void addCombatOnlyTrait(TraitData data) {
        combatOnlyTraits.add(data);
    }

    public void removeCombatOnlyTraits(TraitData data) {
        combatOnlyTraits.remove(data);
    }

    public void learnSpell(SpellData spellData) {
        player.addSpell(spellData);
        MessageUtil.log(player.getName() + " learned spell " + spellData.getId());
        updateSlots();
    }

    public void learnSpell(SpellData spellData, int slot) {
        player.addSpell(spellData);
        assignedSlots[slot] = spellData;
        MessageUtil.log(player.getName() + " learned spell " + spellData.getId() + " at slot " + slot);
        updateSlots();
    }


    public void addXp(double amount) {
        xp += amount;
        checkLevel();
    }

}

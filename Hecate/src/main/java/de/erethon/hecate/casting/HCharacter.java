package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.events.CombatModeEnterEvent;
import de.erethon.hecate.events.CombatModeLeaveEvent;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.util.SpellbookTranslator;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.TraitData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class HCharacter {

    private final static int WEAPON_SLOT = 8;

    private final HPlayer hPlayer;
    private final Player player;
    private final ServerPlayer serverPlayer;
    private final int characterID;

    private int level = 1;
    private double xp = 0;
    private int energy;
    private int maxEnergy;
    private final Set<SpellData> unlockedSpells = new HashSet<>();

    private HClass hClass = Hecate.getInstance().getHClass("Assassin");
    private Traitline selectedTraitline;
    private SpellData[] assignedSlots = new SpellData[WEAPON_SLOT];
    private final Set<TraitData> combatOnlyTraits = new HashSet<>();

    private boolean isInCastmode = false;
    private int hotbarSlot = 0;

    MiniMessage miniMessage = MiniMessage.miniMessage();

    public HCharacter(HPlayer hPlayer, int characterID) {
        this.hPlayer = hPlayer;
        this.player = hPlayer.getPlayer();
        CraftPlayer craftPlayer = (CraftPlayer) player;
        this.serverPlayer = craftPlayer.getHandle();
        this.characterID = characterID;
        this.selectedTraitline = hClass.getDefaultTraitline();
        selectedTraitline.defaultSpellSlots.toArray(new SpellData[8]);
        player.setMaxEnergy(maxEnergy);
        player.setEnergy(energy);
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
        PlayerInventory inventory = player.getInventory();
        if (!isInCastmode) {
            setClassAttributes(hClass);
            hotbarSlot = inventory.getHeldItemSlot();
            inventory.setHeldItemSlot(WEAPON_SLOT);
            // Cooldowns are per material for some reason.
            isInCastmode = true;
            updateDescriptions();
            for (TraitData trait : combatOnlyTraits) {
                player.addTrait(trait);
            }
            CombatModeEnterEvent event = new CombatModeEnterEvent(player, this, reason);
            Bukkit.getPluginManager().callEvent(event);
        } else {
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
        for (int slot = 0; slot < WEAPON_SLOT; slot++) {
            if (inventory.getItem(slot) == null || getSpellAt(slot) == null) {
                continue;
            }
            SpellData spellData = getSpellAt(slot);
            int cdAmount = player.calculateCooldown(spellData);
            inventory.getItem(slot).setAmount(Math.max(1, Math.min(99, cdAmount))); // ItemStack codec fails otherwise
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
        for (int slot = 0; slot < WEAPON_SLOT; slot++) {
            if (getSpellAt(slot) == null) {
                continue;
            }
            SpellData spellData = getSpellAt(slot);
            List<Component> arguments = spellData.getActiveSpell(player).getPlaceholders(player);
            ItemStack item = new ItemStack(SpellbookTranslator.SPELL_ICONS[slot]);
            ItemMeta meta = item.getItemMeta();
            Component name = Component.translatable("spellbook.spell.name." + spellData.getId());
            meta.displayName(Component.text().append(name).color(gethClass().getColor()).decoration(TextDecoration.BOLD, true).build());
            List<Component> lore = new ArrayList<>();
            for (int i = 0; i < spellData.getDescriptionLineCount(); i++) {
                lore.add(Component.translatable("spellbook.spell.description." + spellData.getId() + "." + i, "", arguments));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    public boolean isInCastmode() {
        return isInCastmode;
    }

    public CompletableFuture<Boolean> saveInventory() {
        Base64.Encoder encoder = Base64.getEncoder();
        return CompletableFuture.supplyAsync(() -> {
            try { // Futures swallow exceptions, we don't want that
                File file = new File(Hecate.getInstance().getDataFolder(), "inventories/" + player.getUniqueId() + "_" + hPlayer.getSelectedCharacterID() +".yml");
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
                File file = new File(Hecate.getInstance().getDataFolder(), "inventories/" + player.getUniqueId() + "_" + hPlayer.getSelectedCharacterID() + ".yml");
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
                config.save(file);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public void setClassAttributes(HClass hClass) {
        if (hClass.getAttributesPerLevel(level) == null || hClass.getAttributesPerLevel(level).isEmpty()) { // We don't need to increase attributes every level
            return;
        }
        for (Map.Entry<Attribute, Double> entry : hClass.getAttributesPerLevel(level).entrySet()) {
            MessageUtil.log("Setting " + entry.getKey().name() + " to " + entry.getValue() + " for " + player.getName());
            if (player.getAttribute(entry.getKey()) == null) {
                MessageUtil.log("Attribute " + entry.getKey().name() + " not found on Player class. Check Papyrus.");
                continue;
            }
            player.getAttribute(entry.getKey()).setBaseValue(entry.getValue());
        }
    }

    /* getter and setter */

    public Player getPlayer() {
        return player;
    }

    public ServerPlayer getServerPlayer() {
        return (ServerPlayer) player;
    }

    public SpellData getSpellAt(int slot) {
        return assignedSlots[slot];
    }

    public void setSpellAt(int slot, SpellData data) {
        assignedSlots[slot] = data;
    }

    public SpellData[] getAssignedSlots() {
        return assignedSlots;
    }

    public void setAssignedSlots(SpellData[] assignedSlots) {
        this.assignedSlots = assignedSlots;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getXp() {
        return xp;
    }

    public void setXp(double xp) {
        this.xp = xp;
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

    public void addSpell(SpellData spellData) {
        unlockedSpells.add(spellData);
    }

    public Traitline getSelectedTraitline() {
        return selectedTraitline;
    }

    public void setSelectedTraitline(Traitline selectedTraitline) {
        this.selectedTraitline = selectedTraitline;
    }

    public void setMaxEnergy(int i) {
        maxEnergy = i;
    }

    public void setEnergy(int i) {
        energy = i;
    }

    public Set<SpellData> getUnlockedSpells() {
        return unlockedSpells;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getEnergy() {
        return energy;
    }

    public int getCharacterID() {
        return characterID;
    }

    public ItemStack getIcon() {
        ItemStack itemStack = new ItemStack(Material.DIAMOND);
        ItemMeta meta = itemStack.getItemMeta();
        if (hClass != null) {
            meta.displayName(Component.text(hClass.getId()));
        } else {
            meta.displayName(Component.text("No Class"));
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}

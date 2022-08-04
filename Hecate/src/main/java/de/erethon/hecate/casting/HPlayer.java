package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.user.LoadableUser;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.caster.SpellCaster;
import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.SpellEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HPlayer extends EConfig implements LoadableUser, SpellCaster {

    public static final int CONFIG_VERSION = 1;

    private Player player;
    private Set<SpellData> unlockedSpellData = new HashSet<>();
    private Set<SpellData> passiveSpells = new HashSet<>();
    private final SpellData[] assignedSlots = new SpellData[8];

    Map<SpellData, Long> usedSpells = new LinkedHashMap<>();
    Set<SpellEffect> effects = new HashSet<>();
    Set<SpellbookSpell> runningPassiveSpells = new HashSet<>();

    private int level = 1;
    private double xp = 0;

    private HClass hClass;

    private boolean isInCastmode = false;

    private final List<ItemStack> hotbarItems = new ArrayList<>();
    private int hotbarSlot = 0;

    private int maxEnergy = 0;
    private int energy = 0;

    MiniMessage miniMessage = MiniMessage.miniMessage();

    public HPlayer(Spellbook spellbook, Player player) {
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
        tick();
        if (!isInCastmode) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 1; slot < 8; slot++) { // Start at 1, 0 is weapon
            if (inventory.getItem(slot) == null || getSpellAt(slot) == null) {
                continue;
            }
            SpellData spellData = getSpellAt(slot);
            inventory.getItem(slot).setAmount(calculateCooldown(spellData));
            if (spellData.getCooldown() == calculateCooldown(spellData)) {
                player.setCooldown(inventory.getItem(slot).getType(), (calculateCooldown(getSpellAt(slot)) * 20) - 15); // its slightly offset visually
            }
        }
        StringBuilder positive = new StringBuilder("<green>");
        StringBuilder negative = new StringBuilder("<red>");
        int health = (int) player.getHealth();
        int maxHealth = (int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        for (SpellEffect effect : getEffects()) {
            if (effect.getData().isPositive()) {
                positive.append(effect.getData().getIcon()).append(" ");
            } else {
                negative.append(effect.getData().getIcon()).append(" ");
            }
        }
        Component component = miniMessage.deserialize("<dark_red>" + health + "<dark_gray>/<dark_red>" + maxHealth + " <<<    <green>+" + positive + " <dark_gray>| <red>-" + negative + "  <yellow>>>> " + energy + "<dark_gray>/<yellow>" + maxEnergy);
        player.sendActionBar(component);
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
        for (String spellId : config.getStringList("passiveSpells")) {
            SpellData spellData = spellbook.getLibrary().getSpellByID(spellId);
            if (spellData == null) {
                MessageUtil.log("Unknown spell '" + spellId + "' found under 'passiveSpells'");
                continue;
            }
            addPassiveSpell(spellData.getActiveSpell(this));
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
        maxEnergy = config.getInt("maxEnergy", 100);
        energy = config.getInt("energy", 50);
    }

    /* LoadableUser methods */

    @Override
    public void updatePlayer(Player player) {
        this.player = player;
    }

    @Override
    public void saveUser() {
        MessageUtil.log("User saved!");
        config.set("unlockedSpells", unlockedSpellData.stream().map(SpellData::getId).collect(Collectors.toList()));
        config.set("passiveSpells", getPassiveSpells().stream().map(SpellbookSpell::getId).collect(Collectors.toList()));
        config.set("assignedSlots", Arrays.stream(assignedSlots).map(s -> s == null ? "empty" : s.getId()).collect(Collectors.toList()));
        config.set("level", level);
        config.set("xp", xp);
        config.set("maxEnergy", maxEnergy);
        config.set("energy", energy);
        save();
    }

    /* getter and setter */

    public Player getPlayer() {
        return player;
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

    @Override
    public void sendMessage(String message) {
        MessageUtil.sendMessage(player, message);
    }

    @Override
    public void sendActionbar(String message) {
        MessageUtil.sendActionBarMessage(player, message);
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public Map<SpellData, Long> getUsedSpells() {
        return usedSpells;
    }

    @Override
    public Set<SpellEffect> getEffects() {
        return effects;
    }

    @Override
    public Set<SpellbookSpell> getPassiveSpells() {
        return runningPassiveSpells;
    }

    @Override
    public LivingEntity getEntity() {
        return player;
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public int setEnergy(int e) {
        return energy = Math.min(e, maxEnergy);
    }

    @Override
    public int getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public int setMaxEnergy(int e) {
        return maxEnergy = e;
    }

    @Override
    public int addEnergy(int e) {
        energy = Math.min(energy + e, maxEnergy);
        player.sendActionBar(Component.text("Energie: " + energy));
        return energy;
    }

    @Override
    public int removeEnergy(int e) {
        energy = Math.max(energy - e, 0);
        player.sendActionBar(Component.text("Energie: " + energy));
        return energy;
    }

    @Override
    public Team getTeam() {
        return Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player);
    }

    @Override
    public void setTeam(Team team) {
        getTeam().removePlayer(player);
        team.addPlayer(player);
    }
}

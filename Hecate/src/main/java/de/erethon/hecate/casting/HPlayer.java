package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.user.LoadableUser;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import de.erethon.spellbook.spells.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Arrays;
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

    public void addXp(double amount) {
        xp += amount;
        checkLevel();
    }
}

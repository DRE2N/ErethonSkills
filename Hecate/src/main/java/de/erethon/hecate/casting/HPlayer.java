package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.user.LoadableUser;
import de.erethon.hecate.Hecate;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import de.erethon.spellbook.spells.Spell;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HPlayer extends EConfig implements LoadableUser {

    public static final int CONFIG_VERSION = 1;

    private Player player;
    private final SpellCaster caster;
    private final Set<Spell> unlockedSpells = new HashSet<>();
    private final Spell[] assignedSlots = new Spell[8];

    public HPlayer(Spellbook spellbook, Player player) {
        super(HPlayerCache.getPlayerFile(player), CONFIG_VERSION);
        this.player = player;
        this.caster = new SpellCaster(spellbook, player);
    }

    /* EConfig methods */

    @Override
    public void load() {
        Spellbook spellbook = Hecate.getInstance().getSpellbook();
        for (String spellId : config.getStringList("unlockedSpells")) {
            Spell spell = spellbook.getLibrary().getSpellByID(spellId);
            if (spell == null) {
                MessageUtil.log("Unknown spell '" + spellId + "' found under 'unlockedSlots'");
                continue;
            }
            unlockedSpells.add(spell);
        }
        List<String> slotList = config.getStringList("assignedSlots");
        if (slotList.size() > 8) {
            MessageUtil.log("Illegal amount of slots assigned");
            return;
        }
        for (int i = 0; i < slotList.size(); i++) {
            String spellId = slotList.get(i);
            Spell spell = spellbook.getLibrary().getSpellByID(spellId);
            if (spell == null) {
                MessageUtil.log("Unknown spell '" + spellId + "' found under 'assignedSlots'");
                continue;
            }
            assignedSlots[i] = spell;
        }
    }

    /* LoadableUser methods */

    @Override
    public void updatePlayer(Player player) {
        this.player = player;
    }

    @Override
    public void saveUser() {
        config.set("unlockedSpells", unlockedSpells.stream().map(Spell::getId).collect(Collectors.toList()));
        config.set("assignedSlots", Arrays.stream(assignedSlots).map(s -> s == null ? "empty" : s.getId()).collect(Collectors.toList()));
        save();
    }

    /* getter and setter */

    public Player getPlayer() {
        return player;
    }

    public SpellCaster getCaster() {
        return caster;
    }

    public Set<Spell> getUnlockedSpells() {
        return unlockedSpells;
    }

    public Spell getSpellAt(int slot) {
        return assignedSlots[slot];
    }

    public Spell[] getAssignedSlots() {
        return assignedSlots;
    }

}

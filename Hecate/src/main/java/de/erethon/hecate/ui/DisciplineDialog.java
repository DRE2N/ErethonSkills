package de.erethon.hecate.ui;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.progression.LevelUtil;
import de.erethon.rhea.Rhea;
import de.erethon.rhea.RheaTemplate;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** A no-scroll, localized discipline browser backed by the current class data. */
public final class DisciplineDialog {

    private final Hecate plugin;
    private final Rhea rhea;
    private final Map<String, RheaTemplate> templates = new HashMap<>();
    private final Set<UUID> pendingSelections = ConcurrentHashMap.newKeySet();
    private final Map<LoreKey, Component> loreCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<LoreKey, Component> eldest) {
            return size() > 256;
        }
    };

    public DisciplineDialog(Hecate plugin) {
        this.plugin = plugin;
        this.rhea = new Rhea(plugin);
        for (String language : List.of("en", "de")) for (int selected = 0; selected < 3; selected++) {
            String id = language + "-" + selected;
            try {
                templates.put(id, RheaTemplate.read(plugin, "rhea/discipline/" + id + ".json"));
            } catch (Exception ex) {
                throw new IllegalStateException("Missing discipline dialog template " + id, ex);
            }
        }
    }

    public void open(Player player) { open(player, -1); }

    public void clearLoreCache() {
        synchronized (loreCache) {
            loreCache.clear();
        }
    }

    private void open(Player player, int requestedIndex) {
        HCharacter character = selectedCharacter(player);
        if (character == null || character.getHClass() == null) {
            player.sendMessage(Component.text("Select a character before opening disciplines.", NamedTextColor.RED));
            return;
        }
        List<Traitline> paths = orderedPaths(character.getHClass());
        if (paths.isEmpty() || paths.size() > 3) {
            player.sendMessage(Component.text("This archetype needs one to three configured disciplines.", NamedTextColor.RED));
            return;
        }
        int selected = requestedIndex >= 0 && requestedIndex < paths.size()
                ? requestedIndex : Math.max(0, paths.indexOf(character.getTraitline()));
        LevelUtil.getCharacterLevel(character).whenComplete((level, failure) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline() || selectedCharacter(player) != character) return;
                    if (failure != null) {
                        player.sendMessage(Component.text("Could not load your character level.", NamedTextColor.RED));
                        return;
                    }
                    show(player, character, paths, selected, Math.max(1, level));
                }));
    }

    private void show(Player player, HCharacter character, List<Traitline> paths, int selected, int level) {
        boolean german = player.locale().getLanguage().equals("de");
        String language = german ? "de" : "en";
        RheaTemplate template = templates.get(language + "-" + selected);
        Traitline preview = paths.get(selected);
        Traitline equipped = character.getTraitline();
        Rhea.DialogBuilder dialog = rhea.dialog(template,
                Component.text(german ? "Disziplinen" : "Disciplines"),
                Component.text(german ? "Schließen" : "Close"));

        String className = name(character.getHClass().getString("displayName." + language), character.getHClass().getId());
        put(dialog, "player-state", className + " · " + (german ? "Stufe " : "Level ") + level,
                DisciplineText.GOLD, false);
        for (int index = 0; index < paths.size(); index++) {
            Traitline path = paths.get(index);
            String pathName = name(path.getString("displayName." + language), path.getId());
            put(dialog, "name:" + index, pathName, DisciplineText.LIGHT, false);
            String status = path == equipped ? (german ? "AUSGERÜSTET" : "EQUIPPED")
                    : level >= required(path) ? (german ? "VERFÜGBAR" : "AVAILABLE")
                    : (german ? "AB STUFE " : "LEVEL ") + required(path);
            put(dialog, "status:" + index, status,
                    level >= required(path) ? DisciplineText.GOLD : DisciplineText.LOCKED, false);
            final int target = index;
            dialog.action("discipline:" + index, clicked -> open(clicked, target));
            dialog.hover("discipline:" + index, Component.text(pathName, DisciplineText.GOLD)
                    .append(Component.newline()).append(Component.text(status, DisciplineText.MUTED)));
        }
        put(dialog, "detail-title", name(preview.getString("displayName." + language), preview.getId()),
                DisciplineText.GOLD, false);
        String description = String.join(" ", preview.getStringList("description." + language));
        dialog.slot("detail-description", Component.text(DisciplineText.plain(description), DisciplineText.LIGHT));

        List<SpellData> abilities = abilities(preview);
        for (int index = 0; index < Math.min(6, abilities.size()); index++) {
            SpellData spell = abilities.get(index);
            int unlock = Math.max(required(preview), preview.getSpellUnlockLevel(spell));
            put(dialog, "ability-name:" + index,
                    name(spell.getString("name." + language), spell.getId()),
                    level >= unlock ? DisciplineText.LIGHT : DisciplineText.LOCKED, false);
            dialog.hover("ability:" + index, abilityLore(player, spell, unlock, german));
        }
        for (int index = abilities.size(); index < 6; index++) {
            put(dialog, "ability-name:" + index, "—", DisciplineText.LOCKED, false);
            dialog.hover("ability:" + index, Component.text(
                    german ? "Keine weitere Fähigkeit in dieser Disziplin" : "No further ability in this discipline",
                    DisciplineText.MUTED));
        }

        boolean active = preview == equipped;
        boolean unlocked = level >= required(preview);
        String equipLabel = active ? (german ? "Ausgerüstet" : "Equipped")
                : unlocked ? (german ? "Auswählen" : "Equip discipline")
                : (german ? "Gesperrt" : "Locked");
        String equipStatus = active ? (german ? "Deine aktive Disziplin" : "Your active discipline")
                : unlocked ? (german ? "Bereit zum Ausrüsten" : "Ready to equip")
                : (german ? "Freischaltung ab Stufe " : "Unlocks at level ") + required(preview);
        put(dialog, "equip-label", equipLabel,
                unlocked && !active ? DisciplineText.LIGHT : DisciplineText.MUTED, false);
        put(dialog, "equip-status", equipStatus,
                unlocked ? DisciplineText.MUTED : DisciplineText.LOCKED, false);
        if (unlocked && !active) dialog.action("equip", clicked -> equip(clicked, preview.getId()));
        dialog.hover("equip", Component.text(equipStatus, DisciplineText.GOLD));
        dialog.show(player);
    }

    private void equip(Player player, String disciplineId) {
        HCharacter character = selectedCharacter(player);
        if (character == null || character.getHClass() == null || !pendingSelections.add(character.getCharacterID())) return;
        Traitline requested = character.getHClass().getTraitlines().stream()
                .filter(path -> path.getId().equalsIgnoreCase(disciplineId)).findFirst().orElse(null);
        if (requested == null) {
            pendingSelections.remove(character.getCharacterID());
            return;
        }
        LevelUtil.getCharacterLevel(character).whenComplete((level, failure) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (failure != null || level == null || level < required(requested) ||
                            selectedCharacter(player) != character) {
                        pendingSelections.remove(character.getCharacterID());
                        if (player.isOnline()) open(player);
                        return;
                    }
                    Integer[] clearedTraits = {-1, -1, -1};
                    plugin.getDatabaseManager().updateCharacterDiscipline(character.getCharacterID(),
                            character.getHClass().getId(), requested.getId(), clearedTraits).whenComplete((saved, saveFailure) ->
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                pendingSelections.remove(character.getCharacterID());
                                if (saveFailure != null || !Boolean.TRUE.equals(saved)) {
                                    if (player.isOnline()) player.sendMessage(Component.text(
                                            "Could not save your discipline selection.", NamedTextColor.RED));
                                    return;
                                }
                                character.setSelectedTraits(clearedTraits);
                                character.setTraitline(requested);
                                if (selectedCharacter(player) == character) {
                                    if (character.getCastingManager() != null) character.getCastingManager().refreshDiscipline();
                                    if (player.isOnline()) open(player, orderedPaths(character.getHClass()).indexOf(requested));
                                }
                            }));
                }));
    }

    private Component abilityLore(Player player, SpellData spell, int unlock, boolean german) {
        List<Component> placeholders = List.of();
        try {
            SpellbookSpell active = spell.getActiveSpell(player);
            if (active != null) placeholders = active.getPlaceholders(player);
        } catch (RuntimeException ex) {
            plugin.getLogger().fine("No active preview instance for " + spell.getId() + ": " + ex.getMessage());
        }
        Locale locale = german ? Locale.GERMANY : Locale.US;
        LoreKey key = new LoreKey(spell.getId(), locale, unlock, List.copyOf(placeholders));
        synchronized (loreCache) {
            return loreCache.computeIfAbsent(key, ignored -> translatedAbilityLore(spell, unlock, german, locale, key.placeholders()));
        }
    }

    private Component translatedAbilityLore(SpellData spell, int unlock, boolean german,
                                            Locale locale, List<Component> placeholders) {
        Component title = GlobalTranslator.render(
                Component.translatable("hecate.spellbook.spell.name." + spell.getId()), locale)
                .color(DisciplineText.GOLD).decorate(TextDecoration.BOLD);
        Component lore = Component.empty().decoration(TextDecoration.BOLD, false)
                .append(title).append(Component.newline()).append(Component.text(
                        (german ? "Ab Stufe " : "Available at level ") + unlock, DisciplineText.MUTED));
        for (int line = 0; line < spell.getDescriptionLineCount(); line++) {
            Component description = GlobalTranslator.render(Component.translatable(
                    "hecate.spellbook.spell.description." + spell.getId() + "." + line, placeholders), locale)
                    .decoration(TextDecoration.BOLD, false);
            lore = lore.append(Component.newline()).append(description);
        }
        return lore;
    }

    private record LoreKey(String spellId, Locale locale, int unlock, List<Component> placeholders) { }

    private void put(Rhea.DialogBuilder dialog, String id,
                     String value, TextColor color, boolean bold) {
        Component component = Component.text(DisciplineText.plain(value), color);
        dialog.slot(id, bold ? component.decorate(TextDecoration.BOLD) : component);
    }

    private HCharacter selectedCharacter(Player player) {
        HPlayer hPlayer = plugin.getDatabaseManager().getHPlayer(player);
        return hPlayer == null ? null : hPlayer.getSelectedCharacter();
    }

    private static List<Traitline> orderedPaths(HClass hClass) {
        List<Traitline> result = new ArrayList<>(hClass.getTraitlines());
        result.sort(Comparator.comparingInt(DisciplineDialog::required));
        return result;
    }

    private static List<SpellData> abilities(Traitline path) {
        List<SpellData> result = new ArrayList<>();
        for (String id : path.getStringList("spells")) {
            SpellData spell = Hecate.getInstance().getAPI().getLibrary().getSpellByID(id);
            if (spell != null && !result.contains(spell)) result.add(spell);
        }
        return result;
    }

    private static int required(Traitline path) { return Math.max(1, path.getInitialLevelRequirement()); }

    private static String name(String configured, String fallback) {
        String plain = DisciplineText.plain(configured);
        return plain.isEmpty() ? fallback : plain;
    }
}

package de.erethon.hecate.web;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.data.dao.CharacterDao;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WebInventoryService {

    private final DatabaseManager databaseManager;

    public WebInventoryService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<List<WebInventoryItem>> listBankItems(UUID playerId) {
        return databaseManager.loadBankData(playerId).thenApply(bank -> {
            List<WebInventoryItem> items = new ArrayList<>();
            ItemStack[] contents = bank.getContents();
            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack stack = contents[slot];
                if (isPresent(stack)) {
                    String itemId = readHephaestusId(stack).orElse(stack.getType().getKey().toString());
                    items.add(new WebInventoryItem("bank", null, slot, itemId, stack.getAmount(), stack.clone()));
                }
            }
            return items;
        });
    }

    public CompletableFuture<List<CharacterSummary>> listCharacters(UUID playerId) {
        return databaseManager.queryAsync(handle -> handle.attach(CharacterDao.class)
                .findCharactersByPlayerId(playerId)
                .stream()
                .map(data -> new CharacterSummary(data.getCharacterId(), data.getClassId(), data.getLevel(), data.getLockedBy()))
                .toList());
    }

    public CompletableFuture<List<WebInventoryItem>> listCurrentCharacterItems(UUID playerId) {
        Optional<UUID> currentCharacterId = getCurrentCharacterId(playerId);
        if (currentCharacterId.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        return listCharacterItems(playerId, currentCharacterId.get());
    }

    public CompletableFuture<List<WebInventoryItem>> listCharacterItems(UUID playerId, UUID characterId) {
        return databaseManager.queryAsync(handle -> {
            CharacterDao dao = handle.attach(CharacterDao.class);
            Optional<CharacterDao.FlatData> character = dao.findCharacterById(characterId);
            if (character.isEmpty() || !character.get().getPlayerId().equals(playerId) || character.get().getLockedBy() != null) {
                return List.<WebInventoryItem>of();
            }
            Optional<byte[]> playerData = dao.findCharacterPlayerData(characterId);
            return playerData.map(bytes -> readCharacterInventory(characterId, bytes)).orElse(List.of());
        });
    }

    public Optional<UUID> getCurrentCharacterId(UUID playerId) {
        HPlayer hPlayer = databaseManager.getHPlayer(playerId);
        if (hPlayer == null || hPlayer.getSelectedCharacter() == null) {
            return Optional.empty();
        }
        return Optional.of(hPlayer.getSelectedCharacter().getCharacterID());
    }

    public CompletableFuture<Optional<WebTakenItem>> takeBankItem(UUID playerId, int slot, int quantity) {
        if (slot < 0 || quantity <= 0) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return databaseManager.loadBankData(playerId).thenCompose(bank -> {
            ItemStack[] contents = bank.getContents();
            if (slot >= contents.length || !isPresent(contents[slot]) || contents[slot].getAmount() < quantity) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            ItemStack source = contents[slot];
            ItemStack taken = source.clone();
            taken.setAmount(quantity);
            String itemId = readHephaestusId(taken).orElse(taken.getType().getKey().toString());
            source.setAmount(source.getAmount() - quantity);
            if (source.getAmount() <= 0) {
                contents[slot] = null;
            }
            bank.setContents(contents);
            return databaseManager.saveBankData(playerId, bank.serialize(), bank.getUnlockedPages())
                    .thenApply(success -> success ? Optional.of(new WebTakenItem(itemId, quantity, taken, null)) : Optional.empty());
        });
    }

    public CompletableFuture<Optional<WebTakenItem>> takeCharacterItem(UUID playerId, UUID characterId, int slot, int quantity) {
        if (slot < 0 || quantity <= 0) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return databaseManager.queryAsync(handle -> {
            CharacterDao dao = handle.attach(CharacterDao.class);
            Optional<CharacterDao.FlatData> character = dao.findCharacterById(characterId);
            if (character.isEmpty() || !character.get().getPlayerId().equals(playerId) || character.get().getLockedBy() != null) {
                return Optional.<WebTakenItem>empty();
            }
            Optional<byte[]> playerData = dao.findCharacterPlayerData(characterId);
            if (playerData.isEmpty()) {
                return Optional.empty();
            }
            try {
                CompoundTag root = readCompressed(playerData.get());
                Optional<ListTag> inventoryOpt = root.getList("Inventory");
                if (inventoryOpt.isEmpty()) {
                    return Optional.empty();
                }
                ListTag inventory = inventoryOpt.get();
                for (int i = 0; i < inventory.size(); i++) {
                    Optional<CompoundTag> itemOpt = inventory.getCompound(i);
                    if (itemOpt.isEmpty()) {
                        continue;
                    }
                    CompoundTag item = itemOpt.get();
                    int itemSlot = item.getByte("Slot").map(Byte::intValue).orElse(-999);
                    if (itemSlot != slot) {
                        continue;
                    }
                    Optional<String> itemId = readHephaestusId(item);
                    int amount = item.getInt("count").or(() -> item.getInt("Count")).orElse(1);
                    if (itemId.isEmpty() || amount < quantity) {
                        return Optional.empty();
                    }
                    if (amount == quantity) {
                        inventory.remove(i);
                    } else {
                        item.putInt(item.contains("count") ? "count" : "Count", amount - quantity);
                    }
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    NbtIo.writeCompressed(root, output);
                    dao.updateCharacterPlayerData(characterId, output.toByteArray());
                    return Optional.of(new WebTakenItem(itemId.get(), quantity, null, extractCustomDataBytes(item).orElse(null)));
                }
            } catch (Exception e) {
                Hecate.log("Failed to mutate web character inventory for " + characterId + ": " + e.getMessage());
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Optional<WebTakenItem>> takeCurrentCharacterItem(UUID playerId, UUID characterId, int slot, int quantity) {
        return takeCharacterItem(playerId, characterId, slot, quantity);
    }

    private List<WebInventoryItem> readCharacterInventory(UUID characterId, byte[] playerData) {
        List<WebInventoryItem> items = new ArrayList<>();
        try {
            CompoundTag root = readCompressed(playerData);
            Optional<ListTag> inventoryOpt = root.getList("Inventory");
            if (inventoryOpt.isEmpty()) {
                return items;
            }
            ListTag inventory = inventoryOpt.get();
            for (int i = 0; i < inventory.size(); i++) {
                Optional<CompoundTag> itemOpt = inventory.getCompound(i);
                if (itemOpt.isEmpty()) {
                    continue;
                }
                CompoundTag item = itemOpt.get();
                Optional<String> itemId = readHephaestusId(item);
                if (itemId.isEmpty()) {
                    continue;
                }
                int slot = item.getByte("Slot").map(Byte::intValue).orElse(i);
                int amount = item.getInt("count").or(() -> item.getInt("Count")).orElse(1);
                if (amount > 0) {
                    items.add(new WebInventoryItem("character", characterId, slot, itemId.get(), amount, null));
                }
            }
        } catch (Exception e) {
            Hecate.log("Failed to read web character inventory for " + characterId + ": " + e.getMessage());
        }
        return items;
    }

    private static CompoundTag readCompressed(byte[] data) throws Exception {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            return NbtIo.readCompressed(input, NbtAccounter.create(20 * 1024 * 1024));
        }
    }

    private static Optional<String> readHephaestusId(ItemStack stack) {
        try {
            Object value = stack.getItemMeta().getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey("hephaestus", "id"),
                    org.bukkit.persistence.PersistentDataType.STRING
            );
            if (value instanceof String id && !id.isBlank()) {
                return Optional.of(id);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static Optional<String> readHephaestusId(CompoundTag tag) {
        Optional<String> direct = tag.getString("hephaestus-id").filter(id -> !id.isBlank());
        if (direct.isPresent()) {
            return direct;
        }
        Optional<CompoundTag> components = tag.getCompound("components").or(() -> tag.getCompound("Components"));
        if (components.isPresent()) {
            Optional<CompoundTag> customData = components.get().getCompound("minecraft:custom_data")
                    .or(() -> components.get().getCompound("custom_data"));
            if (customData.isPresent()) {
                Optional<String> id = readHephaestusId(customData.get());
                if (id.isPresent()) {
                    return id;
                }
            }
        }
        for (String key : tag.keySet()) {
            Optional<CompoundTag> child = tag.getCompound(key);
            if (child.isPresent()) {
                Optional<String> id = readHephaestusId(child.get());
                if (id.isPresent()) {
                    return id;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<byte[]> extractCustomDataBytes(CompoundTag itemTag) {
        Optional<CompoundTag> customData = findCustomData(itemTag);
        if (customData.isEmpty()) {
            return Optional.empty();
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(customData.get(), output);
            return Optional.of(output.toByteArray());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<CompoundTag> findCustomData(CompoundTag tag) {
        Optional<CompoundTag> components = tag.getCompound("components").or(() -> tag.getCompound("Components"));
        if (components.isPresent()) {
            Optional<CompoundTag> customData = components.get().getCompound("minecraft:custom_data")
                    .or(() -> components.get().getCompound("custom_data"));
            if (customData.isPresent()) {
                return customData;
            }
        }
        if (tag.contains("hephaestus-id") || tag.contains("hephaestus-data")) {
            return Optional.of(tag);
        }
        for (String key : tag.keySet()) {
            Optional<CompoundTag> child = tag.getCompound(key);
            if (child.isPresent()) {
                Optional<CompoundTag> customData = findCustomData(child.get());
                if (customData.isPresent()) {
                    return customData;
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isPresent(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR && stack.getAmount() > 0;
    }

    public record WebInventoryItem(String source, UUID characterId, int slot, String itemId, int quantity, ItemStack stack) {
    }

    public record WebTakenItem(String itemId, int quantity, ItemStack stack, byte[] customData) {
    }

    public record CharacterSummary(UUID characterId, String classId, int level, String lockedBy) {
    }
}

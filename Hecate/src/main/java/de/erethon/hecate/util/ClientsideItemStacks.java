package de.erethon.hecate.util;

import de.erethon.spellbook.api.SpellData;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClientsideItemStacks {

    private static final Map<Locale, Map<SpellData, List<Component>>> componentCache = new HashMap<>();

    private static final Item[] SPELL_ICONS = new Item[] {
            Items.BLACK_DYE,
            Items.BLUE_DYE,
            Items.BROWN_DYE,
            Items.CYAN_DYE,
            Items.GRAY_DYE,
            Items.GREEN_DYE,
            Items.LIGHT_BLUE_DYE,
            Items.LIGHT_GRAY_DYE,
            Items.LIME_DYE,
            Items.MAGENTA_DYE,
    };
    
    public static void sendSpellIconStack(int slot, SpellData data, Locale locale, ServerPlayer player) {
        ItemStack stack = new ItemStack(SPELL_ICONS[slot]);
        ItemLore lore = new ItemLore(getOrCacheLore(data, locale));
        sendStack(slot, data, locale, player, stack, lore);
    }

    /**
     * Sends a spell icon stack to the player with placeholders. The placeholders are replaced with the given replacements, this may be slower than the other method.
     */
    public static void sendSpellIconStackWithPlaceholders(int slot, SpellData data, Locale locale, ServerPlayer player, String[] args, String[] replacements) {
        ItemStack stack = new ItemStack(SPELL_ICONS[slot]);
        List<net.kyori.adventure.text.Component> adventureLore = data.getDescription(locale, args, replacements);
        ItemLore lore = new ItemLore(PaperAdventure.asVanilla(adventureLore));
        sendStack(slot, data, locale, player, stack, lore);
    }

    private static void sendStack(int slot, SpellData data, Locale locale, ServerPlayer player, ItemStack stack, ItemLore lore) {
        stack.set(DataComponents.LORE, lore);
        stack.set(DataComponents.ITEM_NAME, PaperAdventure.asVanilla(data.getDisplayName(locale)));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(data.getInt("customModelData", 0)));
        ClientboundContainerSetSlotPacket packet = new ClientboundContainerSetSlotPacket(ClientboundContainerSetSlotPacket.PLAYER_INVENTORY, 0, slot, stack);
        player.connection.send(packet);
    }

    private static List<Component> getOrCacheLore(SpellData data, Locale locale) {
        return componentCache.computeIfAbsent(locale, l -> new HashMap<>())
                .computeIfAbsent(data, d -> {
                    List<net.kyori.adventure.text.Component> adventureLore = data.getDescription(locale);
                    return PaperAdventure.asVanilla(adventureLore);
                });
    }
}

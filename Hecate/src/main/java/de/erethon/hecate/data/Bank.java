package de.erethon.hecate.data;

import de.erethon.hecate.Hecate;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a player's shared bank that is accessible across all characters
 */
public class Bank {

    private final HPlayer owner;
    private ItemStack[] contents; // All pages combined
    private int unlockedPages;

    public static final int SLOTS_PER_PAGE = 45; // 5 rows for storage (9 slots per row)
    public static final int DEFAULT_PAGES = 1;
    public static final int MAX_PAGES = 10;

    public Bank(HPlayer owner, byte[] serializedContents, int unlockedPages) {
        this.owner = owner;
        this.unlockedPages = Math.max(DEFAULT_PAGES, unlockedPages);

        if (serializedContents != null && serializedContents.length > 0) {
            try {
                this.contents = ItemStack.deserializeItemsFromBytes(serializedContents);
            } catch (Exception e) {
                Hecate.log("Error deserializing bank contents for player " + owner.getPlayerId() + ": " + e.getMessage());
                e.printStackTrace();
                this.contents = new ItemStack[getTotalSlots()];
            }
        } else {
            this.contents = new ItemStack[getTotalSlots()];
        }

        // Ensure contents array is the correct size
        if (this.contents.length < getTotalSlots()) {
            ItemStack[] newContents = new ItemStack[getTotalSlots()];
            System.arraycopy(this.contents, 0, newContents, 0, this.contents.length);
            this.contents = newContents;
        }
    }

    public HPlayer getOwner() {
        return owner;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }

    public int getUnlockedPages() {
        return unlockedPages;
    }

    public void setUnlockedPages(int pages) {
        int oldTotal = getTotalSlots();
        this.unlockedPages = Math.min(Math.max(DEFAULT_PAGES, pages), MAX_PAGES);
        int newTotal = getTotalSlots();

        // Resize contents array if needed
        if (newTotal != oldTotal) {
            ItemStack[] newContents = new ItemStack[newTotal];
            System.arraycopy(contents, 0, newContents, 0, Math.min(oldTotal, newTotal));
            contents = newContents;
        }
    }

    public int getTotalSlots() {
        return unlockedPages * SLOTS_PER_PAGE;
    }

    public ItemStack[] getPageContents(int page) {
        if (page < 0 || page >= unlockedPages) {
            return new ItemStack[SLOTS_PER_PAGE];
        }

        int startIndex = page * SLOTS_PER_PAGE;
        int endIndex = Math.min(startIndex + SLOTS_PER_PAGE, contents.length);
        ItemStack[] pageContents = new ItemStack[SLOTS_PER_PAGE];

        for (int i = 0; i < SLOTS_PER_PAGE && (startIndex + i) < endIndex; i++) {
            pageContents[i] = contents[startIndex + i];
        }

        return pageContents;
    }

    public void setPageContents(int page, ItemStack[] pageContents) {
        if (page < 0 || page >= unlockedPages) {
            return;
        }

        int startIndex = page * SLOTS_PER_PAGE;
        for (int i = 0; i < SLOTS_PER_PAGE && i < pageContents.length && (startIndex + i) < contents.length; i++) {
            contents[startIndex + i] = pageContents[i];
        }
    }

    public byte[] serialize() {
        try {
            return ItemStack.serializeItemsAsBytes(java.util.Arrays.asList(contents));
        } catch (Exception e) {
            Hecate.log("Error serializing bank contents for player " + owner.getPlayerId() + ": " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }
}

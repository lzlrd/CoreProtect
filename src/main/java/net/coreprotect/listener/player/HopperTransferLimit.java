package net.coreprotect.listener.player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Hopper transfers are logged with the amount their move event reports. A server moves at most "hopper-amount" items
 * per transfer, but its hopper code can report more: Leaf with sleeping-block-entity enabled reports the whole stack
 * in the source slot while moving one item. This cuts such a report down to what the server can move.
 */
final class HopperTransferLimit {

    private static final Map<String, Integer> LIMITS = new ConcurrentHashMap<>();

    private HopperTransferLimit() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns the item a move event transfers, with an amount the server is able to move in one transfer.
     *
     * @param event
     *            The move event
     * @param world
     *            The world the transfer happens in
     * @return The event's own item, or a copy with a smaller amount
     */
    static ItemStack movedItem(InventoryMoveItemEvent event, World world) {
        ItemStack item = event.getItem();
        // The server fires its own transfers as a subclass of the event. A plugin constructs the event itself and
        // may move more than a hopper does, so its amount is left alone.
        if (item == null || world == null || event.getClass() == InventoryMoveItemEvent.class) {
            return item;
        }

        int limit = limit(world);
        if (item.getAmount() <= limit) {
            return item;
        }

        ItemStack movedItem = item.clone();
        movedItem.setAmount(limit);
        return movedItem;
    }

    private static int limit(World world) {
        return LIMITS.computeIfAbsent(world.getName(), HopperTransferLimit::readLimit);
    }

    private static int readLimit(String worldName) {
        try {
            YamlConfiguration config = Bukkit.spigot().getConfig();
            int amount = config.getInt("world-settings." + worldName + ".hopper-amount", config.getInt("world-settings.default.hopper-amount", 1));
            return Math.max(1, amount);
        }
        catch (Throwable e) {
            // Without the setting there is nothing to compare with, so every amount is accepted.
            return Integer.MAX_VALUE;
        }
    }
}

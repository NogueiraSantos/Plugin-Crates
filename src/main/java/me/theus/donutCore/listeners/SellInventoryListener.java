package me.theus.donutCore.listeners;

import me.theus.donutCore.DonutCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class SellInventoryListener implements Listener {

    private final DonutCore plugin;

    public SellInventoryListener(DonutCore plugin) {
        this.plugin = plugin;
    }

    public void triggerInventoryUpdate(Player player) {
        if (player == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (event.getCurrentItem() != null) {
            plugin.stripPriceLore(event.getCurrentItem());
        }
        if (event.getCursor() != null) {
            plugin.stripPriceLore(event.getCursor());
        }

        triggerInventoryUpdate(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (event.getOldCursor() != null) {
            plugin.stripPriceLore(event.getOldCursor());
        }
        if (event.getCursor() != null) {
            plugin.stripPriceLore(event.getCursor());
        }
        for (ItemStack item : event.getNewItems().values()) {
            if (item != null) {
                plugin.stripPriceLore(item);
            }
        }

        triggerInventoryUpdate(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getItemDrop() != null && event.getItemDrop().getItemStack() != null) {
            plugin.stripPriceLore(event.getItemDrop().getItemStack());
        }
        triggerInventoryUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            triggerInventoryUpdate((Player) event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        triggerInventoryUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        triggerInventoryUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        triggerInventoryUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player p = (Player) event.getPlayer();
            org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
            if (top != null && !(top instanceof org.bukkit.inventory.PlayerInventory)) {
                plugin.setOpenTopInventorySize(p.getUniqueId(), top.getSize());
            } else {
                plugin.setOpenTopInventorySize(p.getUniqueId(), 0);
            }
            triggerInventoryUpdate(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            plugin.setOpenTopInventorySize(event.getPlayer().getUniqueId(), 0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.setOpenTopInventorySize(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        triggerInventoryUpdate(event.getPlayer());
    }
}

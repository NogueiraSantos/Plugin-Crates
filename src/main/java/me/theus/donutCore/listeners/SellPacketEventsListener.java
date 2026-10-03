package me.theus.donutCore.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.theus.donutCore.DonutCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SellPacketEventsListener implements PacketListener, Listener {

    private final DonutCore plugin;
    private PacketListenerCommon registeredListener = null;

    public SellPacketEventsListener(DonutCore plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (registeredListener == null) {
            registeredListener = PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
        }
    }

    public void unregister() {
        if (registeredListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(registeredListener);
            registeredListener = null;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
            int windowId = packet.getWindowId();
            int slot = packet.getSlot();

            if (windowId < 0 || slot < 0) {
                return;
            }

            if (windowId > 0) {
                int topSize = plugin.getTopInventorySize(player);
                if (topSize <= 0 || slot < topSize || slot >= topSize + 36) {
                    return;
                }
            } else {
                if (slot < 9 || slot > 45) {
                    return;
                }
            }

            com.github.retrooper.packetevents.protocol.item.ItemStack peItem = packet.getItem();
            if (peItem != null && !peItem.isEmpty()) {
                ItemStack item = SpigotConversionUtil.toBukkitItemStack(peItem);
                if (item != null && item.getType() != Material.AIR && !plugin.isMultiplierButton(item) && plugin.hasWorth(item) && !plugin.isBlocked(item)) {
                    ItemStack cloned = plugin.createPricedItem(player, item);
                    if (cloned != null) {
                        packet.setItem(SpigotConversionUtil.fromBukkitItemStack(cloned));
                    }
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
            int windowId = packet.getWindowId();
            if (windowId < 0) {
                return;
            }

            List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = packet.getItems();
            if (items != null) {
                int topSize = 0;
                if (windowId > 0) {
                    topSize = Math.max(0, items.size() - 36);
                    if (topSize > 0) {
                        plugin.setOpenTopInventorySize(player.getUniqueId(), topSize);
                    } else {
                        topSize = plugin.getTopInventorySize(player);
                    }
                }

                List<com.github.retrooper.packetevents.protocol.item.ItemStack> clonedList = new ArrayList<>(items.size());
                boolean modified = false;
                for (int slot = 0; slot < items.size(); slot++) {
                    com.github.retrooper.packetevents.protocol.item.ItemStack peIt = items.get(slot);
                    if (peIt == null || peIt.isEmpty()) {
                        clonedList.add(peIt);
                        continue;
                    }

                    if (windowId > 0) {
                        if (topSize <= 0 || slot < topSize || slot >= topSize + 36) {
                            clonedList.add(peIt);
                            continue;
                        }
                    } else {
                        if (slot < 9 || slot > 45) {
                            clonedList.add(peIt);
                            continue;
                        }
                    }

                    ItemStack it = SpigotConversionUtil.toBukkitItemStack(peIt);
                    if (it != null && it.getType() != Material.AIR && !plugin.isMultiplierButton(it) && plugin.hasWorth(it) && !plugin.isBlocked(it)) {
                        ItemStack cloned = plugin.createPricedItem(player, it);
                        if (cloned != null) {
                            clonedList.add(SpigotConversionUtil.fromBukkitItemStack(cloned));
                            modified = true;
                            continue;
                        }
                    }
                    clonedList.add(peIt);
                }
                if (modified) {
                    packet.setItems(clonedList);
                }
            }

            if (windowId == 0 && packet.getCarriedItem() != null && packet.getCarriedItem().isPresent()) {
                com.github.retrooper.packetevents.protocol.item.ItemStack peCarried = packet.getCarriedItem().get();
                if (peCarried != null && !peCarried.isEmpty()) {
                    ItemStack carried = SpigotConversionUtil.toBukkitItemStack(peCarried);
                    if (carried != null && carried.getType() != Material.AIR && !plugin.isMultiplierButton(carried) && plugin.hasWorth(carried) && !plugin.isBlocked(carried)) {
                        ItemStack cloned = plugin.createPricedItem(player, carried);
                        if (cloned != null) {
                            packet.setCarriedItem(SpigotConversionUtil.fromBukkitItemStack(cloned));
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        });
    }
}

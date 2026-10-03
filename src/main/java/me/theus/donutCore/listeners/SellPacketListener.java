package me.theus.donutCore.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
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

public class SellPacketListener extends PacketAdapter implements Listener {

    private final DonutCore plugin;
    private boolean registered = false;

    public SellPacketListener(DonutCore plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.WINDOW_ITEMS);
        this.plugin = plugin;
    }

    public void register() {
        if (!registered) {
            ProtocolLibrary.getProtocolManager().addPacketListener(this);
            registered = true;
        }
    }

    public void unregister() {
        if (registered) {
            ProtocolLibrary.getProtocolManager().removePacketListener(this);
            registered = false;
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        PacketContainer packet = event.getPacket();
        PacketType type = event.getPacketType();

        if (type == PacketType.Play.Server.SET_SLOT) {
            int windowId = packet.getIntegers().read(0);
            int slot = -1;
            if (packet.getIntegers().size() >= 3) {
                slot = packet.getIntegers().read(2);
            } else if (packet.getIntegers().size() == 2 && packet.getShorts().size() > 0) {
                slot = packet.getShorts().read(0);
            } else if (packet.getIntegers().size() == 2) {
                slot = packet.getIntegers().read(1);
            }

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

            StructureModifier<ItemStack> itemModifier = packet.getItemModifier();
            if (itemModifier.size() > 0) {
                ItemStack item = itemModifier.read(0);
                if (item != null && item.getType() != Material.AIR && !plugin.isMultiplierButton(item) && plugin.hasWorth(item) && !plugin.isBlocked(item)) {
                    ItemStack cloned = plugin.createPricedItem(player, item);
                    if (cloned != null) {
                        itemModifier.write(0, cloned);
                    }
                }
            }
        } else if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            int windowId = packet.getIntegers().read(0);
            if (windowId < 0) {
                return;
            }

            StructureModifier<List<ItemStack>> listModifier = packet.getItemListModifier();
            if (listModifier.size() > 0) {
                List<ItemStack> items = listModifier.read(0);
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

                    List<ItemStack> clonedList = new ArrayList<>(items.size());
                    boolean modified = false;

                    for (int slot = 0; slot < items.size(); slot++) {
                        ItemStack it = items.get(slot);
                        if (it == null || it.getType() == Material.AIR) {
                            clonedList.add(it);
                            continue;
                        }

                        if (windowId > 0) {
                            if (topSize <= 0 || slot < topSize || slot >= topSize + 36) {
                                clonedList.add(it);
                                continue;
                            }
                        } else {
                            if (slot < 9 || slot > 45) {
                                clonedList.add(it);
                                continue;
                            }
                        }

                        if (!plugin.isMultiplierButton(it) && plugin.hasWorth(it) && !plugin.isBlocked(it)) {
                            ItemStack cloned = plugin.createPricedItem(player, it);
                            if (cloned != null) {
                                clonedList.add(cloned);
                                modified = true;
                                continue;
                            }
                        }
                        clonedList.add(it);
                    }

                    if (modified) {
                        listModifier.write(0, clonedList);
                    }
                }
            }

            if (windowId == 0) {
                StructureModifier<ItemStack> itemModifier = packet.getItemModifier();
                if (itemModifier.size() > 0) {
                    ItemStack carried = itemModifier.read(0);
                    if (carried != null && carried.getType() != Material.AIR && !plugin.isMultiplierButton(carried) && plugin.hasWorth(carried) && !plugin.isBlocked(carried)) {
                        ItemStack cloned = plugin.createPricedItem(player, carried);
                        if (cloned != null) {
                            itemModifier.write(0, cloned);
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

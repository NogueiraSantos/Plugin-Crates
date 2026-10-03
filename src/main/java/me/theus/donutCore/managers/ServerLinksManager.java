package me.theus.donutCore.managers;

import me.theus.donutCore.DonutCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ServerLinks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLinksSendEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

public class ServerLinksManager implements Listener {

    private final DonutCore plugin;
    private final List<LinkEntry> activeLinks = new ArrayList<>();

    public static class LinkEntry {
        private final String key;
        private final String name;
        private final String url;
        private final ServerLinks.Type type;

        public LinkEntry(String key, String name, String url, ServerLinks.Type type) {
            this.key = key;
            this.name = name;
            this.url = url;
            this.type = type;
        }

        public String getKey() { return key; }
        public String getName() { return name; }
        public String getUrl() { return url; }
        public ServerLinks.Type getType() { return type; }
    }

    public ServerLinksManager(DonutCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Carrega os links do config.yml e registra no ServerLinks nativo do Bukkit/Paper.
     * Envia para todos os jogadores online para atualizar o Menu ESC (Pause Menu).
     */
    public void registerLinks() {
        activeLinks.clear();
        if (!plugin.getConfig().getBoolean("SERVER-LINKS.ENABLED", true)) {
            clearBukkitLinks();
            return;
        }

        ServerLinks serverLinks;
        try {
            serverLinks = Bukkit.getServerLinks();
        } catch (Throwable t) {
            plugin.getLogger().warning("DonutCore: ServerLinks não é suportado nesta versão do Bukkit/Paper: " + t.getMessage());
            return;
        }

        if (serverLinks == null) return;

        try {
            List<ServerLinks.ServerLink> current = new ArrayList<>(serverLinks.getLinks());
            for (ServerLinks.ServerLink link : current) {
                serverLinks.removeLink(link);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("DonutCore: Erro ao limpar links antigos: " + t.getMessage());
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("SERVER-LINKS");
        if (section == null) return;

        loadConfiguredLink(section.getConfigurationSection("DISCORD"), "DISCORD", "&#5865F2Discord", "https://dc.gg/aureliumshields", ServerLinks.Type.COMMUNITY, serverLinks);
        loadConfiguredLink(section.getConfigurationSection("SITE"), "SITE", "&#00FFAASite Oficial", "https://aureliumshields.net", ServerLinks.Type.WEBSITE, serverLinks);
        loadConfiguredLink(section.getConfigurationSection("YOUTUBE"), "YOUTUBE", "&#FF0000YouTube", "https://youtube.com/@aureliumshields", ServerLinks.Type.NEWS, serverLinks);
        loadConfiguredLink(section.getConfigurationSection("LOJA"), "LOJA", "&#FFD700Loja", "https://loja.aureliumshields.net", null, serverLinks);

        List<Map<?, ?>> customList = section.getMapList("CUSTOM-LINKS");
        if (customList != null) {
            for (Map<?, ?> map : customList) {
                if (map == null) continue;
                String name = map.containsKey("NAME") ? String.valueOf(map.get("NAME")) : "Link";
                String url = map.containsKey("URL") ? String.valueOf(map.get("URL")) : "";
                String typeStr = map.containsKey("TYPE") ? String.valueOf(map.get("TYPE")) : null;
                ServerLinks.Type type = parseType(typeStr);
                addLinkEntry("CUSTOM", name, url, type, serverLinks, false);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                p.sendLinks(serverLinks);
            } catch (Throwable ignored) {}
        }
    }

    private void loadConfiguredLink(ConfigurationSection linkSec, String key, String defaultName, String defaultUrl, ServerLinks.Type defaultType, ServerLinks serverLinks) {
        if (linkSec == null) {
            String url = defaultUrl;
            if (key.equalsIgnoreCase("DISCORD") && plugin.getMessagesConfig() != null) {
                url = plugin.getMessagesConfig().getString("DISCORD.LINK", defaultUrl);
            } else if (key.equalsIgnoreCase("LOJA") && plugin.getMessagesConfig() != null) {
                url = plugin.getMessagesConfig().getString("LOJA.LINK", defaultUrl);
            }
            addLinkEntry(key, defaultName, url, defaultType, serverLinks, false);
            return;
        }

        if (!linkSec.getBoolean("ENABLED", true)) return;

        String name = linkSec.getString("NAME", defaultName);
        String url = linkSec.getString("URL", defaultUrl);
        if (key.equalsIgnoreCase("DISCORD") && (url == null || url.isEmpty()) && plugin.getMessagesConfig() != null) {
            url = plugin.getMessagesConfig().getString("DISCORD.LINK", defaultUrl);
        } else if (key.equalsIgnoreCase("LOJA") && (url == null || url.isEmpty()) && plugin.getMessagesConfig() != null) {
            url = plugin.getMessagesConfig().getString("LOJA.LINK", defaultUrl);
        }

        String typeStr = linkSec.getString("TYPE", defaultType != null ? defaultType.name() : null);
        ServerLinks.Type type = parseType(typeStr);
        boolean useBuiltin = linkSec.getBoolean("USE-BUILTIN-TRANSLATION", false);

        addLinkEntry(key, name, url, type, serverLinks, useBuiltin);
    }

    private void addLinkEntry(String key, String name, String rawUrl, ServerLinks.Type type, ServerLinks serverLinks, boolean useBuiltinTranslation) {
        URI uri = parseUri(rawUrl);
        if (uri == null) return;

        activeLinks.add(new LinkEntry(key, name, uri.toString(), type));

        try {
            if (useBuiltinTranslation && type != null) {
                serverLinks.setLink(type, uri);
            } else {
                Component displayName = plugin.parseMiniMessageOrLegacy(name);
                serverLinks.addLink(displayName, uri);
            }
        } catch (Throwable t) {
            try {
                serverLinks.addLink(DonutCore.color(name), uri);
            } catch (Throwable t2) {
                plugin.getLogger().warning("DonutCore: Falha ao adicionar ServerLink " + name + ": " + t2.getMessage());
            }
        }
    }

    private ServerLinks.Type parseType(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty() || typeStr.equalsIgnoreCase("CUSTOM")) return null;
        try {
            return ServerLinks.Type.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private URI parseUri(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return null;
        String url = rawUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            return URI.create(url);
        } catch (Throwable t) {
            plugin.getLogger().warning("DonutCore: URL inválida no ServerLinks: '" + rawUrl + "'");
            return null;
        }
    }

    private void clearBukkitLinks() {
        try {
            ServerLinks serverLinks = Bukkit.getServerLinks();
            if (serverLinks != null) {
                List<ServerLinks.ServerLink> current = new ArrayList<>(serverLinks.getLinks());
                for (ServerLinks.ServerLink link : current) {
                    serverLinks.removeLink(link);
                }
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("SERVER-LINKS.ENABLED", true)) return;
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                try {
                    ServerLinks links = Bukkit.getServerLinks();
                    if (links != null) {
                        p.sendLinks(links);
                    }
                } catch (Throwable ignored) {}
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLinksSend(PlayerLinksSendEvent event) {
        if (!plugin.getConfig().getBoolean("SERVER-LINKS.ENABLED", true)) return;
    }

    /**
     * Abre a caixa de diálogo nativa do Minecraft (Dialog API) com os links do servidor caso necessário via API.
     */
    public boolean openDialog(Player player) {
        if (player == null || !player.isOnline()) return false;
        try {
            Class<?> dialogClass = Class.forName("io.papermc.paper.dialog.Dialog");
            Field field = dialogClass.getField("SERVER_LINKS");
            Object serverLinksDialog = field.get(null);

            Class<?> dialogLikeClass = Class.forName("net.kyori.adventure.dialog.DialogLike");
            Method showDialogMethod = null;
            try {
                showDialogMethod = player.getClass().getMethod("showDialog", dialogLikeClass);
            } catch (NoSuchMethodException e) {
                showDialogMethod = net.kyori.adventure.audience.Audience.class.getMethod("showDialog", dialogLikeClass);
            }

            if (showDialogMethod != null) {
                showDialogMethod.invoke(player, serverLinksDialog);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public String getLinkUrl(String key, String fallback) {
        for (LinkEntry entry : activeLinks) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getUrl();
            }
        }
        return fallback;
    }

    public List<LinkEntry> getActiveLinks() {
        return Collections.unmodifiableList(activeLinks);
    }
}

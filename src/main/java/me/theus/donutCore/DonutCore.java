package me.theus.donutCore;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.*;
import org.bukkit.enchantments.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import fr.mrmicky.fastboard.FastBoard;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import me.theus.donutCore.fastboard.AdventureFastBoard;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@SuppressWarnings({"all", "unchecked", "rawtypes", "deprecation", "removal", "unused", "SpellCheckingInspection", "ConstantConditions", "DuplicatedCode", "ResultOfMethodCallIgnored", "CallToPrintStackTrace"})
public final class DonutCore extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static DonutCore instance;
    private FileConfiguration menuConfig;
    private FileConfiguration scoreboardConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration deathsConfig;
    private FileConfiguration soundsConfig;
    private FileConfiguration rtpConfig;
    private FileConfiguration worthConfig;
    private FileConfiguration shopConfig;
    private FileConfiguration cuboidsConfig;
    private FileConfiguration warpsConfig;

    private me.theus.donutCore.listeners.SellPacketListener sellPacketListener;
    private me.theus.donutCore.listeners.SellPacketEventsListener sellPacketEventsListener;
    private me.theus.donutCore.listeners.SellInventoryListener sellInventoryListener;
    private me.theus.donutCore.managers.ServerLinksManager serverLinksManager;

    public me.theus.donutCore.managers.ServerLinksManager getServerLinksManager() {
        return serverLinksManager;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    // Managers & Data
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, Team> teams = new ConcurrentHashMap<>();
    private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();
    private final Map<UUID, Location> teleportTasks = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> tpaRequests = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();
    private final Map<UUID, Long> shardBoosters = new ConcurrentHashMap<>();
    private final Map<UUID, Double> bounties = new ConcurrentHashMap<>();
    private final Map<UUID, AdventureFastBoard> boards = new ConcurrentHashMap<>();
    private final Map<UUID, String> teamInvites = new ConcurrentHashMap<>();
    private final Map<UUID, String> searchPrompt = new ConcurrentHashMap<>();
    private final Map<UUID, Location> tempSigns = new ConcurrentHashMap<>();
    private final Map<UUID, BlockData> oldBlockData = new ConcurrentHashMap<>();

    public static class ShopPurchase {
        public String itemKey;
        public double pricePerUnit;
        public String currency;
        public Material material;
        public ItemStack itemStack;
        public int quantity = 1;
        public String menuName;
        public String command = "";
        public ShopPurchase() {}
        public ShopPurchase(String itemKey, double pricePerUnit, String currency, Material material, int quantity) {
            this.itemKey = itemKey;
            this.pricePerUnit = pricePerUnit;
            this.currency = currency;
            this.material = material;
            this.itemStack = new ItemStack(material != null ? material : Material.STONE, quantity);
            this.quantity = quantity;
        }
        public ShopPurchase(String itemKey, double pricePerUnit, String currency, ItemStack itemStack, int quantity) {
            this.itemKey = itemKey;
            this.pricePerUnit = pricePerUnit;
            this.currency = currency;
            this.material = itemStack != null ? itemStack.getType() : Material.STONE;
            this.itemStack = itemStack;
            this.quantity = quantity;
        }
    }

    public static class PendingAction {
        public UUID targetUUID;
        public double amount;
        public String extraData;
        public Location targetLocation;
        public PendingAction(UUID targetUUID, double amount, String extraData) {
            this(targetUUID, amount, extraData, null);
        }
        public PendingAction(UUID targetUUID, double amount, String extraData, Location targetLocation) {
            this.targetUUID = targetUUID;
            this.amount = amount;
            this.extraData = extraData;
            this.targetLocation = targetLocation;
        }
    }

    private final Map<UUID, UUID> editingTeammate = new ConcurrentHashMap<>();
    private final Map<UUID, String> viewingLeaderboardType = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> viewingLeaderboardPage = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> viewingLeaderboardAscending = new ConcurrentHashMap<>();
    private final Map<UUID, String> viewingProgressCategory = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> viewingProgressPage = new ConcurrentHashMap<>();
    private final Map<UUID, String> viewingHistorySort = new ConcurrentHashMap<>();
    private final Map<UUID, String> viewingWorthSort = new ConcurrentHashMap<>();
    private final Map<UUID, String> viewingWorthFilter = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> viewingStatsProfile = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> viewingPlayerManagerProfile = new ConcurrentHashMap<>();

    private final Map<UUID, String> viewingHistoryFilter = new ConcurrentHashMap<>();
    private final Map<UUID, String> viewingBountySort = new ConcurrentHashMap<>();
    private final Map<UUID, String> viewingTeamFilter = new ConcurrentHashMap<>();
    private final Map<UUID, ShopPurchase> activePurchases = new ConcurrentHashMap<>();
    private final Map<UUID, PendingAction> pendingActions = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeMenuSearch = new ConcurrentHashMap<>();
    private final Set<UUID> activeRtpPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> rtpCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Location> activeEnderChestBlocks = new ConcurrentHashMap<>();
    private me.theus.donutCore.database.DatabaseManager dbManager;

    private final Map<UUID, Long> pendingHeldSeconds = new ConcurrentHashMap<>();
    private final Map<UUID, Location> wandPos1 = new ConcurrentHashMap<>();
    private final Map<UUID, Location> wandPos2 = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> afkTimeSeconds = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivityMillis = new ConcurrentHashMap<>();
    private NamespacedKey toolKey;
    private NamespacedKey wandKey;
    private NamespacedKey amethystExpireKey;
    private NamespacedKey amethystDurationKey;
    private NamespacedKey amethystRemainingSecondsKey;
    private int keyallCountdown = 3600;
    private int clearlagCountdown = 300;
    private int rtpZoneCountdown = -1;
    private int lunarPresenceCountdown = 0;
    private final Map<UUID, UUID> crystalDamagers = new ConcurrentHashMap<>();
    private final Map<Location, UUID> anchorExploders = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> openTopInventorySizes = new ConcurrentHashMap<>();

    public void setOpenTopInventorySize(UUID uuid, int size) {
        if (uuid == null) return;
        if (size > 0) {
            openTopInventorySizes.put(uuid, size);
        } else {
            openTopInventorySizes.remove(uuid);
        }
    }


    @Override
    public void onEnable() {
        instance = this;
        this.toolKey = new NamespacedKey(this, "donut_tool");
        this.wandKey = new NamespacedKey(this, "donut_wand");
        this.amethystExpireKey = new NamespacedKey(this, "donut_tool_expire");
        this.amethystDurationKey = new NamespacedKey(this, "donut_tool_duration");
        this.amethystRemainingSecondsKey = new NamespacedKey(this, "donut_tool_remaining_seconds");

        saveDefaultConfig();
        loadAllConfigs();
        loadTeams();
        loadProfiles();
        loadCuboids();
        loadWarps();


        // Register Events
        getServer().getPluginManager().registerEvents(this, this);

        keyallCountdown = Math.max(1, getConfig().getInt("KEY-ALL.EVERY", getConfig().getInt("KEY-ALL.TIME", 60))) * 60;

        // Register Commands
        String[] cmds = {"ec", "echest", "endersee", "setspawn", "spawn", "afk", "team", "home", "homes", "sethome", "delhome", "rtp", "sell", "worth", "stats",
                "settings", "leaderboards", "tpa", "tpahere", "tpaccept", "tpdeny", "bounty", "shop",
                "rules", "tpauto", "phantom", "clearlag", "pay", "donutcore",
                "midia", "sellhistory", "help", "fragmentos", "fragmentopay", "findplayer", "discord", "loja",
                "nightvision", "tell", "r", "ignore", "blocktell", "shardmanager", "moneymanager", "playermanager", "stash", "warp", "warpmanager"};
        for (String cmd : cmds) {
            PluginCommand pc = getCommand(cmd);
            if (pc != null) {
                pc.setExecutor(this);
                pc.setTabCompleter(this);
            }
        }

        // Initialize ServerLinks & Dialog API manager
        try {
            this.serverLinksManager = new me.theus.donutCore.managers.ServerLinksManager(this);
            this.serverLinksManager.registerLinks();
            getServer().getPluginManager().registerEvents(this.serverLinksManager, this);
        } catch (Throwable t) {
            getLogger().warning("DonutCore: Falha ao inicializar ServerLinksManager: " + t.getMessage());
        }

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new DonutVaultEconomy(this), this, org.bukkit.plugin.ServicePriority.Highest);
                getLogger().info("DonutCore: Vault Economy registrado com sucesso!");
            } catch (Throwable t) {
                getLogger().warning("DonutCore: Erro ao registrar Vault Economy: " + t.getMessage());
            }
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new DonutCoreExpansion(this).register();
                getLogger().info("DonutCore: PlaceholderAPI expansion registrado com sucesso!");
            } catch (Throwable t) {
                getLogger().warning("DonutCore: Erro ao registrar PlaceholderAPI expansion: " + t.getMessage());
            }
        }

        startTasks();

        this.sellInventoryListener = new me.theus.donutCore.listeners.SellInventoryListener(this);
        getServer().getPluginManager().registerEvents(sellInventoryListener, this);

        boolean packetHooked = false;
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            try {
                this.sellPacketListener = new me.theus.donutCore.listeners.SellPacketListener(this);
                this.sellPacketListener.register();
                getServer().getPluginManager().registerEvents(sellPacketListener, this);
                getLogger().info("DonutCore: ProtocolLib detectado! Injeção de preço no lore ativada via ProtocolLib.");
                packetHooked = true;
            } catch (Throwable t) {
                getLogger().warning("DonutCore: Erro ao registrar listener do ProtocolLib: " + t.getMessage());
            }
        }

        if (!packetHooked && getServer().getPluginManager().getPlugin("packetevents") != null) {
            try {
                this.sellPacketEventsListener = new me.theus.donutCore.listeners.SellPacketEventsListener(this);
                this.sellPacketEventsListener.register();
                getServer().getPluginManager().registerEvents(sellPacketEventsListener, this);
                getLogger().info("DonutCore: PacketEvents detectado! Injeção de preço no lore ativada via PacketEvents.");
                packetHooked = true;
            } catch (Throwable t) {
                getLogger().warning("DonutCore: Erro ao registrar listener do PacketEvents: " + t.getMessage());
            }
        }

        if (!packetHooked) {
            getLogger().warning("DonutCore: Nem ProtocolLib nem PacketEvents detectados. A injeção de preço no lore não estará ativa.");
        }

        getLogger().info("DonutCore carregado com sucesso! (All-in-One Plugin)");
    }

    @Override
    public void onDisable() {
        if (sellPacketListener != null) {
            try {
                sellPacketListener.unregister();
            } catch (Throwable ignored) {}
        }
        if (sellPacketEventsListener != null) {
            try {
                sellPacketEventsListener.unregister();
            } catch (Throwable ignored) {}
        }

        for (AdventureFastBoard board : boards.values()) {
            try { board.delete(); } catch (Throwable ignored) {}
        }
        boards.clear();
        saveProfiles();
        saveTeams();
        saveCuboids();
        saveWarps();
        if (dbManager != null) dbManager.close();
        getLogger().info("DonutCore desativado com sucesso!");
    }

    public static DonutCore getInstance() {
        return instance;
    }

    public int getKeyallCountdown() {
        return keyallCountdown;
    }

    public int getClearlagCountdown() {
        return clearlagCountdown;
    }

    public int getRtpZoneCountdown() {
        return rtpZoneCountdown;
    }

    public long getPlayerBoosterEnd(UUID uuid) {
        long end = shardBoosters.getOrDefault(uuid, 0L);
        if (end <= System.currentTimeMillis()) {
            shardBoosters.remove(uuid);
            return 0L;
        }
        return end;
    }

    private void saveConfigFileIfNotExists(String filename) {
        File file = new File(getDataFolder(), filename);
        if (!file.exists()) {
            try {
                if (!getDataFolder().exists()) getDataFolder().mkdirs();
                saveResource(filename, false);
                getLogger().info("DonutCore: Arquivo " + filename + " criado com sucesso!");
            } catch (Exception e) {
                getLogger().warning("DonutCore: Erro ao criar o arquivo " + filename + ": " + e.getMessage());
            }
        }
    }

    private YamlConfiguration loadConfigWithDefaults(String filename, boolean mergeDefaults) {
        File file = new File(getDataFolder(), filename);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!mergeDefaults) return config;
        java.io.InputStream stream = getResource(filename);
        if (stream != null) {
            try {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
                boolean changed = false;
                for (String key : defConfig.getKeys(true)) {
                    if (!config.contains(key, true)) {
                        config.set(key, defConfig.get(key));
                        changed = true;
                    }
                }
                if (changed) {
                    config.save(file);
                }
            } catch (Exception e) {
                getLogger().warning("DonutCore: Erro ao sincronizar defaults de " + filename + ": " + e.getMessage());
            }
        }
        return config;
    }

    public void loadAllConfigs() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        saveDefaultConfig();
        reloadConfig();

        try {
            java.io.InputStream stream = getResource("config.yml");
            if (stream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
                boolean changed = false;
                for (String key : defConfig.getKeys(true)) {
                    if (!getConfig().contains(key, true)) {
                        getConfig().set(key, defConfig.get(key));
                        changed = true;
                    }
                }
                if (changed) {
                    saveConfig();
                }
            }
        } catch (Throwable t) {
            getLogger().warning("DonutCore: Erro ao sincronizar defaults do config.yml: " + t.getMessage());
        }

        String[] configFiles = {
                "menu.yml", "scoreboard.yml", "messages.yml",
                "death-messages.yml", "sounds.yml", "rtp.yml",
                "worth.yml", "shop.yml"
        };
        for (String file : configFiles) {
            saveConfigFileIfNotExists(file);
        }

        menuConfig = loadConfigWithDefaults("menu.yml", false);
        scoreboardConfig = loadConfigWithDefaults("scoreboard.yml", false);
        messagesConfig = loadConfigWithDefaults("messages.yml", true);
        deathsConfig = loadConfigWithDefaults("death-messages.yml", true);
        soundsConfig = loadConfigWithDefaults("sounds.yml", true);
        rtpConfig = loadConfigWithDefaults("rtp.yml", false);
        worthConfig = loadConfigWithDefaults("worth.yml", false);
        shopConfig = loadConfigWithDefaults("shop.yml", false);
        saveConfigFileIfNotExists("database.yml");
        if (dbManager == null) {
            dbManager = new me.theus.donutCore.database.DatabaseManager(this);
            dbManager.init();
        }
    }

    public void reloadPlugin(CommandSender sender) {
        reloadConfig();
        loadAllConfigs();
        loadCuboids();
        loadWarps();
        Bukkit.getScheduler().cancelTasks(this);
        keyallCountdown = Math.max(1, getConfig().getInt("KEY-ALL.EVERY", getConfig().getInt("KEY-ALL.TIME", 60))) * 60;
        clearlagCountdown = Math.max(1, getConfig().getInt("CLEAR-LAG.EVERY", 5)) * 60;
        rtpZoneCountdown = Math.max(1, getConfig().getInt("RTP-ZONE.EVERY", 30));
        startTasks();
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.updateInventory();
        }
        if (this.serverLinksManager != null) {
            this.serverLinksManager.registerLinks();
        }
        if (sender != null) {
            String pingVal = (sender instanceof Player) ? ((Player) sender).getPing() + "ms" : "0ms";
            String msg = "&7Plugin carregado com sucesso &#FFF000{ping}";
            sender.sendMessage(color(msg.replace("{ping}", pingVal)));
        }
    }

    public static double getCategoryGoal(int level) {
        if (instance != null && instance.menuConfig != null && instance.menuConfig.contains("PROGRESS-MENU.LEVEL")) {
            List<Long> levels = instance.menuConfig.getLongList("PROGRESS-MENU.LEVEL");
            int idx = level - 1;
            if (idx >= 0 && idx < levels.size()) {
                return (double) levels.get(idx);
            } else if (!levels.isEmpty()) {
                long last = levels.get(levels.size() - 1);
                return (double) (last * 2L * (idx - levels.size() + 1));
            }
        }
        return 25000 * Math.pow(6, level - 1);
    }

    // ==========================================
    // DATA CLASSES & STORAGE
    // ==========================================
    public static class PlayerProfile {
        public UUID uuid;
        public String username;
        public double money = 1000.0;
        public long shards = 0;
        public int kills = 0;
        public int deaths = 0;
        public long playtimeSeconds = 0;
        public String teamName = "";
        public boolean tpAuto = false;
        public boolean phantomDisabled = false;
        public boolean scoreboardVisible = true;
        public boolean tpaEnabled = true;
        public boolean teamInvitesEnabled = true;
        public boolean payAlerts = true;
        public boolean hotbarMessages = true;
        public boolean clearEntitiesMessages = false;
        public boolean bountyAlerts = true;
        public boolean chainmailOnRespawn = true;
        public boolean tpaConfirmMenus = true;
        public boolean lunarTeammates = true;
        public boolean tpaHereEnabled = true;
        public boolean paymentsEnabled = true;
        public boolean teamChatEnabled = true;
        public boolean disableMobSpawn = false;
        public boolean payConfirmMenus = true;
        public boolean totemParticles = true;
        public boolean fastCrystals = true;
        public boolean permEditHome = false;
        public boolean permManageTeammates = false;
        public boolean permTogglePvp = false;
        public boolean permVisitHome = true;
        public boolean permTeamChat = true;
        public boolean permHelper = false;
        public boolean nightVisionEnabled = false;
        public boolean blockTell = false;
        public Set<UUID> ignoredPlayers = new HashSet<>();
        public long blocksPlaced = 0;
        public long blocksBroken = 0;
        public long mobsKilled = 0;
        public int killStreak = 0;
        public int highestKillStreak = 0;
        public double moneySpent = 0.0;
        public double moneyMade = 0.0;
        public Map<String, Location> homes = new LinkedHashMap<>();
        public Map<String, Integer> sellMultipliers = new HashMap<>();
        public Map<String, Double> categoryProgress = new HashMap<>();
        public Map<String, Double> soldPriceHistory = new ConcurrentHashMap<>();
        public Map<String, Integer> soldAmountHistory = new ConcurrentHashMap<>();
        public Map<String, Long> lastSoldTime = new ConcurrentHashMap<>();
        public ItemStack[] enderchestContents = new ItemStack[54];
        public Location lastLocation = null;


        public PlayerProfile(UUID uuid, String username) {
            this.uuid = uuid;
            this.username = username;
            String[] categories = {"CROPS", "ORES", "MOBS", "NATURAL", "ARMOR_AND_TOOLS", "FISH", "BOOK", "POTION", "BLOCKS"};
            for (String cat : categories) {
                sellMultipliers.put(cat, 1);
                categoryProgress.put(cat, 0.0);
            }
        }

        public double getMultiplier(String category) {
            if (category != null && category.equalsIgnoreCase("POTIONS")) category = "POTION";
            int level = sellMultipliers.getOrDefault(category != null ? category.toUpperCase() : "CROPS", 1);
            return 1.0 + ((level - 1) * 0.25);
        }

        public void addSellProgress(String category, double earned) {
            if (category == null) category = "CROPS";
            if (category.equalsIgnoreCase("POTIONS")) category = "POTION";
            category = category.replace("-", "_").toUpperCase();
            double current = categoryProgress.getOrDefault(category, 0.0) + earned;
            int level = sellMultipliers.getOrDefault(category, 1);
            int maxLevel = 20;
            if (level >= maxLevel) {
                sellMultipliers.put(category, maxLevel);
                categoryProgress.put(category, DonutCore.getCategoryGoal(maxLevel));
                return;
            }
            int oldLevel = level;
            double nextGoal = DonutCore.getCategoryGoal(level);
            while (current >= nextGoal && level < maxLevel) {
                current -= nextGoal;
                level++;
                sellMultipliers.put(category, level);
                if (level >= maxLevel) {
                    current = DonutCore.getCategoryGoal(maxLevel);
                    break;
                }
                nextGoal = DonutCore.getCategoryGoal(level);
            }
            categoryProgress.put(category, current);
            if (earned > 0.0 && level > oldLevel) {
                Player p = Bukkit.getPlayer(this.uuid);
                if (p != null && DonutCore.instance != null && DonutCore.instance.messagesConfig != null) {
                    List<String> lines = DonutCore.instance.messagesConfig.getStringList("SELL-MULTIPLIER.LEVEL-UP");
                    if (lines == null || lines.isEmpty()) {
                        lines = Arrays.asList(
                            "",
                            "&#6BF18Dѕᴇʟʟ ᴍᴜʟᴛɪᴘʟɪᴇʀ",
                            "&7Your sell multiplier for &#6BF18D{category} &7has leveled up!",
                            "&7New Level: &#6BF18D{level} &8| &7New Multiplier: &#6BF18D{multiplier}x",
                            ""
                        );
                    }
                    String catDisplay = category;
                    if (category.equalsIgnoreCase("CROPS")) catDisplay = "Crops";
                    else if (category.equalsIgnoreCase("ORES")) catDisplay = "Ores";
                    else if (category.equalsIgnoreCase("MOBS")) catDisplay = "Mobs";
                    else if (category.equalsIgnoreCase("NATURAL")) catDisplay = "Natural";
                    else if (category.equalsIgnoreCase("ARMOR_AND_TOOLS")) catDisplay = "Armor & Tools";
                    else if (category.equalsIgnoreCase("FISH")) catDisplay = "Fish";
                    else if (category.equalsIgnoreCase("BOOK")) catDisplay = "Book";
                    else if (category.equalsIgnoreCase("POTION")) catDisplay = "Potion";
                    else if (category.equalsIgnoreCase("BLOCKS")) catDisplay = "Blocks";

                    String multStr = String.format(Locale.US, "%.2f", getMultiplier(category));
                    for (String l : lines) {
                        l = l.replace("{category}", catDisplay)
                             .replace("{level}", String.valueOf(level))
                             .replace("{multiplier}", multStr);
                        p.sendMessage(DonutCore.color(l));
                    }
                    DonutCore.instance.playSound(p, "PLAYER.LEVEL-UP");
                }
            }
        }

        public void recalculateAllLevels() {
            for (String cat : new ArrayList<>(categoryProgress.keySet())) {
                addSellProgress(cat, 0.0);
            }
        }
    }

    public static class Team {
        public String name;
        public UUID leader;
        public Set<UUID> members = new HashSet<>();
        public Location home;
        public boolean pvp = false;
        public boolean teamChat = false;

        public Team(String name, UUID leader) {
            this.name = name;
            this.leader = leader;
            this.members.add(leader);
        }
    }

    private void loadProfiles() {
        if (dbManager != null && dbManager.isSql()) {
            dbManager.loadAllProfiles(profiles);
        }
        File folder = new File(getDataFolder(), "database/profiles");
        File oldFolder = new File(getDataFolder(), "profiles");
        if (oldFolder.exists() && oldFolder.isDirectory()) loadProfilesFromDir(oldFolder);
        if (folder.exists() && folder.isDirectory()) loadProfilesFromDir(folder);
    }

    private void loadProfilesFromDir(File folder) {
        if (!folder.exists() || !folder.isDirectory()) return;
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.getName().endsWith(".yml")) {
                YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
                try {
                    String nameWithoutExt = file.getName().replace(".yml", "");
                    String uuidStr = nameWithoutExt.contains(" - ") ? nameWithoutExt.split(" - ")[0] : nameWithoutExt;
                    UUID uuid = UUID.fromString(uuidStr);
                    if (profiles.containsKey(uuid)) continue;
                    PlayerProfile prof = new PlayerProfile(uuid, conf.getString("username", "Unknown"));
                    prof.money = conf.getDouble("money", 1000.0);
                    prof.shards = conf.getLong("shards", 0);
                    prof.kills = conf.getInt("kills", 0);
                    prof.deaths = conf.getInt("deaths", 0);
                    prof.playtimeSeconds = conf.getLong("playtime", 0);
                    prof.blocksPlaced = conf.getLong("blocksPlaced", 0);
                    prof.blocksBroken = conf.getLong("blocksBroken", 0);
                    prof.mobsKilled = conf.getLong("mobsKilled", 0);
                    prof.killStreak = conf.getInt("killStreak", 0);
                    prof.highestKillStreak = conf.getInt("highestKillStreak", 0);
                    prof.moneySpent = conf.getDouble("moneySpent", 0.0);
                    prof.moneyMade = conf.getDouble("moneyMade", 0.0);
                    prof.teamName = conf.getString("team", "");
                    prof.tpAuto = conf.getBoolean("tpAuto", false);
                    prof.phantomDisabled = conf.getBoolean("phantomDisabled", false);
                    prof.scoreboardVisible = conf.getBoolean("scoreboardVisible", true);
                    prof.tpaEnabled = conf.getBoolean("tpaEnabled", true);
                    prof.teamInvitesEnabled = conf.getBoolean("teamInvitesEnabled", true);
                    prof.payAlerts = conf.getBoolean("payAlerts", true);
                    prof.hotbarMessages = conf.getBoolean("hotbarMessages", true);
                    prof.clearEntitiesMessages = conf.getBoolean("clearEntitiesMessages", false);
                    prof.bountyAlerts = conf.getBoolean("bountyAlerts", true);
                    prof.chainmailOnRespawn = conf.getBoolean("chainmailOnRespawn", true);
                    prof.tpaConfirmMenus = conf.getBoolean("tpaConfirmMenus", true);
                    prof.lunarTeammates = conf.getBoolean("lunarTeammates", true);
                    prof.tpaHereEnabled = conf.getBoolean("tpaHereEnabled", true);
                    prof.paymentsEnabled = conf.getBoolean("paymentsEnabled", true);
                    prof.teamChatEnabled = conf.getBoolean("teamChatEnabled", true);
                    prof.disableMobSpawn = conf.getBoolean("disableMobSpawn", false);
                    prof.payConfirmMenus = conf.getBoolean("payConfirmMenus", true);
                    prof.totemParticles = conf.getBoolean("totemParticles", true);
                    prof.fastCrystals = conf.getBoolean("fastCrystals", true);
                    prof.permEditHome = conf.getBoolean("permEditHome", false);
                    prof.permManageTeammates = conf.getBoolean("permManageTeammates", false);
                    prof.permTogglePvp = conf.getBoolean("permTogglePvp", false);
                    prof.permVisitHome = conf.getBoolean("permVisitHome", true);
                    prof.permTeamChat = conf.getBoolean("permTeamChat", true);
                    prof.permHelper = conf.getBoolean("permHelper", false);
                    prof.nightVisionEnabled = conf.getBoolean("nightVisionEnabled", false);
                    prof.blockTell = conf.getBoolean("blockTell", false);
                    List<String> ignList = conf.getStringList("ignoredPlayers");
                    if (ignList != null) {
                        for (String idStr : ignList) {
                            try { prof.ignoredPlayers.add(UUID.fromString(idStr)); } catch (Exception ignored) {}
                        }
                    }

                    ConfigurationSection homesSec = conf.getConfigurationSection("homes");
                    if (homesSec != null) {
                        for (String key : homesSec.getKeys(false)) {
                            prof.homes.put(key, homesSec.getLocation(key));
                        }
                    }
                    ConfigurationSection multSec = conf.getConfigurationSection("multipliers");
                    if (multSec != null) {
                        for (String key : multSec.getKeys(false)) {
                            prof.sellMultipliers.put(key, multSec.getInt(key, 1));
                        }
                    }
                    ConfigurationSection progSec = conf.getConfigurationSection("progress");
                    if (progSec != null) {
                        for (String key : progSec.getKeys(false)) {
                            prof.categoryProgress.put(key, progSec.getDouble(key, 0.0));
                        }
                    }
                    ConfigurationSection sphSec = conf.getConfigurationSection("soldPriceHistory");
                    if (sphSec != null) {
                        for (String key : sphSec.getKeys(false)) {
                            prof.soldPriceHistory.put(key, sphSec.getDouble(key, 0.0));
                        }
                    }
                    ConfigurationSection sahSec = conf.getConfigurationSection("soldAmountHistory");
                    if (sahSec != null) {
                        for (String key : sahSec.getKeys(false)) {
                            prof.soldAmountHistory.put(key, sahSec.getInt(key, 0));
                        }
                    }
                    ConfigurationSection lstSec = conf.getConfigurationSection("lastSoldTime");
                    if (lstSec != null) {
                        for (String key : lstSec.getKeys(false)) {
                            prof.lastSoldTime.put(key, lstSec.getLong(key, 0L));
                        }
                    }
                    List<?> ecLoaded = conf.getList("enderchestContents");
                    if (ecLoaded != null) {
                        for (int i = 0; i < ecLoaded.size() && i < 54; i++) {
                            Object obj = ecLoaded.get(i);
                            if (obj instanceof ItemStack) {
                                prof.enderchestContents[i] = (ItemStack) obj;
                            }
                        }
                    }
                    prof.recalculateAllLevels();
                    profiles.put(uuid, prof);
                } catch (Exception e) {
                    getLogger().warning("Erro ao carregar perfil: " + file.getName());
                }
            }
        }
    }

    public void saveProfiles() {
        if (profiles.isEmpty()) return;
        int count = 0;
        for (PlayerProfile prof : profiles.values()) {
            saveProfile(prof);
            count++;
        }
        if (getConfig().getBoolean("PROFILE.ENABLED", true)) {
            String msg = getConfig().getString("PROFILE.CONSOLE-MESSAGE", "&b&l[Profile] &a{profiles} &fprofiles were saved.");
            getLogger().info(ChatColor.stripColor(color(msg.replace("{profiles}", String.valueOf(count)))));
        }
    }

    public void saveProfile(PlayerProfile prof) {
        if (prof == null) return;
        if (!isEnabled()) {
            saveProfileSync(prof);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> saveProfileSync(prof));
        }
    }

    private void saveProfileSync(PlayerProfile prof) {
        if (prof == null) return;
        synchronized (prof) {
            if (dbManager != null && dbManager.isSql()) {
                dbManager.saveProfile(prof);
                return;
            }
            File folder = new File(getDataFolder(), "profiles");
            if (!folder.exists()) folder.mkdirs();
            File file = new File(folder, prof.uuid.toString() + " - " + prof.username + ".yml");
            YamlConfiguration conf = new YamlConfiguration();
            conf.set("uuid", prof.uuid.toString());
            conf.set("username", prof.username);
            conf.set("money", prof.money);
            conf.set("shards", prof.shards);
            conf.set("kills", prof.kills);
            conf.set("deaths", prof.deaths);
            conf.set("playtime", prof.playtimeSeconds);
            conf.set("blocksPlaced", prof.blocksPlaced);
            conf.set("blocksBroken", prof.blocksBroken);
            conf.set("mobsKilled", prof.mobsKilled);
            conf.set("killStreak", prof.killStreak);
            conf.set("highestKillStreak", prof.highestKillStreak);
            conf.set("moneySpent", prof.moneySpent);
            conf.set("moneyMade", prof.moneyMade);
            conf.set("team", prof.teamName);
            conf.set("tpAuto", prof.tpAuto);
            conf.set("phantomDisabled", prof.phantomDisabled);
            conf.set("scoreboardVisible", prof.scoreboardVisible);
            conf.set("tpaEnabled", prof.tpaEnabled);
            conf.set("teamInvitesEnabled", prof.teamInvitesEnabled);
            conf.set("payAlerts", prof.payAlerts);
            conf.set("hotbarMessages", prof.hotbarMessages);
            conf.set("clearEntitiesMessages", prof.clearEntitiesMessages);
            conf.set("bountyAlerts", prof.bountyAlerts);
            conf.set("chainmailOnRespawn", prof.chainmailOnRespawn);
            conf.set("tpaConfirmMenus", prof.tpaConfirmMenus);
            conf.set("lunarTeammates", prof.lunarTeammates);
            conf.set("tpaHereEnabled", prof.tpaHereEnabled);
            conf.set("paymentsEnabled", prof.paymentsEnabled);
            conf.set("teamChatEnabled", prof.teamChatEnabled);
            conf.set("disableMobSpawn", prof.disableMobSpawn);
            conf.set("payConfirmMenus", prof.payConfirmMenus);
            conf.set("totemParticles", prof.totemParticles);
            conf.set("fastCrystals", prof.fastCrystals);
            conf.set("permEditHome", prof.permEditHome);
            conf.set("permManageTeammates", prof.permManageTeammates);
            conf.set("permTogglePvp", prof.permTogglePvp);
            conf.set("permVisitHome", prof.permVisitHome);
            conf.set("permTeamChat", prof.permTeamChat);
            conf.set("permHelper", prof.permHelper);
            conf.set("nightVisionEnabled", prof.nightVisionEnabled);
            conf.set("blockTell", prof.blockTell);
            conf.set("ignoredPlayers", prof.ignoredPlayers.stream().map(UUID::toString).collect(Collectors.toList()));
            for (Map.Entry<String, Location> e : prof.homes.entrySet()) {
                conf.set("homes." + e.getKey(), e.getValue());
            }
            for (Map.Entry<String, Integer> e : prof.sellMultipliers.entrySet()) {
                conf.set("multipliers." + e.getKey(), e.getValue());
            }
            for (Map.Entry<String, Double> e : prof.categoryProgress.entrySet()) {
                conf.set("progress." + e.getKey(), e.getValue());
            }
            for (Map.Entry<String, Double> e : prof.soldPriceHistory.entrySet()) {
                conf.set("soldPriceHistory." + e.getKey(), e.getValue());
            }
            for (Map.Entry<String, Integer> e : prof.soldAmountHistory.entrySet()) {
                conf.set("soldAmountHistory." + e.getKey(), e.getValue());
            }
            for (Map.Entry<String, Long> e : prof.lastSoldTime.entrySet()) {
                conf.set("lastSoldTime." + e.getKey(), e.getValue());
            }
            List<ItemStack> ecList = new ArrayList<>();
            if (prof.enderchestContents != null) {
                for (ItemStack is : prof.enderchestContents) {
                    ecList.add(is);
                }
            }
            conf.set("enderchestContents", ecList);
            try {
                conf.save(file);
            } catch (IOException e) {
                getLogger().warning("Erro ao salvar perfil de: " + prof.username);
            }
        }
    }

    private void loadTeams() {
        if (dbManager != null && dbManager.isSql()) {
            dbManager.loadAllTeams(teams);
        }
        File folder = new File(getDataFolder(), "teams");

        // Compatibility: load from old database/teams and teams.yml if exists
        File oldDbFolder = new File(getDataFolder(), "database/teams");
        if (oldDbFolder.exists() && oldDbFolder.isDirectory()) {
            File[] oldFiles = oldDbFolder.listFiles();
            if (oldFiles != null) {
                for (File file : oldFiles) {
                    if (file.getName().endsWith(".yml")) {
                        YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
                        String tName = conf.getString("name", file.getName().replace(".yml", ""));
                        try {
                            UUID leader = UUID.fromString(Objects.requireNonNull(conf.getString("leader")));
                            Team team = new Team(tName, leader);
                            team.pvp = conf.getBoolean("pvp", false);
                            team.home = conf.getLocation("home");
                            List<String> mems = conf.getStringList("members");
                            for (String m : mems) team.members.add(UUID.fromString(m));
                            teams.put(tName.toLowerCase(), team);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        File oldFile = new File(getDataFolder(), "teams.yml");
        if (oldFile.exists()) {
            YamlConfiguration conf = YamlConfiguration.loadConfiguration(oldFile);
            for (String tName : conf.getKeys(false)) {
                ConfigurationSection sec = conf.getConfigurationSection(tName);
                if (sec != null) {
                    try {
                        UUID leader = UUID.fromString(Objects.requireNonNull(sec.getString("leader")));
                        Team team = new Team(tName, leader);
                        team.pvp = sec.getBoolean("pvp", false);
                        team.home = sec.getLocation("home");
                        List<String> mems = sec.getStringList("members");
                        for (String m : mems) team.members.add(UUID.fromString(m));
                        teams.put(tName.toLowerCase(), team);
                    } catch (Exception ignored) {}
                }
            }
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
                    String tName = conf.getString("name", file.getName().replace(".yml", ""));
                    try {
                        UUID leader = UUID.fromString(Objects.requireNonNull(conf.getString("leader")));
                        Team team = new Team(tName, leader);
                        team.pvp = conf.getBoolean("pvp", false);
                        team.home = conf.getLocation("home");
                        List<String> mems = conf.getStringList("members");
                        for (String m : mems) team.members.add(UUID.fromString(m));
                        teams.put(tName.toLowerCase(), team);
                    } catch (Exception e) {
                        getLogger().warning("Erro ao carregar time: " + file.getName());
                    }
                }
            }
        }
    }

    private void saveTeams() {
        if (!isEnabled()) {
            saveTeamsSync();
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, this::saveTeamsSync);
        }
    }

    private void saveTeamsSync() {
        if (dbManager != null && dbManager.isSql()) {
            for (Team team : teams.values()) {
                dbManager.saveTeam(team);
            }
            return;
        }
        if (teams.isEmpty()) return;
        File folder = new File(getDataFolder(), "teams");
        if (!folder.exists()) folder.mkdirs();
        for (Team team : teams.values()) {
            File file = new File(folder, team.name + ".yml");
            YamlConfiguration conf = new YamlConfiguration();
            conf.set("name", team.name);
            conf.set("leader", team.leader.toString());
            conf.set("pvp", team.pvp);
            conf.set("home", team.home);
            conf.set("members", team.members.stream().map(UUID::toString).collect(Collectors.toList()));
            try {
                conf.save(file);
            } catch (IOException e) {
                getLogger().warning("Erro ao salvar time: " + team.name);
            }
        }
    }

    private void deleteTeamFile(String teamName) {
        if (dbManager != null && dbManager.isSql()) {
            dbManager.deleteTeam(teamName);
        }
        File file = new File(getDataFolder(), "teams/" + teamName + ".yml");
        if (file.exists()) file.delete();
        File oldFile = new File(getDataFolder(), "database/teams/" + teamName + ".yml");
        if (oldFile.exists()) oldFile.delete();
    }

    private FileConfiguration getCuboidsConfig() {
        return cuboidsConfig != null ? cuboidsConfig : getConfig();
    }

    private void loadCuboids() {
        if (dbManager != null && dbManager.isSql()) {
            if (cuboidsConfig == null) cuboidsConfig = new YamlConfiguration();
            dbManager.loadCuboids(cuboidsConfig);
            return;
        }
        File folder = new File(getDataFolder(), "database");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "cuboids.yml");
        File oldFile = new File(getDataFolder(), "cuboids.yml");
        if (file.exists()) {
            cuboidsConfig = YamlConfiguration.loadConfiguration(file);
        } else if (oldFile.exists()) {
            cuboidsConfig = YamlConfiguration.loadConfiguration(oldFile);
            saveCuboids();
            oldFile.delete();
        } else {
            cuboidsConfig = new YamlConfiguration();
        }

        ConfigurationSection oldCubSec = getConfig().getConfigurationSection("CUBOIDS");
        if (oldCubSec != null && !oldCubSec.getKeys(false).isEmpty()) {
            for (String key : oldCubSec.getKeys(false)) {
                ConfigurationSection sec = oldCubSec.getConfigurationSection(key);
                if (sec != null && !cuboidsConfig.contains("CUBOIDS." + key)) {
                    cuboidsConfig.set("CUBOIDS." + key + ".WORLD", sec.getString("WORLD", "world"));
                    cuboidsConfig.set("CUBOIDS." + key + ".MIN-X", sec.getDouble("MIN-X"));
                    cuboidsConfig.set("CUBOIDS." + key + ".MIN-Y", sec.getDouble("MIN-Y"));
                    cuboidsConfig.set("CUBOIDS." + key + ".MIN-Z", sec.getDouble("MIN-Z"));
                    cuboidsConfig.set("CUBOIDS." + key + ".MAX-X", sec.getDouble("MAX-X"));
                    cuboidsConfig.set("CUBOIDS." + key + ".MAX-Y", sec.getDouble("MAX-Y"));
                    cuboidsConfig.set("CUBOIDS." + key + ".MAX-Z", sec.getDouble("MAX-Z"));
                }
            }
            getConfig().set("CUBOIDS", null);
            saveConfig();
            saveCuboids();
            getLogger().info("DonutCore: Migrated CUBOIDS from config.yml to cuboids.yml!");
        }
    }

    private void saveCuboids() {
        if (!isEnabled()) {
            saveCuboidsSync();
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, this::saveCuboidsSync);
        }
    }

    private void saveCuboidsSync() {
        if (dbManager != null && dbManager.isSql()) {
            dbManager.saveCuboids(cuboidsConfig);
            return;
        }
        File folder = new File(getDataFolder(), "database");
        if (!folder.exists()) folder.mkdirs();
        try {
            cuboidsConfig.save(new File(folder, "cuboids.yml"));
        } catch (IOException e) {
            getLogger().warning("Erro ao salvar cuboids.yml");
        }
    }

    private FileConfiguration getWarpsConfig() {
        return warpsConfig != null ? warpsConfig : getConfig();
    }

    private void loadWarps() {
        if (dbManager != null && dbManager.isSql()) {
            if (warpsConfig == null) warpsConfig = new YamlConfiguration();
            dbManager.loadWarps(warpsConfig);
            return;
        }
        File folder = new File(getDataFolder(), "database");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "warps.yml");
        if (file.exists()) {
            warpsConfig = YamlConfiguration.loadConfiguration(file);
        } else {
            warpsConfig = new YamlConfiguration();
        }
    }

    private void saveWarps() {
        if (!isEnabled()) {
            saveWarpsSync();
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, this::saveWarpsSync);
        }
    }

    private void saveWarpsSync() {
        if (dbManager != null && dbManager.isSql()) {
            dbManager.saveWarps(warpsConfig);
            return;
        }
        File folder = new File(getDataFolder(), "database");
        if (!folder.exists()) folder.mkdirs();
        try {
            warpsConfig.save(new File(folder, "warps.yml"));
        } catch (IOException e) {
            getLogger().warning("Erro ao salvar warps.yml");
        }
    }

    public PlayerProfile getProfile(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), k -> {
            PlayerProfile prof = new PlayerProfile(player.getUniqueId(), player.getName());
            prof.recalculateAllLevels();
            return prof;
        });
    }

    public PlayerProfile getProfile(OfflinePlayer player) {
        return profiles.computeIfAbsent(player.getUniqueId(), k -> {
            PlayerProfile prof = new PlayerProfile(player.getUniqueId(), player.getName() != null ? player.getName() : "Desconhecido");
            prof.recalculateAllLevels();
            return prof;
        });
    }

    public PlayerProfile getProfile(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return getProfile(op);
    }

    public java.util.Collection<PlayerProfile> getAllProfiles() {
        return profiles.values();
    }

    // ==========================================
    // UTILS & COLORS
    // ==========================================
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String color(String text) {
        if (text == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static Component parseMiniMessageOrLegacy(String text) {
        if (text == null) return Component.empty();
        String trimmed = text.trim();
        if (trimmed.startsWith("/tellraw ") || trimmed.startsWith("tellraw ")) {
            int jsonStart = trimmed.indexOf('{');
            if (jsonStart == -1) jsonStart = trimmed.indexOf('[');
            if (jsonStart != -1) {
                trimmed = trimmed.substring(jsonStart);
            }
        }
        if (trimmed.startsWith("{\"") || trimmed.startsWith("[\"") || trimmed.startsWith("{ \"") || trimmed.startsWith("[ \"")) {
            try {
                return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(trimmed);
            } catch (Throwable t) {
                try {
                    return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.colorDownsamplingGson().deserialize(trimmed);
                } catch (Throwable ignored) {}
            }
        }
        text = text.replaceAll("<([^>]+)>\\s*<\\1>", "<$1>");

        String resetDecorations = "<!bold><!italic><!underlined><!strikethrough><!obfuscated>";

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, resetDecorations + "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        text = sb.toString();

        Matcher hexLegacy = Pattern.compile("§x§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])").matcher(text);
        sb = new StringBuilder();
        while (hexLegacy.find()) {
            hexLegacy.appendReplacement(sb, resetDecorations + "<#" + hexLegacy.group(1) + hexLegacy.group(2) + hexLegacy.group(3) + hexLegacy.group(4) + hexLegacy.group(5) + hexLegacy.group(6) + ">");
        }
        hexLegacy.appendTail(sb);
        text = sb.toString();

        text = text.replaceAll("[&§]0", resetDecorations + "<black>")
                   .replaceAll("[&§]1", resetDecorations + "<dark_blue>")
                   .replaceAll("[&§]2", resetDecorations + "<dark_green>")
                   .replaceAll("[&§]3", resetDecorations + "<dark_aqua>")
                   .replaceAll("[&§]4", resetDecorations + "<dark_red>")
                   .replaceAll("[&§]5", resetDecorations + "<dark_purple>")
                   .replaceAll("[&§]6", resetDecorations + "<gold>")
                   .replaceAll("[&§]7", resetDecorations + "<gray>")
                   .replaceAll("[&§]8", resetDecorations + "<dark_gray>")
                   .replaceAll("[&§]9", resetDecorations + "<blue>")
                   .replaceAll("[&§][aA]", resetDecorations + "<green>")
                   .replaceAll("[&§][bB]", resetDecorations + "<aqua>")
                   .replaceAll("[&§][cC]", resetDecorations + "<red>")
                   .replaceAll("[&§][dD]", resetDecorations + "<light_purple>")
                   .replaceAll("[&§][eE]", resetDecorations + "<yellow>")
                   .replaceAll("[&§][fF]", resetDecorations + "<white>")
                   .replaceAll("[&§][kK]", "<obfuscated>")
                   .replaceAll("[&§][lL]", "<bold>")
                   .replaceAll("[&§][mM]", "<strikethrough>")
                   .replaceAll("[&§][nN]", "<underlined>")
                   .replaceAll("[&§][oO]", "<italic>")
                   .replaceAll("[&§][rR]", resetDecorations + "<white>");

        try {
            return MiniMessage.miniMessage().deserialize(text);
        } catch (Throwable t) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
    }

    public String getMsg(String path) {
        return color(messagesConfig.getString(path, "&cMensagem não encontrada: " + path));
    }

    public void playSound(Player p, String path) {
        if (soundsConfig == null || p == null) return;
        String val = soundsConfig.getString(path);
        if (val == null || val.isEmpty()) return;
        String[] parts = val.split("\\|");
        String rawName = parts[0];
        float vol = 1.0f;
        float pitch = 1.0f;
        if (parts.length > 1) {
            try { vol = Float.parseFloat(parts[1]); } catch (Exception ignored) {}
        }
        if (parts.length > 2) {
            try { pitch = Float.parseFloat(parts[2]); } catch (Exception ignored) {}
        }
        String enumName = rawName;
        if (enumName.startsWith("minecraft:")) enumName = enumName.substring(10);
        enumName = enumName.replace(".", "_").toUpperCase();
        try {
            Sound s = Sound.valueOf(enumName);
            p.playSound(p.getLocation(), s, vol, pitch);
        } catch (Exception e) {
            try {
                p.playSound(p.getLocation(), rawName, vol, pitch);
            } catch (Exception ignored) {}
        }
    }

    // ==========================================
    // SCHEDULERS (Scoreboard, ClearLag, AFK)
    // ==========================================
    private void broadcastClearLag(String msg) {
        if (msg == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerProfile prof = getProfile(p);
            if (prof != null && !prof.clearEntitiesMessages) continue;
            p.sendMessage(msg);
        }
    }

    private void broadcastBounty(String msg) {
        if (msg == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerProfile prof = getProfile(p);
            if (prof != null && !prof.bountyAlerts) continue;
            p.sendMessage(msg);
        }
    }

    private void startTasks() {
        // Save profiles every 15 mins
        int saveInterval = getConfig().getInt("PROFILE.SAVE-EVERY", 15) * 60 * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                saveProfiles();
                saveTeams();
            }
        }.runTaskTimer(this, saveInterval, saveInterval);

        // Scoreboard & ActionBar & Playtime & Tablist updater (Every second / 20 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                sbTitleIndex++;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerProfile prof = getProfile(p);
                    prof.playtimeSeconds++;
                    updateScoreboard(p, prof);
                    updateTablist(p);
                    checkCombatTag(p);
                    checkAmethystToolExpiration(p);
                    checkAfkZone(p, prof);
                    checkSpawnInactivityToAfk(p);
                    if (prof.nightVisionEnabled && !p.hasPotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION)) {
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                    }
                }
                checkRtpZoneTask();
                checkLunarClientTasks();
                checkKeyAllTask();
            }
        }.runTaskTimer(this, 20L, 20L);

        // ClearLag task (Every 5 mins)
        if (getConfig().getBoolean("CLEAR-LAG.ENABLED", true)) {
            int clInterval = getConfig().getInt("CLEAR-LAG.EVERY", 5) * 60;
            new BukkitRunnable() {
                @Override
                public void run() {
                    clearlagCountdown--;
                    if (clearlagCountdown == 60 || clearlagCountdown == 30 || clearlagCountdown == 10 || clearlagCountdown == 5 || clearlagCountdown == 3 || clearlagCountdown == 2 || clearlagCountdown == 1) {
                        String timeStr = clearlagCountdown >= 60 ? (clearlagCountdown % 60 == 0 ? (clearlagCountdown / 60) + "m" : (clearlagCountdown / 60) + "m " + (clearlagCountdown % 60) + "s") : clearlagCountdown + "s";
                        String msg = getMsg("CLEAR-LAG.COUNTDOWN").replace("{seconds}", String.valueOf(clearlagCountdown)).replace("{time}", timeStr);
                        broadcastClearLag(msg);
                    } else if (clearlagCountdown <= 0) {
                        clearlagCountdown = clInterval;
                        int cleared = 0;
                        List<String> excluded = getConfig().getStringList("CLEAR-LAG.EXCLUDED-WORLDS");
                        for (World w : Bukkit.getWorlds()) {
                            if (excluded.contains(w.getName())) continue;
                            for (Entity e : w.getEntities()) {
                                if (e instanceof Item && getConfig().getBoolean("CLEAR-LAG.DROPPED-ITEMS", true)) {
                                    e.remove();
                                    cleared++;
                                } else if (e instanceof Monster && getConfig().getBoolean("CLEAR-LAG.MONSTERS", true)) {
                                    e.remove();
                                    cleared++;
                                } else if (e instanceof Animals && getConfig().getBoolean("CLEAR-LAG.ANIMALS", true)) {
                                    e.remove();
                                    cleared++;
                                }
                            }
                        }
                        String msg = getMsg("CLEAR-LAG.SUCCESS").replace("{total}", String.valueOf(cleared));
                        broadcastClearLag(msg);
                    }
                }
            }.runTaskTimer(this, 20L, 20L);
        }

        // Shards Everywhere Task
        new BukkitRunnable() {
            private int minutesPassed = 0;
            @Override
            public void run() {
                if (!getConfig().getBoolean("SHARDS.EVERYWHERE.ENABLED", true)) return;
                minutesPassed++;
                int everyMins = Math.max(1, getConfig().getInt("SHARDS.EVERYWHERE.EVERY", 3));
                if (minutesPassed >= everyMins) {
                    minutesPassed = 0;
                    long amount = getConfig().getLong("SHARDS.EVERYWHERE.AMOUNT", 1);
                    List<String> excludedWorlds = getConfig().getStringList("SHARDS.EVERYWHERE.EXCLUDED-WORLDS");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (excludedWorlds != null && excludedWorlds.contains(p.getWorld().getName())) continue;
                        PlayerProfile prof = getProfile(p);
                        long finalAmt = amount;
                        if (shardBoosters.containsKey(p.getUniqueId()) && shardBoosters.get(p.getUniqueId()) > System.currentTimeMillis()) {
                            finalAmt *= getConfig().getInt("SHARDS.BOOSTER-MULTIPLIER", 4);
                        }
                        prof.shards += finalAmt;
                        String recMsg = color(getConfig().getString("SHARDS.RECEIVED", "&#A303F9+%amount% Shard").replace("%amount%", String.valueOf(finalAmt)));
                        p.sendMessage(recMsg);
                        saveProfile(prof);
                    }
                }
            }
        }.runTaskTimer(this, 1200L, 1200L);
    }

    private void checkKeyAllTask() {
        if (!getConfig().getBoolean("KEY-ALL.ENABLED", true)) return;
        keyallCountdown--;
        if (keyallCountdown <= 0) {
            triggerKeyAll();
            keyallCountdown = Math.max(1, getConfig().getInt("KEY-ALL.EVERY", getConfig().getInt("KEY-ALL.TIME", 60))) * 60;
        }
    }

    public void triggerKeyAll() {
        String keyName = getConfig().getString("KEY-ALL.KEY-NAME", "Ametista");
        List<String> bcast = getConfig().getStringList("KEY-ALL.MESSAGE");
        if (bcast == null || bcast.isEmpty()) {
            bcast = getConfig().getStringList("KEY-ALL.BROADCAST-MESSAGE");
        }
        if (bcast == null || bcast.isEmpty()) {
            bcast = getConfig().getStringList("KEY-ALL.BROADCAST");
        }
        if (bcast != null && !bcast.isEmpty()) {
            for (String l : bcast) {
                if (!l.trim().isEmpty()) {
                    Bukkit.broadcastMessage(color(l.replace("{key_name}", keyName)));
                }
            }
        } else if (!getConfig().contains("KEY-ALL.MESSAGE") && !getConfig().contains("KEY-ALL.BROADCAST-MESSAGE") && !getConfig().contains("KEY-ALL.BROADCAST")) {
            Bukkit.broadcastMessage(color("&a&lKEY-ALL ATIVADO! &fTodos receberam a chave: &e" + keyName));
        }
        List<String> customCmds = getConfig().getStringList("KEY-ALL.COMMANDS");
        boolean hasValidCommand = false;
        if (customCmds != null) {
            for (String c : customCmds) {
                if (c != null && !c.trim().isEmpty()) {
                    hasValidCommand = true;
                    break;
                }
            }
        }
        String singleCmd = getConfig().getString("KEY-ALL.COMMAND", "");
        if (!hasValidCommand && !singleCmd.trim().isEmpty()) {
            customCmds = Collections.singletonList(singleCmd);
            hasValidCommand = true;
        }

        if (hasValidCommand && customCmds != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                for (String c : customCmds) {
                    if (c != null && !c.trim().isEmpty()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.replace("{player}", p.getName()).replace("{key_name}", keyName));
                    }
                }
            }
        }
    }

    private void checkRtpZoneTask() {
        if (!getConfig().getBoolean("RTP-ZONE.ENABLED", true)) return;
        List<Player> inRtpZone = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String cub = getContainingCuboidName(p.getLocation());
            if (cub != null && cub.toLowerCase().startsWith("rtp")) {
                inRtpZone.add(p);
            }
        }
        if (rtpZoneCountdown < 0) {
            rtpZoneCountdown = getConfig().getInt("RTP-ZONE.EVERY", 30);
        }
        rtpZoneCountdown--;
        if (rtpZoneCountdown > 0) {
            if (!inRtpZone.isEmpty()) {
                String title = getConfig().getString("RTP-ZONE.TITLE", "&c&lʀᴛᴘ ᴢᴏɴᴇ");
                String sub = getConfig().getString("RTP-ZONE.SUB-TITLE", "&fTeleporting in %countdown%").replace("%countdown%", String.valueOf(rtpZoneCountdown) + "s");
                for (Player p : inRtpZone) {
                    p.sendTitle(color(title), color(sub), 0, 25, 5);
                    try { p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f); } catch (Throwable ignored) {}
                }
            }
        } else {
            if (!inRtpZone.isEmpty()) {
                String wName = getConfig().getString("RTP-ZONE.WORLD.NAME", "world");
                World targetW = Bukkit.getWorld(wName);
                if (targetW == null) targetW = inRtpZone.get(0).getWorld();
                int minR = getConfig().getInt("RTP-ZONE.WORLD.MIN-RADIUS", 500);
                int maxR = getConfig().getInt("RTP-ZONE.WORLD.MAX-RADIUS", 2000);
                Location sharedLoc = findSafeRTPLocation(targetW, "overworld", minR, maxR, 25);
                if (sharedLoc != null) {
                    for (Player p : inRtpZone) {
                        try { p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f); } catch (Throwable ignored) {}
                        p.teleport(sharedLoc);
                    }
                } else {
                    for (Player p : inRtpZone) {
                        p.sendMessage(color("&cNão foi possível encontrar um local seguro para o RTP no momento."));
                    }
                }
            }
            rtpZoneCountdown = getConfig().getInt("RTP-ZONE.EVERY", 30);
        }
    }

    private void checkLunarClientTasks() {
        boolean richEnabled = getConfig().getBoolean("LUNAR-CLIENT.RICH-PRESENCE.ENABLED", true);
        boolean teamViewEnabled = getConfig().getBoolean("LUNAR-CLIENT.TEAM-VIEW.ENABLED", true);
        if (!richEnabled && !teamViewEnabled) return;

        lunarPresenceCountdown++;
        int richIntervalSecs = Math.max(1, getConfig().getInt("LUNAR-CLIENT.RICH-PRESENCE.UPDATE", 1)) * 60;

        if (richEnabled && lunarPresenceCountdown >= richIntervalSecs) {
            lunarPresenceCountdown = 0;
            try {
                if (Bukkit.getPluginManager().getPlugin("Apollo") != null) {
                    Class<?> apolloClass = Class.forName("com.lunarclient.apollo.Apollo");
                }
            } catch (Throwable ignored) {}
        }
    }

    private int sbTitleIndex = 0;

    private void updateScoreboard(Player p, PlayerProfile prof) {
        AdventureFastBoard board = boards.get(p.getUniqueId());
        if (board == null || board.isDeleted()) {
            board = new AdventureFastBoard(p);
            boards.put(p.getUniqueId(), board);
        }

        if (!prof.scoreboardVisible || !scoreboardConfig.getBoolean("SCOREBOARD.ENABLED", true)) {
            if (!board.isDeleted()) {
                board.delete();
                boards.remove(p.getUniqueId());
            }
            return;
        }

        List<String> titles = scoreboardConfig.getStringList("SCOREBOARD.TITLE");
        if (!titles.isEmpty()) {
            String titleStr = titles.get((sbTitleIndex / 2) % titles.size());
            titleStr = applyDonutInvestPlaceholders(p, titleStr);
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    titleStr = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, titleStr);
                } catch (Throwable ignored) {}
            }
            board.updateTitle(parseMiniMessageOrLegacy(titleStr));
        } else {
            board.updateTitle(parseMiniMessageOrLegacy("&b&lDonutSMP"));
        }

        List<String> lines = scoreboardConfig.getStringList("SCOREBOARD.LINES");
        List<Component> activeLines = new ArrayList<>();

        long boosterEnd = shardBoosters.getOrDefault(p.getUniqueId(), 0L);
        boolean hasBooster = boosterEnd > System.currentTimeMillis();
        if (!hasBooster) {
            shardBoosters.remove(p.getUniqueId());
        }
        String boosterTimeStr = hasBooster ? formatTime((boosterEnd - System.currentTimeMillis()) / 1000L) : "";
        String dateStr = new java.text.SimpleDateFormat("dd/MM HH:mm").format(new java.util.Date());
        String timeStr = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
        String teamDisplay = prof.teamName.isEmpty() ? (scoreboardConfig.contains("SCOREBOARD.TEAM-NONE") ? scoreboardConfig.getString("SCOREBOARD.TEAM-NONE", "") : "") : scoreboardConfig.getString("SCOREBOARD.TEAM", "&fTeam %donutcore_team%").replace("%economy_team%", prof.teamName).replace("%donutcore_team%", prof.teamName);

        for (String line : lines) {
            boolean isBoosterLine = line.contains("{shard_booster}") || line.contains("{booster}") || line.contains("%economy_booster_countdown%") || line.contains("%donutcore_booster_countdown%") || line.contains("{booster_countdown}") || line.contains("%economy_booster%") || line.contains("%donutcore_booster%");
            if (isBoosterLine && !hasBooster) {
                continue;
            }
            if (line.contains("{team}") && prof.teamName.isEmpty() && (!scoreboardConfig.contains("SCOREBOARD.TEAM-NONE") || scoreboardConfig.getString("SCOREBOARD.TEAM-NONE", "").trim().isEmpty())) {
                continue;
            }
            boolean isConditional = isBoosterLine;
            String formatted = line.replace("%date%", dateStr)
                    .replace("%time%", timeStr)
                    .replace("%date_time%", dateStr)
                    .replace("%player%", p.getName())
                    .replace("%username%", p.getName())
                    .replace("%economy_nicestMoney%", formatAbbreviated(prof.money))
                    .replace("%donutcore_nicestMoney%", formatAbbreviated(prof.money))
                    .replace("%economy_shards%", formatAbbreviated(prof.shards))
                    .replace("%donutcore_shards%", formatAbbreviated(prof.shards))
                    .replace("%economy_kills%", String.valueOf(prof.kills))
                    .replace("%donutcore_kills%", String.valueOf(prof.kills))
                    .replace("%economy_deaths%", String.valueOf(prof.deaths))
                    .replace("%donutcore_deaths%", String.valueOf(prof.deaths))
                    .replace("%economy_playtime%", formatTime(prof.playtimeSeconds))
                    .replace("%donutcore_playtime%", formatTime(prof.playtimeSeconds))
                    .replace("%economy_keyall_countdown%", formatTime(keyallCountdown))
                    .replace("%donutcore_keyall_countdown%", formatTime(keyallCountdown))
                    .replace("%donutcore_keyall_cooldown%", formatTime(keyallCountdown))
                    .replace("%economy_ping%", String.valueOf(p.getPing()))
                    .replace("%donutcore_ping%", String.valueOf(p.getPing()))
                    .replace("{team}", teamDisplay)
                    .replace("{shard_booster}", hasBooster ? scoreboardConfig.getString("SCOREBOARD.SHARD-BOOSTER", "&dBooster Ativo").replace("%economy_booster_countdown%", boosterTimeStr).replace("%donutcore_booster_countdown%", boosterTimeStr).replace("{booster_countdown}", boosterTimeStr) : "")
                    .replace("%economy_booster_countdown%", boosterTimeStr)
                    .replace("%donutcore_booster_countdown%", boosterTimeStr)
                    .replace("{booster_countdown}", boosterTimeStr)
                    .replace("%economy_booster%", boosterTimeStr)
                    .replace("%donutcore_booster%", boosterTimeStr)
                    .replace("{booster}", boosterTimeStr);

            formatted = applyDonutInvestPlaceholders(p, formatted);

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    formatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, formatted);
                } catch (Throwable ignored) {}
            }

            if (isConditional && formatted.trim().isEmpty()) {
                continue;
            }
            activeLines.add(parseMiniMessageOrLegacy(formatted));
        }

        try {
            board.updateLines(activeLines.toArray(new Component[0]));
        } catch (Throwable ignored) {}
    }

    public String applyDonutInvestPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (!text.contains("invest") && !text.contains("Invest") && !text.contains("INVEST")
                && !text.contains("income") && !text.contains("Income") && !text.contains("INCOME")) {
            return text;
        }

        org.bukkit.plugin.Plugin pl = Bukkit.getPluginManager().getPlugin("DonutInvest");
        double income = 0.0;
        double invested = 0.0;
        double pending = 0.0;
        double collected = 0.0;
        double maxInvest = 0.0;

        String formattedIncome = null;
        String formattedInvested = null;
        String formattedPending = null;
        String formattedCollected = null;
        String formattedMax = null;

        if (pl != null && pl.isEnabled() && player != null) {
            try {
                Object invManager = pl.getClass().getMethod("getInvestmentManager").invoke(pl);
                Object ecoManager = pl.getClass().getMethod("getEconomyManager").invoke(pl);
                if (invManager != null) {
                    Object user = invManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(invManager, player.getUniqueId());
                    if (user != null) {
                        try {
                            income = (double) invManager.getClass().getMethod("getIncomePerSecond", Player.class, user.getClass()).invoke(invManager, player, user);
                        } catch (Throwable ignored) {}
                        try {
                            invested = (double) user.getClass().getMethod("getInvestedAmount").invoke(user);
                        } catch (Throwable ignored) {}
                        try {
                            pending = (double) invManager.getClass().getMethod("getPendingIncome", Player.class, user.getClass()).invoke(invManager, player, user);
                        } catch (Throwable ignored) {}
                        try {
                            collected = (double) user.getClass().getMethod("getCollectedAmount").invoke(user);
                        } catch (Throwable ignored) {}
                        try {
                            maxInvest = (double) invManager.getClass().getMethod("getMaxInvest", Player.class).invoke(invManager, player);
                        } catch (Throwable ignored) {}
                    }
                }
                if (ecoManager != null) {
                    java.lang.reflect.Method formatMoneyMethod = ecoManager.getClass().getMethod("formatMoney", double.class);
                    formattedIncome = ((String) formatMoneyMethod.invoke(ecoManager, income)) + "/s";
                    formattedInvested = (String) formatMoneyMethod.invoke(ecoManager, invested);
                    formattedPending = (String) formatMoneyMethod.invoke(ecoManager, pending);
                    formattedCollected = (String) formatMoneyMethod.invoke(ecoManager, collected);
                    formattedMax = (String) formatMoneyMethod.invoke(ecoManager, maxInvest);
                }
            } catch (Throwable ignored) {}
        }

        if (formattedIncome == null) formattedIncome = formatAbbreviated(income) + "/s";
        if (formattedInvested == null) formattedInvested = formatAbbreviated(invested);
        if (formattedPending == null) formattedPending = formatAbbreviated(pending);
        if (formattedCollected == null) formattedCollected = formatAbbreviated(collected);
        if (formattedMax == null) formattedMax = formatAbbreviated(maxInvest);

        String rawIncome = String.format(java.util.Locale.US, "%.2f", income);
        String rawInvested = String.format(java.util.Locale.US, "%.2f", invested);

        return text
                .replace("%donutinvest_investment%", formattedIncome)
                .replace("%donutinvest_income%", formattedIncome)
                .replace("%donutinvest_income_raw%", rawIncome)
                .replace("%donutinvest_invested%", formattedInvested)
                .replace("%donutinvest_amount_invested%", formattedInvested)
                .replace("%donutinvest_total_invested%", formattedInvested)
                .replace("%donutinvest_invest_amount%", formattedInvested)
                .replace("%donutinvest_amount%", formattedInvested)
                .replace("%donutinvest_invested_raw%", rawInvested)
                .replace("%donutinvest_pending%", formattedPending)
                .replace("%donutinvest_can_collect%", formattedPending)
                .replace("%donutinvest_collected%", formattedCollected)
                .replace("%donutinvest_total_collected%", formattedCollected)
                .replace("%donutinvest_max%", formattedMax)
                .replace("%donutinvest_max_invest%", formattedMax)
                .replace("%donutinvest_invest%", formattedIncome)
                .replace("%donutinvest_nicestInvestment%", formattedIncome)
                .replace("%donutinvest_nicestInvested%", formattedInvested)
                .replace("%donutcore_nicestInvestment%", formattedIncome)
                .replace("%donutcore_nicestInvested%", formattedInvested)
                .replace("%donutcore_investment%", formattedIncome)
                .replace("%donutcore_income%", formattedIncome)
                .replace("%donutcore_invested%", formattedInvested)
                .replace("%donutcore_invest%", formattedInvested)
                .replace("%investment%", formattedIncome)
                .replace("%invested%", formattedInvested);
    }

    private void updateAllTablists() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            updateTablist(online);
        }
    }

    private void updateTablist(Player p) {
        if (!getConfig().getBoolean("TABLIST.ENABLED", true)) return;
        String header = getConfig().getString("TABLIST.HEADER", "");
        String footer = getConfig().getString("TABLIST.FOOTER", "");

        int online = Bukkit.getOnlinePlayers().size();
        header = header.replace("%online%", String.valueOf(online))
                .replace("%player%", p.getName())
                .replace("%ping%", String.valueOf(p.getPing()));
        footer = footer.replace("%online%", String.valueOf(online))
                .replace("%player%", p.getName())
                .replace("%ping%", String.valueOf(p.getPing()));

        header = applyDonutInvestPlaceholders(p, header);
        footer = applyDonutInvestPlaceholders(p, footer);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                header = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, header);
                footer = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, footer);
            } catch (Throwable ignored) {}
        }

        p.setPlayerListHeaderFooter(color(header), color(footer));

        String prefix = getPlayerPrefix(p);
        String suffix = getPlayerSuffix(p);

        PlayerProfile prof = getProfile(p);
        boolean showTeam = getConfig().getBoolean("TABLIST.SHOW-TEAM-NAME", false);
        String teamFmt = getConfig().getString("TABLIST.TEAM-FORMAT", " &7[%team%]");
        String teamSuffix = (showTeam && !prof.teamName.isEmpty()) ? teamFmt.replace("%team%", prof.teamName) : "";

        String tabFormat = getConfig().getString("TABLIST.FORMAT", "%prefix%%player%%team%%suffix%");
        
        // Force inject %team% if the user has an outdated config
        if (!tabFormat.contains("%team%")) {
            tabFormat = tabFormat.replace("%player%", "%player%%team%");
        }
        
        prefix = prefix.replace("%player%", p.getName()).replace("%username%", p.getName());
        suffix = suffix.replace("%player%", p.getName()).replace("%username%", p.getName());
        
        tabFormat = tabFormat.replace("%player%", p.getName())
                             .replace("%username%", p.getName())
                             .replace("%prefix%", prefix)
                             .replace("%suffix%", suffix)
                             .replace("%team%", teamSuffix);
        tabFormat = applyDonutInvestPlaceholders(p, tabFormat);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                tabFormat = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, tabFormat);
            } catch (Throwable ignored) {}
        }
        p.playerListName(parseMiniMessageOrLegacy(tabFormat));
    }

    public String getPlayerPrefix(Player p) {
        String prefix = "";
        if (getConfig().getBoolean("TABLIST.LUCKPERMS-PRIORITY", true) && Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Object lp = providerClass.getMethod("get").invoke(null);
                Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
                Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, p.getUniqueId());
                if (user != null) {
                    Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                    Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
                    String pfx = (String) metaData.getClass().getMethod("getPrefix").invoke(metaData);
                    if (pfx != null) prefix = pfx;
                }
            } catch (Throwable ignored) {}
        }
        if (prefix.isEmpty() && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                prefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, "%vault_prefix%");
                if (prefix.equals("%vault_prefix%") || prefix.isEmpty()) {
                    prefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, "%luckperms_prefix%");
                    if (prefix.equals("%luckperms_prefix%")) prefix = "";
                }
            } catch (Throwable ignored) {}
        }
        return prefix;
    }



    public String getPlayerSuffix(Player p) {
        String suffix = "";
        if (getConfig().getBoolean("TABLIST.LUCKPERMS-PRIORITY", true) && Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Object lp = providerClass.getMethod("get").invoke(null);
                Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
                Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, p.getUniqueId());
                if (user != null) {
                    Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                    Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
                    String sfx = (String) metaData.getClass().getMethod("getSuffix").invoke(metaData);
                    if (sfx != null) suffix = sfx;
                }
            } catch (Throwable ignored) {}
        }
        if (suffix.isEmpty() && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                suffix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, "%vault_suffix%");
                if (suffix.equals("%vault_suffix%") || suffix.isEmpty()) {
                    suffix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, "%luckperms_suffix%");
                    if (suffix.equals("%luckperms_suffix%")) suffix = "";
                }
            } catch (Throwable ignored) {}
        }
        return suffix;
    }

    public String formatTime(long totalSecs) {
        long d = totalSecs / 86400;
        long h = (totalSecs % 86400) / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        if (d > 0) {
            if (h > 0) return d + "d " + h + "h";
            if (m > 0) return d + "d " + m + "m";
            return d + "d " + s + "s";
        }
        if (h > 0) return String.format("%02dh %02dm", h, m);
        return String.format("%02dm %02ds", m, s);
    }

    private String formatFullTime(long totalSecs) {
        long d = totalSecs / 86400;
        long h = (totalSecs % 86400) / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        return d + "d " + h + "h " + m + "m " + s + "s";
    }

    // ==========================================
    // COMBAT & TELEPORT SYSTEMS
    // ==========================================
    public void tagPlayer(Player p) {
        int cd = getConfig().getInt("COMBAT-MANAGER.COOLDOWN", 16);
        combatTags.put(p.getUniqueId(), System.currentTimeMillis() + (cd * 1000L));
    }

    public boolean isInCombat(Player p) {
        return combatTags.containsKey(p.getUniqueId()) && combatTags.get(p.getUniqueId()) > System.currentTimeMillis();
    }

    public void sendHotbar(Player p, String msg) {
        if (p == null || msg == null) return;
        PlayerProfile prof = getProfile(p);
        if (prof != null && !prof.hotbarMessages) return;
        p.sendActionBar(msg);
    }

    private void checkCombatTag(Player p) {
        if (isInCombat(p)) {
            long left = (combatTags.get(p.getUniqueId()) - System.currentTimeMillis()) / 1000L;
            String bar = color(getConfig().getString("COMBAT-MANAGER.ACTION-BAR", "&fCombat: &b${time}s").replace("${time}", String.valueOf(left)));
            p.sendActionBar(bar);
        } else if (combatTags.remove(p.getUniqueId()) != null) {
            p.sendMessage(getMsg("COMBAT.UNTAGGED"));
        }
    }

    public void teleportInstant(Player p, Location target) {
        if (isInCombat(p)) {
            p.sendMessage(color(getConfig().getString("COMBAT-MANAGER.BLOCK-MESSAGE", "&cYou can't use this command in combat.")));
            try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
            return;
        }
        p.teleport(target);
        p.sendMessage(getMsg("TELEPORT.SUCCESS"));
        sendHotbar(p, color("&aTeleportado com sucesso!"));
        try { p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f); } catch (Exception ignored) { try { p.playSound(p.getLocation(), Sound.valueOf("ENTITY_PLAYER_TELEPORT"), 1.0f, 1.0f); } catch (Exception ignored2) {} }
    }

    public void teleportWithCooldown(Player p, Location target, String type) {
        if (isInCombat(p)) {
            p.sendMessage(color(getConfig().getString("COMBAT-MANAGER.BLOCK-MESSAGE", "&cYou can't use this command in combat.")));
            try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
            return;
        }
        int cd = getConfig().getInt("TELEPORT-COOLDOWN." + type.toUpperCase(), 5);
        if (cd <= 0) {
            p.teleport(target);
            p.sendMessage(getMsg("TELEPORT.SUCCESS"));
            sendHotbar(p, color("&aTeleportado com sucesso!"));
            try { p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f); } catch (Exception ignored) { try { p.playSound(p.getLocation(), Sound.valueOf("ENTITY_PLAYER_TELEPORT"), 1.0f, 1.0f); } catch (Exception ignored2) {} }
            return;
        }
        boolean showMsg = getConfig().getBoolean("SETTINGS.TELEPORT-COUNTDOWN-MESSAGE", getConfig().getBoolean("TELEPORT-COUNTDOWN-MESSAGE", true));
        boolean showAction = getConfig().getBoolean("SETTINGS.TELEPORT-COUNTDOWN-ACTIONBAR", getConfig().getBoolean("TELEPORT-COUNTDOWN-ACTIONBAR", true));
        String cdText = getMsg("TELEPORT.COUNTDOWN").replace("{seconds}", String.valueOf(cd));
        if (showMsg) p.sendMessage(cdText);
        if (showAction) sendHotbar(p, cdText);
        try { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f); } catch (Exception ignored) { try { p.playSound(p.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), 1.0f, 1.0f); } catch (Exception ignored2) {} }
        Location startLoc = p.getLocation().clone();

        BukkitTask task = new BukkitRunnable() {
            int time = cd;
            @Override
            public void run() {
                if (!p.isOnline() || startLoc.distanceSquared(p.getLocation()) > 0.05 || startLoc.getBlockX() != p.getLocation().getBlockX() || startLoc.getBlockY() != p.getLocation().getBlockY() || startLoc.getBlockZ() != p.getLocation().getBlockZ()) {
                    p.sendMessage(getMsg("TELEPORT.CANCELED"));
                    if (showAction) sendHotbar(p, getMsg("TELEPORT.CANCELED"));
                    try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                    teleportTasks.remove(p.getUniqueId());
                    cancel();
                    return;
                }
                time--;
                if (time <= 0) {
                    p.teleport(target);
                    p.sendMessage(getMsg("TELEPORT.SUCCESS"));
                    if (showAction) sendHotbar(p, getMsg("TELEPORT.SUCCESS"));
                    try { p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f); } catch (Exception ignored) { try { p.playSound(p.getLocation(), Sound.valueOf("ENTITY_PLAYER_TELEPORT"), 1.0f, 1.0f); } catch (Exception ignored2) {} }
                    teleportTasks.remove(p.getUniqueId());
                    cancel();
                } else {
                    String timeText = getMsg("TELEPORT.COUNTDOWN").replace("{seconds}", String.valueOf(time));
                    if (showMsg) p.sendMessage(timeText);
                    if (showAction) sendHotbar(p, timeText);
                    try { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f); } catch (Exception ignored) { try { p.playSound(p.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), 1.0f, 1.0f); } catch (Exception ignored2) {} }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
        teleportTasks.put(p.getUniqueId(), startLoc);
    }

    public void teleportToPlayerWithCooldown(Player p, Player targetPlayer, String type) {
        if (isInCombat(p)) {
            p.sendMessage(color(getConfig().getString("COMBAT-MANAGER.BLOCK-MESSAGE", "&cYou can't use this command in combat.")));
            try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
            return;
        }
        int cd = getConfig().getInt("TELEPORT-COOLDOWN." + type.toUpperCase(), 5);
        if (cd <= 0) {
            if (!targetPlayer.isOnline()) {
                p.sendMessage(getMsg("TPA.PLAYER-OFFLINE"));
                return;
            }
            p.teleport(targetPlayer.getLocation());
            p.sendMessage(getMsg("TELEPORT.SUCCESS"));
            sendHotbar(p, color("&aTeleportado com sucesso!"));
            try { p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f); } catch (Exception ignored) { try { p.playSound(p.getLocation(), Sound.valueOf("ENTITY_PLAYER_TELEPORT"), 1.0f, 1.0f); } catch (Exception ignored2) {} }
            return;
        }
        boolean showMsg = getConfig().getBoolean("SETTINGS.TELEPORT-COUNTDOWN-MESSAGE", getConfig().getBoolean("TELEPORT-COUNTDOWN-MESSAGE", true));
        boolean showAction = getConfig().getBoolean("SETTINGS.TELEPORT-COUNTDOWN-ACTIONBAR", getConfig().getBoolean("TELEPORT-COUNTDOWN-ACTIONBAR", true));
        String cdText = getMsg("TELEPORT.COUNTDOWN").replace("{seconds}", String.valueOf(cd));
        if (showMsg) p.sendMessage(cdText);
        if (showAction) sendHotbar(p, cdText);
        try { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f); } catch (Exception ignored) { try { p.playSound(p.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), 1.0f, 1.0f); } catch (Exception ignored2) {} }
        Location startLoc = p.getLocation().clone();

        BukkitTask task = new BukkitRunnable() {
            int time = cd;
            @Override
            public void run() {
                if (!p.isOnline() || !targetPlayer.isOnline() || startLoc.distanceSquared(p.getLocation()) > 0.05 || startLoc.getBlockX() != p.getLocation().getBlockX() || startLoc.getBlockY() != p.getLocation().getBlockY() || startLoc.getBlockZ() != p.getLocation().getBlockZ()) {
                    if (p.isOnline()) {
                        if (!targetPlayer.isOnline()) {
                            p.sendMessage(getMsg("TPA.PLAYER-OFFLINE"));
                        } else {
                            p.sendMessage(getMsg("TELEPORT.CANCELED"));
                            if (showAction) sendHotbar(p, getMsg("TELEPORT.CANCELED"));
                        }
                        try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                    }
                    teleportTasks.remove(p.getUniqueId());
                    cancel();
                    return;
                }
                time--;
                if (time <= 0) {
                    if (!targetPlayer.isOnline()) {
                        p.sendMessage(getMsg("TPA.PLAYER-OFFLINE"));
                    } else {
                        p.teleport(targetPlayer.getLocation());
                        p.sendMessage(getMsg("TELEPORT.SUCCESS"));
                        if (showAction) sendHotbar(p, getMsg("TELEPORT.SUCCESS"));
                        try { p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f); } catch (Exception ignored) { try { p.playSound(p.getLocation(), Sound.valueOf("ENTITY_PLAYER_TELEPORT"), 1.0f, 1.0f); } catch (Exception ignored2) {} }
                    }
                    teleportTasks.remove(p.getUniqueId());
                    cancel();
                } else {
                    String timeText = getMsg("TELEPORT.COUNTDOWN").replace("{seconds}", String.valueOf(time));
                    if (showMsg) p.sendMessage(timeText);
                    if (showAction) sendHotbar(p, timeText);
                    try { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f); } catch (Exception ignored) { try { p.playSound(p.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), 1.0f, 1.0f); } catch (Exception ignored2) {} }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
        teleportTasks.put(p.getUniqueId(), startLoc);
    }

    // ==========================================
    // EVENT LISTENERS
    // ==========================================
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerProfile prof = getProfile(p);
        prof.username = p.getName();
        if (prof.nightVisionEnabled) {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        }
        try {
            AdventureFastBoard board = new AdventureFastBoard(p);
            boards.put(p.getUniqueId(), board);
        } catch (Throwable t) {
            getLogger().warning("Failed to initialize scoreboard for " + p.getName() + ": " + t.getMessage());
        }
        if (prof.chainmailOnRespawn && !p.hasPlayedBefore()) {
            giveChainmailKit(p);
        }
        Location spawnLoc = getServerSpawnLocation();
        if (spawnLoc != null && (!p.hasPlayedBefore() || getConfig().getBoolean("SETTINGS.TELEPORT-SPAWN-ON-JOIN", true))) {
            p.teleport(spawnLoc);
        }
        updateTablist(p);
        for (Player online : Bukkit.getOnlinePlayers()) {
            updateTablist(online);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        AdventureFastBoard board = boards.remove(e.getPlayer().getUniqueId());
        if (board != null) {
            try { board.delete(); } catch (Throwable ignored) {}
        }
        if (tempSigns.containsKey(e.getPlayer().getUniqueId())) {
            Location loc = tempSigns.remove(e.getPlayer().getUniqueId());
            BlockData old = oldBlockData.remove(e.getPlayer().getUniqueId());
            if (loc != null && old != null && loc.getWorld() != null) loc.getBlock().setBlockData(old, false);
        }
        if (isInCombat(e.getPlayer())) {
            e.getPlayer().setHealth(0.0);
            Bukkit.broadcastMessage(color("&c☠ " + e.getPlayer().getName() + " deslogou em combate e foi morto!"));
        }
        combatTags.remove(e.getPlayer().getUniqueId());
        lastMessaged.remove(e.getPlayer().getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    updateTablist(online);
                }
            }
        }.runTaskLater(this, 5L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        boolean respawnOnBed = getConfig().getBoolean("SETTINGS.RESPAWN-ON-BED", false);
        boolean useBed = respawnOnBed && (e.isBedSpawn() || e.isAnchorSpawn());
        
        if (!useBed) {
            Location spawnLoc = getServerSpawnLocation();
            if (spawnLoc != null) {
                try { e.setRespawnLocation(spawnLoc); } catch (Throwable ignored) {}
            }
        }
        
        Location finalSpawn = useBed ? null : getServerSpawnLocation();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (finalSpawn != null && getConfig().getBoolean("SETTINGS.TELEPORT-SPAWN-ON-RESPAWN", true)) {
                    try { e.getPlayer().teleport(finalSpawn); } catch (Throwable ignored) {}
                }
                PlayerProfile prof = getProfile(e.getPlayer());
                if (getConfig().getBoolean("SETTINGS.CHAINMAIL-ON-RESPAWN", true) && prof.chainmailOnRespawn) {
                    giveChainmailKit(e.getPlayer());
                }
                if (prof.nightVisionEnabled) {
                    e.getPlayer().addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                }
            }
        }.runTaskLater(this, 5L);
    }

    private void giveChainmailKit(Player p) {
        p.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
        p.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
    }

    private Location getServerSpawnLocation() {
        Location loc = parseLocation(getConfig().getString("LOCATIONS.SERVER-SPAWN", ""));
        if (loc == null && !Bukkit.getWorlds().isEmpty()) {
            loc = Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        return loc;
    }

    private void setServerSpawn(Player p) {
        Location loc = p.getLocation();
        getConfig().set("LOCATIONS.SERVER-SPAWN", serializeLocation(loc));
        saveConfig();

        if (loc.getWorld() != null) {
            loc.getWorld().setSpawnLocation(loc);
        }

        try { Bukkit.dispatchCommand(p, "setspawnworld"); } catch (Throwable ignored) {}
        if (loc.getWorld() != null) {
            try { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setspawnworld " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()); } catch (Throwable ignored) {}
        }

        p.sendMessage(color("&7Spawn do mundo definido"));
    }

    private void setSpawnCommandPoint(Player p, String spawnName) {
        Location loc = p.getLocation();
        getConfig().set("LOCATIONS.SPAWN-SPAWNS." + spawnName, serializeLocation(loc));
        if (spawnName.equals("1") || spawnName.equalsIgnoreCase("spawn1")) {
            getConfig().set("LOCATIONS.SPAWN-SPAWNS.1", serializeLocation(loc));
            getConfig().set("LOCATIONS.SPAWN-SPAWNS.spawn1", serializeLocation(loc));
            getConfig().set("LOCATIONS.SPAWN-LOCATION", serializeLocation(loc));
        } else if (spawnName.equals("2") || spawnName.equalsIgnoreCase("spawn2")) {
            getConfig().set("LOCATIONS.SPAWN-SPAWNS.2", serializeLocation(loc));
            getConfig().set("LOCATIONS.SPAWN-SPAWNS.spawn2", serializeLocation(loc));
        }
        saveConfig();
        p.sendMessage(color("&a✔ Ponto de Spawn &#00A4FC" + spawnName + " &adefinido com sucesso no seu local!"));
        String containing = getContainingCuboidName(loc);
        if (containing != null) {
            p.sendMessage(color("&a✔ Ponto de spawn está dentro da região Cuboid '&f" + containing + "&a'!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        lastActivityMillis.put(p.getUniqueId(), System.currentTimeMillis());
        if (searchPrompt.containsKey(p.getUniqueId())) {
            e.setCancelled(true);
            String searchType = searchPrompt.remove(p.getUniqueId());
            String query = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
            if (query.equalsIgnoreCase("cancelar") || query.equalsIgnoreCase("cancel")) {
                p.sendMessage(color("&cPesquisa cancelada."));
                return;
            }
            Bukkit.getScheduler().runTask(this, () -> {
                if ("TEAM".equals(searchType)) {
                    openGUI(p, "TEAM-MENUS.TEAM");
                    p.sendMessage(color("&a&lPesquisa: &fExibindo resultados para &e" + query));
                } else {
                    openGUI(p, "LEADERBOARDS-MENU");
                }
            });
            return;
        }
        PlayerProfile prof = getProfile(p);
        if (!prof.teamName.isEmpty()) {
            Team team = teams.get(prof.teamName.toLowerCase());
            if (team != null && team.teamChat) {
                e.setCancelled(true);
                if (!team.leader.equals(p.getUniqueId()) && !prof.permHelper && !prof.permTeamChat) {
                    p.sendMessage(color("&cVocê não tem permissão para falar no chat do team"));
                    return;
                }
                String plainMsg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.message());
                String tcMsg = color("&#6BF18DTeam &7→ &#DDDDDD" + p.getName() + ": &#FFFFFF" + plainMsg);
                for (UUID m : team.members) {
                    Player mp = Bukkit.getPlayer(m);
                    if (mp != null) mp.sendMessage(tcMsg);
                }
                return;
            }
        }

        e.viewers().removeIf(viewer -> {
            if (viewer instanceof Player) {
                PlayerProfile vProf = getProfile((Player) viewer);
                if (vProf != null && vProf.ignoredPlayers.contains(p.getUniqueId())) {
                    return true;
                }
            }
            return false;
        });

        if (!getConfig().getBoolean("SETTINGS.CHAT-FORMAT", true)) return;

        List<String> hoverLoreList = getConfig().getStringList("CHAT-FORMAT.HOVER-LORE");
        if (hoverLoreList.isEmpty()) {
            hoverLoreList = Arrays.asList(
                "%prefix%%player%",
                "&7&m----------",
                "&#00FC00&l$ &fᴍᴏɴᴇʏ &#00FC00%money%",
                "&#FC0000⚔ &fᴋɪʟʟѕ &#FC0000%kills%",
                "&#FCE300⌚ &fᴘʟᴀʏᴛɪᴍᴇ &#FCE300%playtime%",
                "&#F97603☠ &fᴅᴇᴀᴛʜѕ &#F97603%deaths%",
                "&#DF9FFF★ &fѕʜᴀʀᴅѕ &#A303F9%shards%",
                "&7&m----------"
            );
        }
        String prefixStr = getPlayerPrefix(p);
        String suffixStr = getPlayerSuffix(p);
        final String teamStr = prof.teamName.isEmpty() ? "" : "&8[" + prof.teamName + "&8] &r";
        
        final String finalPrefixStr = prefixStr.replace("%player%", p.getName()).replace("%username%", p.getName());
        final String finalSuffixStr = suffixStr.replace("%player%", p.getName()).replace("%username%", p.getName());
        
        Component hoverComponent = Component.empty();
        for (int i = 0; i < hoverLoreList.size(); i++) {
            String hLine = hoverLoreList.get(i)
                    .replace("%player%", p.getName())
                    .replace("%username%", p.getName())
                    .replace("%prefix%", finalPrefixStr)
                    .replace("%suffix%", finalSuffixStr)
                    .replace("%team%", teamStr)
                    .replace("%money%", formatAbbreviated(prof.money))
                    .replace("%economy_nicestMoney%", formatAbbreviated(prof.money))
                    .replace("%donutcore_nicestMoney%", formatAbbreviated(prof.money))
                    .replace("%economy_money%", formatAbbreviated(prof.money))
                    .replace("%donutcore_money%", formatAbbreviated(prof.money))
                    .replace("%kills%", String.valueOf(prof.kills))
                    .replace("%economy_kills%", String.valueOf(prof.kills))
                    .replace("%donutcore_kills%", String.valueOf(prof.kills))
                    .replace("%playtime%", formatTime(prof.playtimeSeconds))
                    .replace("%economy_playtime%", formatTime(prof.playtimeSeconds))
                    .replace("%donutcore_playtime%", formatTime(prof.playtimeSeconds))
                    .replace("%deaths%", String.valueOf(prof.deaths))
                    .replace("%economy_deaths%", String.valueOf(prof.deaths))
                    .replace("%donutcore_deaths%", String.valueOf(prof.deaths))
                    .replace("%shards%", formatAbbreviated(prof.shards))
                    .replace("%economy_shards%", formatAbbreviated(prof.shards))
                    .replace("%donutcore_shards%", formatAbbreviated(prof.shards));
            hLine = applyDonutInvestPlaceholders(p, hLine);
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    hLine = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, hLine);
                } catch (Throwable ignored) {}
            }
            if (i > 0) {
                hoverComponent = hoverComponent.append(Component.newline());
            }
            hoverComponent = hoverComponent.append(parseMiniMessageOrLegacy(hLine));
        }

        final Component finalHover = hoverComponent;

        e.renderer((source, sourceDisplayName, message, viewer) -> {
            String format = getConfig().getString("CHAT-FORMAT.CHAT", "&f%prefix%%player%&7: &#FFFFFF%message%");
            String plainMsg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(message);

            for (Player op : Bukkit.getOnlinePlayers()) {
                String opName = op.getName();
                if (!source.getUniqueId().equals(op.getUniqueId())) {
                    if (plainMsg.matches("(?i).*\\b" + java.util.regex.Pattern.quote(opName) + "\\b.*")) {
                        plainMsg = plainMsg.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(opName) + "\\b", "<#FFF4B7>" + opName + "</#FFF4B7>");
                        if (viewer instanceof Player && ((Player) viewer).getUniqueId().equals(op.getUniqueId())) {
                            Player v = (Player) viewer;
                            v.playSound(v.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            v.sendActionBar(parseMiniMessageOrLegacy("&aVocê foi mencionado no chat!"));
                        }
                    }
                }
            }

            String formatted = format.replace("%player%", source.getName())
                    .replace("%username%", source.getName())
                    .replace("%prefix%", finalPrefixStr)
                    .replace("%suffix%", finalSuffixStr)
                    .replace("%team%", teamStr)
                    .replace("%message%", plainMsg);
            formatted = applyDonutInvestPlaceholders(source, formatted);
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    formatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(source, formatted);
                } catch (Throwable ignored) {}
            }
            Component result = parseMiniMessageOrLegacy(formatted);
            return result.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(finalHover))
                         .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/tell " + source.getName() + " "));
        });
    }

    public void openSignGUI(Player p, String type, String line1, String line2) {
        try {
            Location loc = p.getLocation().clone().add(0, -4, 0);
            if (loc.getY() < p.getWorld().getMinHeight() + 1) loc.setY(p.getWorld().getMinHeight() + 1);
            Block block = loc.getBlock();
            
            oldBlockData.put(p.getUniqueId(), block.getBlockData().clone());
            tempSigns.put(p.getUniqueId(), loc);
            searchPrompt.put(p.getUniqueId(), type);

            block.setType(Material.OAK_SIGN, false);
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                sign.setLine(0, "");
                sign.setLine(1, "^ ^ ^ ^ ^ ^");
                sign.setLine(2, line1);
                sign.setLine(3, line2);
                sign.update(true, false);
                
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    try {
                        p.openSign(sign);
                    } catch (Throwable ex) {
                        p.sendMessage(color("&8[&bPesquisa&8] &eDigite sua busca no chat (ou 'cancelar'):"));
                    }
                }, 2L);
            }
        } catch (Throwable ex) {
            searchPrompt.put(p.getUniqueId(), type);
            p.sendMessage(color("&8[&bPesquisa&8] &eDigite sua busca no chat (ou 'cancelar'):"));
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();
        if (tempSigns.containsKey(p.getUniqueId())) {
            Location loc = tempSigns.remove(p.getUniqueId());
            BlockData old = oldBlockData.remove(p.getUniqueId());
            if (loc != null && old != null && loc.getWorld() != null) {
                loc.getBlock().setBlockData(old, false);
            }
            String query = e.getLine(0);
            String searchType = searchPrompt.remove(p.getUniqueId());
            
            if (query == null || query.trim().isEmpty() || query.equalsIgnoreCase("cancelar") || query.equalsIgnoreCase("cancel")) {
                p.sendMessage(color("&cPesquisa cancelada."));
                Bukkit.getScheduler().runTask(this, () -> {
                    if ("TEAM".equals(searchType)) openGUI(p, "TEAM-MENUS.TEAM");
                    else if ("SHOP".equals(searchType)) openGUI(p, "CATEGORIES");
                    else if ("BOUNTY".equals(searchType)) openGUI(p, "BOUNTIES-MENU");
                    else openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU");
                });
                return;
            }
            final String finalQuery = query.trim();
            Bukkit.getScheduler().runTask(this, () -> {
                if ("TEAM".equals(searchType)) {
                    activeMenuSearch.put(p.getUniqueId(), finalQuery.toLowerCase());
                    openGUI(p, "TEAM-MENUS.TEAM");
                    p.sendMessage(color("&a&lPesquisa Team: &fExibindo resultados para &e" + finalQuery));
                } else if ("SHOP".equals(searchType)) {
                    openShopSearchResults(p, finalQuery);
                    p.sendMessage(color("&a&lPesquisa Shop: &fExibindo itens para &e" + finalQuery));
                } else if ("BOUNTY".equals(searchType)) {
                    activeMenuSearch.put(p.getUniqueId(), finalQuery.toLowerCase());
                    openGUI(p, "BOUNTIES-MENU");
                    p.sendMessage(color("&a&lPesquisa Bounty: &fExibindo resultados para &e" + finalQuery));
                } else {
                    activeMenuSearch.put(p.getUniqueId(), finalQuery.toLowerCase());
                    openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU");
                    p.sendMessage(color("&a&lPesquisa Leaderboards: &fExibindo resultados para &e" + finalQuery));
                }
            });
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() != null && e.getDamager().getType().name().contains("ENDER_CRYSTAL")) {
            if (!getConfig().getBoolean("END-CRYSTAL.ENABLED", false)) {
                e.setCancelled(true);
                return;
            } else {
                double dmgScale = getConfig().getDouble("END-CRYSTAL.DAMAGE", 2.0);
                e.setDamage(e.getDamage() * dmgScale);
            }
        }

        if (e.getEntity() != null && e.getEntity().getType().name().contains("ENDER_CRYSTAL")) {
            Player pDamager = null;
            if (e.getDamager() instanceof Player) pDamager = (Player) e.getDamager();
            else if (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof Player) pDamager = (Player) ((Projectile) e.getDamager()).getShooter();
            if (pDamager != null) {
                crystalDamagers.put(e.getEntity().getUniqueId(), pDamager.getUniqueId());
            }
        }

        Player damager = null;
        if (e.getDamager() instanceof Player) {
            damager = (Player) e.getDamager();
        } else if (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof Player) {
            damager = (Player) ((Projectile) e.getDamager()).getShooter();
            String pType = e.getDamager().getType().name();
            if (pType.contains("ENDER_PEARL") && !getConfig().getBoolean("COMBAT-MANAGER.ENDER-PEARL", true)) damager = null;
            if ((pType.contains("WIND_CHARGE") || pType.contains("BREEZE_WIND_CHARGE")) && !getConfig().getBoolean("COMBAT-MANAGER.WIND-CHARGE", true)) damager = null;
            if (pType.contains("TRIDENT") && !getConfig().getBoolean("COMBAT-MANAGER.SPEAR", true)) damager = null;
        } else if (e.getDamager() != null && e.getDamager().getType().name().contains("ENDER_CRYSTAL") && getConfig().getBoolean("COMBAT-MANAGER.ENDER-CRYSTAL", true)) {
            UUID damagerId = crystalDamagers.get(e.getDamager().getUniqueId());
            if (damagerId != null) {
                Player pDamager = Bukkit.getPlayer(damagerId);
                if (pDamager != null && pDamager.isOnline()) damager = pDamager;
            }
        }

        if (e.getEntity() instanceof Player && damager != null && !damager.equals(e.getEntity())) {
            Player victim = (Player) e.getEntity();
            PlayerProfile vProf = getProfile(victim);
            PlayerProfile dProf = getProfile(damager);

            if (!vProf.teamName.isEmpty() && vProf.teamName.equalsIgnoreCase(dProf.teamName)) {
                Team team = teams.get(vProf.teamName.toLowerCase());
                if (team != null) {
                    boolean canHit = team.leader.equals(damager.getUniqueId()) ? team.pvp : dProf.permTogglePvp;
                    if (!canHit) {
                        e.setCancelled(true);
                        damager.sendMessage(color("&cO PvP da equipe está desativado para você!"));
                        return;
                    }
                }
                double allyRange = getConfig().getDouble("TEAM.ALLY-DAMAGE-RANGE", 5.0);
                if (victim.getWorld().equals(damager.getWorld()) && victim.getLocation().distance(damager.getLocation()) <= allyRange && (e.getCause().name().contains("SWEEP") || e.getCause().name().contains("AREA") || e.getCause().name().contains("SPLASH"))) {
                    e.setCancelled(true);
                    return;
                }
            }
            tagPlayer(victim);
            tagPlayer(damager);
        }
    }

    @EventHandler
    public void onGeneralEntityDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            Player victim = (Player) e.getEntity();
            boolean isAnchor = false;
            for (Map.Entry<Location, UUID> entry : anchorExploders.entrySet()) {
                if (entry.getKey().getWorld().equals(victim.getWorld()) && entry.getKey().distanceSquared(victim.getLocation()) <= 100) {
                    isAnchor = true;
                    if (getConfig().getBoolean("COMBAT-MANAGER.RESPAWN-ANCHOR", true)) {
                        Player pDamager = Bukkit.getPlayer(entry.getValue());
                        if (pDamager != null && pDamager.isOnline() && !pDamager.equals(victim)) {
                            tagPlayer(victim);
                            tagPlayer(pDamager);
                        }
                    }
                    break;
                }
            }
            if (isAnchor) {
                if (!getConfig().getBoolean("RESPAWN-ANCHOR.ENABLED", false)) {
                    e.setCancelled(true);
                } else {
                    double dmgScale = getConfig().getDouble("RESPAWN-ANCHOR.DAMAGE", 2.0);
                    e.setDamage(e.getDamage() * dmgScale);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        PlayerProfile vProf = getProfile(victim);
        vProf.deaths++;
        vProf.killStreak = 0;

        if (killer != null && killer != victim) {
            PlayerProfile kProf = getProfile(killer);
            kProf.kills++;
            kProf.killStreak++;
            if (kProf.killStreak > kProf.highestKillStreak) {
                kProf.highestKillStreak = kProf.killStreak;
            }
            long shardsGain = getConfig().getLong("SETTINGS.SHARDS-PER-KILL", 1);
            kProf.shards += shardsGain;
            killer.sendMessage(color(getConfig().getString("SETTINGS.SHARDS-KILL-MESSAGE", "&d+{shards} Shard").replace("{shards}", String.valueOf(shardsGain))));
            
            // Check bounty
            if (bounties.containsKey(victim.getUniqueId())) {
                double reward = bounties.remove(victim.getUniqueId());
                kProf.money += reward;
                kProf.moneyMade += reward;
                String claimMsg = getMsg("BOUNTY.CLAIMED")
                        .replace("${amount}", "$" + formatAbbreviated(reward))
                        .replace("{amount}", formatAbbreviated(reward))
                        .replace("{killer}", killer.getName())
                        .replace("{victim}", victim.getName());
                broadcastBounty(claimMsg);
            }

            // Steal 5% of victim's money and add to killer's bounty
            if (vProf.money > 0) {
                double stolen = vProf.money * 0.05;
                if (stolen >= 0.01) {
                    vProf.money -= stolen;
                    double currentKillerBounty = bounties.getOrDefault(killer.getUniqueId(), 0.0);
                    bounties.put(killer.getUniqueId(), currentKillerBounty + stolen);
                    killer.sendMessage(color("&7Você matou o &#FFF000" + victim.getName() + " &7e roubou &#6BF18D5% ($" + formatAbbreviated(stolen) + ") &7do money dele. E aumentou seu bounty!"));
                    victim.sendMessage(color("&7O &#FFF000" + killer.getName() + " &7roubou &#6BF18D5% ($" + formatAbbreviated(stolen) + ") &7do seu money."));
                }
            }
            saveProfile(kProf);
            updateScoreboard(killer, kProf);
            e.setDeathMessage(color("&c☠ " + victim.getName() + " foi morto por " + killer.getName()));
        } else {
            e.setDeathMessage(color("&c☠ " + victim.getName() + " morreu."));
        }
        saveProfile(vProf);
        updateScoreboard(victim, vProf);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        PlayerProfile prof = getProfile(e.getPlayer());
        prof.blocksPlaced++;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        if (e.getEntity().getKiller() != null && !(e.getEntity() instanceof Player)) {
            PlayerProfile prof = getProfile(e.getEntity().getKiller());
            prof.mobsKilled++;
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (isInCombat(e.getPlayer())) {
            String cmd = e.getMessage().split(" ")[0].toLowerCase();
            List<String> blocked = getConfig().getStringList("COMBAT-MANAGER.BLOCK-COMMANDS");
            if (blocked.contains(cmd)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(color(getConfig().getString("COMBAT-MANAGER.BLOCK-MESSAGE", "&cYou can't use this command in combat.")));
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockY() != e.getTo().getBlockY() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            lastActivityMillis.put(p.getUniqueId(), System.currentTimeMillis());
        }
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        if (getConfig().getBoolean("SETTINGS.DOUBLE-JUMP", true) && isInSpawnZone(p)) {
            if (p.getLocation().subtract(0, 0.1, 0).getBlock().getType() != Material.AIR) {
                p.setAllowFlight(true);
            }
        } else if (p.getAllowFlight()) {
            p.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        if (getConfig().getBoolean("SETTINGS.DOUBLE-JUMP", true) && isInSpawnZone(p)) {
            e.setCancelled(true);
            p.setAllowFlight(false);
            p.setFlying(false);
            p.setVelocity(p.getLocation().getDirection().multiply(1.2).setY(1.0));
            p.playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.2f);
        }
    }

    // ==========================================
    // AMETHYST TOOLS MECHANICS
    // ==========================================
    public ItemStack createAmethystTool(String type) {
        return createAmethystTool(type, 0, "Permanente");
    }

    @SuppressWarnings("deprecation")
    public ItemStack createAmethystTool(String type, long durationSeconds, String durationText) {
        String path = "AMETHYST-TOOLS." + type.toUpperCase();
        Material mat = Material.NETHERITE_PICKAXE;
        if (type.equalsIgnoreCase("AXE") || type.equalsIgnoreCase("SELLAXE")) mat = Material.NETHERITE_AXE;
        else if (type.equalsIgnoreCase("SHOVEL")) mat = Material.NETHERITE_SHOVEL;
        else if (type.equalsIgnoreCase("BUCKET")) mat = Material.BUCKET;
        else if (type.equalsIgnoreCase("BOOSTER")) mat = Material.POTION;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta instanceof org.bukkit.inventory.meta.PotionMeta) {
                try {
                    ((org.bukkit.inventory.meta.PotionMeta) meta).setBasePotionType(org.bukkit.potion.PotionType.WATER);
                } catch (Throwable ignored) {}
            }
            meta.setDisplayName(color(getConfig().getString(path + ".NAME", "&dAmethyst Tool")));
            List<String> rawLore = getConfig().getStringList(path + ".LORE");
            List<String> lore = new ArrayList<>();
            String timeFormatted = (durationSeconds > 0) ? (getSmoothHexColor(1.0) + formatAmethystTimeFormatted(durationSeconds)) : "Permanente";
            for (String l : rawLore) {
                String lineReplaced = l.replace("{time}", timeFormatted);
                if (durationSeconds > 0 && l.toLowerCase().contains("self destruct")) {
                    lineReplaced = "&8Self Destruct";
                }
                lore.add(color(lineReplaced));
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, type.toUpperCase());
            if (durationSeconds > 0) {
                long expireMs = System.currentTimeMillis() + (durationSeconds * 1000L);
                meta.getPersistentDataContainer().set(amethystExpireKey, PersistentDataType.LONG, expireMs);
                meta.getPersistentDataContainer().set(amethystDurationKey, PersistentDataType.LONG, durationSeconds);
                meta.getPersistentDataContainer().set(amethystRemainingSecondsKey, PersistentDataType.LONG, durationSeconds);
            }
            meta.setUnbreakable(true);
            List<String> enchList = getConfig().getStringList(path + ".ENCHANTMENTS");
            for (String enchStr : enchList) {
                String[] parts = enchStr.split(":");
                if (parts.length >= 1) {
                    Enchantment ench = resolveEnchantment(parts[0]);
                    int lvl = 1;
                    if (parts.length >= 2) {
                        try { lvl = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                    }
                    if (ench != null) {
                        meta.addEnchant(ench, lvl, true);
                    }
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("deprecation")
    public static Enchantment resolveEnchantment(String name) {
        if (name == null) return null;
        name = name.trim().toLowerCase();
        Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft(name));
        if (e != null) return e;
        return Enchantment.getByName(name.toUpperCase());
    }

    public String formatAmethystTimeFormatted(long remainingSeconds) {
        if (remainingSeconds <= 0) return "0d 0h 0m 0s";
        long days = remainingSeconds / 86400L;
        long hours = (remainingSeconds % 86400L) / 3600L;
        long minutes = (remainingSeconds % 3600L) / 60L;
        long seconds = remainingSeconds % 60L;
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }

    public String getSmoothHexColor(double fractionRemaining) {
        fractionRemaining = Math.max(0.0, Math.min(1.0, fractionRemaining));
        if (fractionRemaining > 0.5) {
            return "&a";
        } else if (fractionRemaining > 0.25) {
            return "&6";
        } else {
            return "&c";
        }
    }

    public String getAmethystTimeColor(double fractionRemaining) {
        return getSmoothHexColor(fractionRemaining);
    }

    public String applyHexGradient(String text, int r1, int g1, int b1, int r2, int g2, int b2) {
        if (text == null || text.isEmpty()) return "";
        char[] chars = text.toCharArray();
        int len = chars.length;
        if (len == 1) {
            return String.format("&#%02X%02X%02X%s", r1, g1, b1, text);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            double factor = (double) i / (double) (len - 1);
            int r = (int) Math.round(r1 + factor * (r2 - r1));
            int g = (int) Math.round(g1 + factor * (g2 - g1));
            int b = (int) Math.round(b1 + factor * (b2 - b1));
            sb.append(String.format("&#%02X%02X%02X", r, g, b)).append(chars[i]);
        }
        return sb.toString();
    }

    public String getAmethystGradientLine(String text, double fractionRemaining) {
        fractionRemaining = Math.max(0.0, Math.min(1.0, fractionRemaining));
        int r1, g1, b1, r2, g2, b2;
        if (fractionRemaining >= 0.5) {
            double t = (fractionRemaining - 0.5) * 2.0;
            r1 = (int) Math.round(255 - t * 255);
            g1 = 255;
            b1 = (int) Math.round(t * 100);

            r2 = (int) Math.round(255 - t * 135);
            g2 = (int) Math.round(140 + t * 115);
            b2 = 0;
        } else {
            double t = fractionRemaining * 2.0;
            r1 = 255;
            g1 = (int) Math.round(60 + t * 155);
            b1 = 0;

            r2 = 255;
            g2 = (int) Math.round(t * 140);
            b2 = (int) Math.round((1.0 - t) * 80);
        }
        return applyHexGradient(text, r1, g1, b1, r2, g2, b2);
    }

    public long parseDurationSeconds(String str) {
        if (str == null || str.isEmpty()) return 0;
        str = str.toLowerCase().trim();
        try {
            if (str.endsWith("s")) {
                return Long.parseLong(str.substring(0, str.length() - 1));
            } else if (str.endsWith("m")) {
                return Long.parseLong(str.substring(0, str.length() - 1)) * 60L;
            } else if (str.endsWith("h")) {
                return Long.parseLong(str.substring(0, str.length() - 1)) * 3600L;
            } else if (str.endsWith("d")) {
                return Long.parseLong(str.substring(0, str.length() - 1)) * 86400L;
            } else {
                return Long.parseLong(str);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isAmethystToolExcluded(Player p) {
        if (p == null || p.getWorld() == null) return false;
        List<String> excluded = getConfig().getStringList("AMETHYST-TOOLS.EXCLUDED-WORLDS");
        for (String w : excluded) {
            if (p.getWorld().getName().equalsIgnoreCase(w)) return true;
        }
        return false;
    }

    private final Map<UUID, Long> heldAmethystAccumulator = new java.util.concurrent.ConcurrentHashMap<>();

    public long getAmethystRemainingSeconds(ItemMeta meta) {
        if (meta == null) return 0L;
        Long remaining = meta.getPersistentDataContainer().get(amethystRemainingSecondsKey, PersistentDataType.LONG);
        if (remaining != null) return Math.max(0L, remaining);
        Long expireMs = meta.getPersistentDataContainer().get(amethystExpireKey, PersistentDataType.LONG);
        if (expireMs != null && expireMs > 0) {
            long rem = Math.max(0L, (expireMs - System.currentTimeMillis()) / 1000L);
            meta.getPersistentDataContainer().set(amethystRemainingSecondsKey, PersistentDataType.LONG, rem);
            meta.getPersistentDataContainer().remove(amethystExpireKey);
            return rem;
        }
        return 0L;
    }

    public boolean isAmethystToolExpired(Player p, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        long remaining = getAmethystRemainingSeconds(item.getItemMeta());
        if (remaining <= 0 && (item.getItemMeta().getPersistentDataContainer().has(amethystRemainingSecondsKey, PersistentDataType.LONG) || item.getItemMeta().getPersistentDataContainer().has(amethystExpireKey, PersistentDataType.LONG))) {
            item.setAmount(0);
            try { p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f); } catch (Exception ignored) {}
            return true;
        }
        return false;
    }

    public void checkAmethystToolExpiration(Player p) {
        if (p == null || !p.isOnline()) return;
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;

        int heldSlot = p.getInventory().getHeldItemSlot();
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            boolean hasRemaining = meta.getPersistentDataContainer().has(amethystRemainingSecondsKey, PersistentDataType.LONG);
            boolean hasExpire = meta.getPersistentDataContainer().has(amethystExpireKey, PersistentDataType.LONG);
            if (!hasRemaining && !hasExpire) continue;

            long remainingSeconds = getAmethystRemainingSeconds(meta);
            if (i == heldSlot) {
                long accum = heldAmethystAccumulator.getOrDefault(p.getUniqueId(), 0L) + 1L;
                if (remainingSeconds - accum <= 0) {
                    p.getInventory().setItem(i, null);
                    try { p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f); } catch (Exception ignored) {}
                    heldAmethystAccumulator.remove(p.getUniqueId());
                    continue;
                }
                heldAmethystAccumulator.put(p.getUniqueId(), accum);
            } else {
                remainingSeconds = remainingSeconds - 1L;
                if (remainingSeconds <= 0) {
                    p.getInventory().setItem(i, null);
                    try { p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f); } catch (Exception ignored) {}
                    continue;
                }
                meta.getPersistentDataContainer().set(amethystRemainingSecondsKey, PersistentDataType.LONG, remainingSeconds);

                Long totalDuration = meta.getPersistentDataContainer().get(amethystDurationKey, PersistentDataType.LONG);
                if (totalDuration == null || totalDuration <= 0) totalDuration = remainingSeconds;
                double fractionRemaining = Math.min(1.0, Math.max(0.0, (double) remainingSeconds / (double) totalDuration));
                String colorCode = getSmoothHexColor(fractionRemaining);

                List<String> lore = meta.getLore();
                if (lore != null) {
                    List<String> newLore = new ArrayList<>();
                    for (String line : lore) {
                        String clean = line.replaceAll("§[0-9a-fk-or]", "").trim();
                        if (clean.equalsIgnoreCase("Self Destruct") || clean.matches(".*\\d+d \\d+h \\d+m \\d+s.*")) {
                            if (clean.equalsIgnoreCase("Self Destruct")) {
                                newLore.add(color("&8Self Destruct"));
                            } else {
                                newLore.add(color(colorCode + formatAmethystTimeFormatted(remainingSeconds)));
                            }
                        } else {
                            newLore.add(line);
                        }
                    }
                    meta.setLore(newLore);
                }
                item.setItemMeta(meta);
            }
        }
    }

    public void updateAmethystVisuals(Player p) {
        if (p == null || !p.isOnline()) return;

        long accum = heldAmethystAccumulator.getOrDefault(p.getUniqueId(), 0L);
        if (accum > 0) {
            ItemStack held = p.getInventory().getItemInMainHand();
            if (held != null && held.hasItemMeta()) {
                ItemMeta hMeta = held.getItemMeta();
                if (hMeta.getPersistentDataContainer().has(amethystRemainingSecondsKey, PersistentDataType.LONG) || hMeta.getPersistentDataContainer().has(amethystExpireKey, PersistentDataType.LONG)) {
                    long rem = getAmethystRemainingSeconds(hMeta) - accum;
                    if (rem <= 0) {
                        p.getInventory().setItemInMainHand(null);
                        try { p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f); } catch (Exception ignored) {}
                        heldAmethystAccumulator.remove(p.getUniqueId());
                        return;
                    }
                    hMeta.getPersistentDataContainer().set(amethystRemainingSecondsKey, PersistentDataType.LONG, rem);
                    held.setItemMeta(hMeta);
                }
            }
            heldAmethystAccumulator.remove(p.getUniqueId());
        }

        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            boolean hasRemaining = meta.getPersistentDataContainer().has(amethystRemainingSecondsKey, PersistentDataType.LONG);
            boolean hasExpire = meta.getPersistentDataContainer().has(amethystExpireKey, PersistentDataType.LONG);
            if (!hasRemaining && !hasExpire) continue;

            long remainingSeconds = getAmethystRemainingSeconds(meta);
            if (remainingSeconds <= 0) {
                item.setAmount(0);
                try { p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f); } catch (Exception ignored) {}
                continue;
            }

            Long totalDuration = meta.getPersistentDataContainer().get(amethystDurationKey, PersistentDataType.LONG);
            if (totalDuration == null || totalDuration <= 0) totalDuration = remainingSeconds;
            double fractionRemaining = Math.min(1.0, Math.max(0.0, (double) remainingSeconds / (double) totalDuration));
            String colorCode = getSmoothHexColor(fractionRemaining);

            List<String> lore = meta.getLore();
            if (lore != null) {
                List<String> newLore = new ArrayList<>();
                for (String line : lore) {
                    String clean = line.replaceAll("§[0-9a-fk-or]", "").trim();
                    if (clean.equalsIgnoreCase("Self Destruct") || clean.matches(".*\\d+d \\d+h \\d+m \\d+s.*")) {
                        if (clean.equalsIgnoreCase("Self Destruct")) {
                            newLore.add(color("&8Self Destruct"));
                        } else {
                            newLore.add(color(colorCode + formatAmethystTimeFormatted(remainingSeconds)));
                        }
                    } else {
                        newLore.add(line);
                    }
                }
                meta.setLore(newLore);
            }

            item.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onAmethystInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) {
            updateAmethystVisuals(p);
        }
    }

    @EventHandler
    public void onAmethystInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            updateAmethystVisuals(p);
        }
    }

    @EventHandler
    public void onAmethystInventoryClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            updateAmethystVisuals(p);
        }
    }

    // ==========================================
    // AFK CUBOID & REWARDS SYSTEM
    // ==========================================
    private void checkAfkZone(Player p, PlayerProfile prof) {
        String cuboidIn = getPlayerAfkCuboid(p);
        if (cuboidIn != null) {
            if (!afkTimeSeconds.containsKey(p.getUniqueId())) {
                p.sendTitle(color("&#A303F9ᴀꜰᴋ"), color("&fGanhar fragmentos a cada 1 minuto"), 10, 50, 15);
            }
            int secs = afkTimeSeconds.getOrDefault(p.getUniqueId(), 0) + 1;
            int intervalSecs = Math.max(1, getConfig().getInt("SHARDS.EVERY", 1)) * 60;
            int rem = Math.max(0, intervalSecs - secs);
            String countdownMsg = getConfig().getString("SHARDS.COUNTDOWN", "&7Next shard in &#A303F9%time%s").replace("%time%", String.valueOf(rem));
            sendHotbar(p, color(countdownMsg));

            if (secs >= intervalSecs) {
                secs = 0;
                long amount = getConfig().getLong("SHARDS.AMOUNT", 1L);
                if (shardBoosters.containsKey(p.getUniqueId()) && shardBoosters.get(p.getUniqueId()) > System.currentTimeMillis()) {
                    amount *= getConfig().getInt("SHARDS.BOOSTER-MULTIPLIER", 4);
                }
                prof.shards += amount;
                String recMsg = getConfig().getString("SHARDS.RECEIVED", "&#A303F9+%amount% Shard").replace("%amount%", String.valueOf(amount));
                p.sendMessage(color(recMsg));
                try { p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f); } catch (Exception ignored) {}
                saveProfile(prof);
            }
            afkTimeSeconds.put(p.getUniqueId(), secs);
        } else {
            afkTimeSeconds.remove(p.getUniqueId());
        }
    }

    public boolean isInSpawnZone(Player p) {
        String spawnCuboid = getConfig().getString("AFK-SYSTEM.SPAWN-CUBOID-NAME", "spawn");
        boolean inSpawn = isPlayerInCuboid(p, spawnCuboid);
        if (!inSpawn) {
            String cont = getContainingCuboidName(p.getLocation());
            if (cont != null && cont.toLowerCase().startsWith("spawn")) {
                inSpawn = true;
            }
        }
        if (!inSpawn && getCuboidsConfig().getConfigurationSection("CUBOIDS." + spawnCuboid) == null) {
            Location spawnLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-LOCATION", ""));
            if (spawnLoc != null && p.getWorld().equals(spawnLoc.getWorld()) && p.getLocation().distanceSquared(spawnLoc) <= 10000) {
                inSpawn = true;
            }
        }
        return inSpawn;
    }

    private void checkSpawnInactivityToAfk(Player p) {
        if (!getConfig().getBoolean("AFK-SYSTEM.ENABLED", true)) return;
        if (!isInSpawnZone(p)) return;

        long timeLimitSecs = getConfig().getLong("AFK-SYSTEM.TIME", 180L);
        long lastAct = lastActivityMillis.getOrDefault(p.getUniqueId(), System.currentTimeMillis());
        if ((System.currentTimeMillis() - lastAct) >= timeLimitSecs * 1000L) {
            Location afkLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-LOCATION", ""));
            if (afkLoc == null) {
                ConfigurationSection spawnsSec = getConfig().getConfigurationSection("LOCATIONS.AFK-SPAWNS");
                if (spawnsSec != null && !spawnsSec.getKeys(false).isEmpty()) {
                    afkLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-SPAWNS.1", ""));
                }
            }
            if (afkLoc == null) return;
            p.teleport(afkLoc);
            lastActivityMillis.put(p.getUniqueId(), System.currentTimeMillis());
            String msg = getConfig().getString("AFK-SYSTEM.MESSAGE", "&7You have been moved to the AFK area for being inactive in the spawn.");
            if (msg != null && !msg.isEmpty()) p.sendMessage(color(msg));
        }
    }

    public String getPlayerAfkCuboid(Player p) {
        ConfigurationSection cuboids = getCuboidsConfig().getConfigurationSection("CUBOIDS");
        if (cuboids == null) return null;
        for (String key : cuboids.getKeys(false)) {
            if (key.toLowerCase().startsWith("afk") || isAfkCuboidInMenu(key)) {
                if (isPlayerInCuboid(p, key)) {
                    return key;
                }
            }
        }
        return null;
    }

    public boolean isAfkCuboidInMenu(String cuboidName) {
        ConfigurationSection areas = menuConfig.getConfigurationSection("AFK-MENU.AREAS");
        if (areas != null) {
            for (String k : areas.getKeys(false)) {
                if (cuboidName.equalsIgnoreCase(areas.getString(k + ".CUBOID"))) return true;
            }
        }
        return false;
    }

    public boolean isLocationInCuboid(Location loc, String cuboidName) {
        if (loc == null || loc.getWorld() == null || cuboidName == null) return false;
        ConfigurationSection sec = getCuboidsConfig().getConfigurationSection("CUBOIDS." + cuboidName);
        if (sec == null) sec = getCuboidsConfig().getConfigurationSection("CUBOIDS.spawn" + cuboidName);
        if (sec == null) sec = getCuboidsConfig().getConfigurationSection("CUBOIDS.afk" + cuboidName);
        if (sec == null) return false;
        String worldName = sec.getString("WORLD", "world");
        if (!loc.getWorld().getName().equalsIgnoreCase(worldName)) return false;
        double minX = Math.min(sec.getDouble("MIN-X"), sec.getDouble("MAX-X"));
        double maxX = Math.max(sec.getDouble("MIN-X"), sec.getDouble("MAX-X"));
        double minY = Math.min(sec.getDouble("MIN-Y"), sec.getDouble("MAX-Y"));
        double maxY = Math.max(sec.getDouble("MIN-Y"), sec.getDouble("MAX-Y"));
        double minZ = Math.min(sec.getDouble("MIN-Z"), sec.getDouble("MAX-Z"));
        double maxZ = Math.max(sec.getDouble("MIN-Z"), sec.getDouble("MAX-Z"));
        return loc.getX() >= minX && loc.getX() <= maxX + 1 && loc.getY() >= minY && loc.getY() <= maxY + 1 && loc.getZ() >= minZ && loc.getZ() <= maxZ + 1;
    }

    public String getContainingCuboidName(Location loc) {
        ConfigurationSection cuboidsSec = getCuboidsConfig().getConfigurationSection("CUBOIDS");
        if (cuboidsSec == null || loc == null || loc.getWorld() == null) return null;
        for (String key : cuboidsSec.getKeys(false)) {
            if (isLocationInCuboid(loc, key)) {
                return key;
            }
        }
        return null;
    }

    public boolean isPlayerInCuboid(Player p, String cuboidName) {
        return isLocationInCuboid(p.getLocation(), cuboidName);
    }

    public String getPlayerLocationType(Player p) {
        if (getPlayerAfkCuboid(p) != null || isPlayerInCuboid(p, "afk") || isPlayerInCuboid(p, "afk1") || isPlayerInCuboid(p, "afk2")) {
            return "&#DF9FFFDᴀꜰᴋ";
        }
        Location afkLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-LOCATION", ""));
        if (afkLoc != null && p.getWorld().equals(afkLoc.getWorld()) && p.getLocation().distanceSquared(afkLoc) <= 10000) {
            return "&#DF9FFFDᴀꜰᴋ";
        }
        String spawnCuboid = getConfig().getString("AFK-SYSTEM.SPAWN-CUBOID-NAME", "spawn");
        if (isPlayerInCuboid(p, spawnCuboid) || isPlayerInCuboid(p, "spawn") || isPlayerInCuboid(p, "spawn1") || isPlayerInCuboid(p, "spawn2")) {
            return "&#6BF18Dѕᴘᴀᴡɴ";
        }
        Location spawnLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-LOCATION", ""));
        if (spawnLoc != null && p.getWorld().equals(spawnLoc.getWorld()) && p.getLocation().distanceSquared(spawnLoc) <= 10000) {
            return "&#6BF18Dѕᴘᴀᴡɴ";
        }
        ConfigurationSection cuboids = getCuboidsConfig().getConfigurationSection("CUBOIDS");
        if (cuboids != null) {
            for (String key : cuboids.getKeys(false)) {
                if (isPlayerInCuboid(p, key)) {
                    if (key.toLowerCase().startsWith("afk")) return "&#DF9FFFDᴀꜰᴋ";
                    if (key.toLowerCase().startsWith("spawn")) return "&#6BF18Dѕᴘᴀᴡɴ";
                }
            }
        }
        if (p.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER || p.getWorld().getName().toLowerCase().contains("nether")) {
            return "&#FF0000ɴᴇᴛʜᴇʀ";
        }
        if (p.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END || p.getWorld().getName().toLowerCase().contains("end")) {
            return "&#F5FF9Dᴛʜᴇ ᴇɴᴅ";
        }
        return "&#0CFF00ᴏᴠᴇʀᴡᴏʀʟᴅ";
    }

    public int countPlayersInCuboid(String cuboidName) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isPlayerInCuboid(p, cuboidName)) {
                count++;
            }
        }
        return count;
    }

    @EventHandler
    public void onAmethystItemHeldChange(org.bukkit.event.player.PlayerItemHeldEvent e) {
        updateAmethystVisuals(e.getPlayer());
    }

    public void spawnAmethystParticle(Location loc, String toolType) {
        if (loc == null || loc.getWorld() == null) return;
        String path = "AMETHYST-TOOLS." + toolType.toUpperCase() + ".PARTICLE";
        int amount = getConfig().getInt(path + ".AMOUNT", 10);
        String matName = getConfig().getString(path + ".MATERIAL", "PURPLE_CONCRETE_POWDER");
        Material mat = Material.matchMaterial(matName);
        if (mat == null || !mat.isBlock()) mat = Material.PURPLE_CONCRETE_POWDER;
        try {
            loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc.clone().add(0.5, 0.5, 0.5), amount, 0.3, 0.3, 0.3, mat.createBlockData());
        } catch (Throwable t) {
            try {
                loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0.5, 0.5, 0.5), amount, 0.3, 0.3, 0.3, 0.01);
            } catch (Throwable ignored) {}
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        PlayerProfile prof = getProfile(p);
        prof.blocksBroken++;
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.hasItemMeta() && hand.getItemMeta().getPersistentDataContainer().has(toolKey, PersistentDataType.STRING)) {
            String toolType = hand.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
            if (isAmethystToolExcluded(p)) {
                e.setCancelled(true);
                p.sendMessage(getMsg("AMETHYST.DISABLED-WORLD"));
                return;
            }
            if (isAmethystToolExpired(p, hand)) {
                e.setCancelled(true);
                return;
            }

            Block center = e.getBlock();
            if ("SELLAXE".equals(toolType) && center.getState() instanceof Chest) {
                e.setCancelled(true);
                Chest chest = (Chest) center.getState();
                double total = sellInventory(p, chest.getInventory());
                spawnAmethystParticle(center.getLocation(), "SELLAXE");
                if (total <= 0) {
                    sendHotbar(p, getMsg("AMETHYST.SELLAXE-NO-ITEMS"));
                    try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                }
                return;
            }

            if ("PICKAXE".equals(toolType)) {
                break3x3Filtered(p, center, hand, "PICKAXE");
            } else if ("SHOVEL".equals(toolType)) {
                break3x3Filtered(p, center, hand, "SHOVEL");
            } else if ("AXE".equals(toolType)) {
                if (isLog(center.getType())) {
                    chopTreeOnlyLogs(center, hand);
                    spawnAmethystParticle(center.getLocation(), "AXE");
                }
            }
        }
    }

    private boolean isLog(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.contains("LOG") || name.contains("WOOD") || name.contains("STEM") || name.contains("HYPHAE");
    }

    private BlockFace getTargetFace(Player p, Block center) {
        try {
            org.bukkit.util.RayTraceResult result = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 6, org.bukkit.FluidCollisionMode.NEVER);
            if (result != null && result.getHitBlock() != null && result.getHitBlock().equals(center) && result.getHitBlockFace() != null) {
                return result.getHitBlockFace();
            }
        } catch (Throwable ignored) {}
        try {
            BlockFace face = p.getTargetBlockFace(6);
            if (face != null) return face;
        } catch (Throwable ignored) {}
        
        float pitch = p.getLocation().getPitch();
        if (pitch < -45) return BlockFace.DOWN;
        if (pitch > 45) return BlockFace.UP;
        
        return p.getFacing().getOppositeFace();
    }

    private void break3x3Filtered(Player p, Block center, ItemStack tool, String toolType) {
        List<String> disabled = getConfig().getStringList("AMETHYST-TOOLS." + toolType + ".DISABLED-BLOCKS");
        List<String> allowed = getConfig().getStringList("AMETHYST-TOOLS." + toolType + ".ALLOWED-BLOCKS");
        boolean useAllowed = (allowed != null && !allowed.isEmpty());

        BlockFace face = getTargetFace(p, center);
        int minX = -1, maxX = 1;
        int minY = -1, maxY = 1;
        int minZ = -1, maxZ = 1;

        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            minY = 0; maxY = 0;
        } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
            minZ = 0; maxZ = 0;
        } else if (face == BlockFace.EAST || face == BlockFace.WEST) {
            minX = 0; maxX = 0;
        } else {
            minY = 0; maxY = 0;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block b = center.getRelative(x, y, z);
                    if (b.getType() == Material.BEDROCK || b.getType() == Material.AIR || b.getType().isInteractable()) continue;
                    String bName = b.getType().name();
                    if (useAllowed) {
                        if (!allowed.contains(bName)) continue;
                    } else if (disabled != null && disabled.contains(bName)) {
                        continue;
                    }
                    b.breakNaturally(tool);
                }
            }
        }
        spawnAmethystParticle(center.getLocation(), toolType);
    }

    private void chopTreeOnlyLogs(Block block, ItemStack tool) {
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        queue.add(block);
        visited.add(block);
        int count = 0;
        while (!queue.isEmpty() && count < 256) {
            Block curr = queue.poll();
            if (!curr.equals(block)) {
                curr.breakNaturally(tool);
            }
            count++;
            for (BlockFace face : BlockFace.values()) {
                Block rel = curr.getRelative(face);
                if (isLog(rel.getType()) && !visited.contains(rel)) {
                    visited.add(rel);
                    queue.add(rel);
                }
            }
        }
    }

    @EventHandler
    public void onTeleport(org.bukkit.event.player.PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (isInCombat(p) && getConfig().getBoolean("COMBAT-MANAGER.ANTI-STASIS-CHAMBER.ENABLED", true)) {
            if (e.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                boolean prevWorld = getConfig().getBoolean("COMBAT-MANAGER.ANTI-STASIS-CHAMBER.PREVENT-WORLD-CHANGE", true);
                double maxDist = getConfig().getDouble("COMBAT-MANAGER.ANTI-STASIS-CHAMBER.MAX-DISTANCE", 500);
                if ((prevWorld && !e.getFrom().getWorld().equals(e.getTo().getWorld())) || (e.getFrom().getWorld().equals(e.getTo().getWorld()) && e.getFrom().distance(e.getTo()) > maxDist)) {
                    e.setCancelled(true);
                    p.sendMessage(color("&cTeleporte por Ender Pearl bloqueado em combate (câmara de stasis detectada / distância excedida)."));
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (isInCombat(p) && getConfig().getBoolean("COMBAT-MANAGER.BLOCK-INTERACTIONS.ENABLED", true)) {
            ItemStack item = e.getItem();
            if (item != null) {
                List<String> blockedItems = getConfig().getStringList("COMBAT-MANAGER.BLOCK-INTERACTIONS.ITEMS");
                if (blockedItems != null && blockedItems.contains(item.getType().name())) {
                    e.setCancelled(true);
                    p.sendMessage(color(getConfig().getString("COMBAT-MANAGER.BLOCK-MESSAGE", "&cYou can't use this command in combat.")));
                    return;
                }
            }
        }
        if (e.getClickedBlock() != null && e.getClickedBlock().getType().name().contains("RESPAWN_ANCHOR")) {
            anchorExploders.put(e.getClickedBlock().getLocation(), p.getUniqueId());
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.ENDER_CHEST) {
            if (!p.isSneaking() || (e.getItem() == null || !e.getItem().getType().isBlock())) {
                boolean sixRows = getConfig().getBoolean("ENDER-CHEST.SIX-ROW", false) || p.hasPermission("donutcore.enderchest.sixrows") || p.hasPermission("donucore.enderchest.sixrows");
                if (sixRows) {
                    e.setCancelled(true);
                    activeEnderChestBlocks.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                    openCustomEnderChest(p);
                    return;
                }
            }
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand != null && hand.hasItemMeta() && (hand.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.STRING) || (hand.getType() == Material.GOLDEN_HOE && (hand.getItemMeta().getDisplayName().contains("ᴀꜰᴋ") || hand.getItemMeta().getDisplayName().contains("ᴡᴀɴᴅ") || hand.getItemMeta().getDisplayName().toLowerCase().contains("wand") || hand.getItemMeta().getDisplayName().toLowerCase().contains("selec") || hand.getItemMeta().getDisplayName().toLowerCase().contains("spawn"))))) {
            e.setCancelled(true);
            Location targetLoc = null;
            if (e.getClickedBlock() != null) {
                targetLoc = e.getClickedBlock().getLocation();
            } else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR) {
                org.bukkit.block.Block target = p.getTargetBlockExact(5);
                if (target != null) targetLoc = target.getLocation();
            }
            if (targetLoc != null) {
                String coords = targetLoc.getBlockX() + ", " + targetLoc.getBlockY() + ", " + targetLoc.getBlockZ();
                if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR) {
                    Location current = wandPos1.get(p.getUniqueId());
                    if (current == null || !current.equals(targetLoc)) {
                        wandPos1.put(p.getUniqueId(), targetLoc);
                        p.sendMessage(color("&ePos #1 &f- selecionado: &e" + coords));
                    }
                } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    Location current = wandPos2.get(p.getUniqueId());
                    if (current == null || !current.equals(targetLoc)) {
                        wandPos2.put(p.getUniqueId(), targetLoc);
                        p.sendMessage(color("&ePos #2 &f- selecionado: &e" + coords));
                    }
                }
            }
            return;
        }
        if (hand.hasItemMeta() && hand.getItemMeta().getPersistentDataContainer().has(toolKey, PersistentDataType.STRING)) {
            String type = hand.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
            if (isAmethystToolExcluded(p)) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    p.sendMessage(color("&cO uso de ferramentas Ametista está desativado neste mundo!"));
                    e.setCancelled(true);
                }
                return;
            }
            if (isAmethystToolExpired(p, hand)) {
                e.setCancelled(true);
                return;
            }

            if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if ("SELLAXE".equals(type) && e.getClickedBlock() != null && e.getClickedBlock().getState() instanceof Chest) {
                    e.setCancelled(true);
                    Chest chest = (Chest) e.getClickedBlock().getState();
                    double total = sellInventory(p, chest.getInventory());
                    spawnAmethystParticle(e.getClickedBlock().getLocation(), "SELLAXE");
                    if (total <= 0) {
                        sendHotbar(p, getMsg("AMETHYST.SELLAXE-NO-ITEMS"));
                        try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                    }
                }
            }

            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                if ("BUCKET".equals(type)) {
                    Block start = e.getClickedBlock();
                    if (start == null || !isWaterBlock(start)) {
                        start = p.getTargetBlockExact(5);
                    }
                    if (start != null && isWaterBlock(start)) {
                        e.setCancelled(true);
                        int cleared = drainWater4x4x2(start);
                        spawnAmethystParticle(start.getLocation(), "BUCKET");
                        try { p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f); } catch (Exception ignored) {}
                        sendHotbar(p, getMsg("AMETHYST.BUCKET-DRAINED").replace("{cleared}", String.valueOf(cleared)));
                        p.updateInventory();
                    }
                } else if ("BOOSTER".equals(type)) {
                    e.setCancelled(true);
                    if (shardBoosters.containsKey(p.getUniqueId()) && shardBoosters.get(p.getUniqueId()) > System.currentTimeMillis()) {
                        p.sendMessage(getMsg("SHARD-BOOSTER.ALREADY-ACTIVATED"));
                        return;
                    }
                    shardBoosters.put(p.getUniqueId(), System.currentTimeMillis() + (24 * 3600 * 1000L));
                    if (hand.getAmount() > 1) {
                        hand.setAmount(hand.getAmount() - 1);
                    } else {
                        p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                    }
                    spawnAmethystParticle(p.getLocation(), "BOOSTER");
                    try { p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f); } catch (Exception ignored) {}
                    p.sendMessage(getMsg("SHARD-BOOSTER.ACTIVATED"));
                }
            }
        }
    }

    @EventHandler
    public void onAmethystBucketFill(org.bukkit.event.player.PlayerBucketFillEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand != null && hand.hasItemMeta() && hand.getItemMeta().getPersistentDataContainer().has(toolKey, PersistentDataType.STRING)) {
            String type = hand.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
            if ("BUCKET".equals(type)) {
                e.setCancelled(true);
                Block start = e.getBlockClicked();
                if (start == null || !isWaterBlock(start)) {
                    start = p.getTargetBlockExact(5);
                }
                if (start != null && isWaterBlock(start)) {
                    int cleared = drainWater4x4x2(start);
                    spawnAmethystParticle(start.getLocation(), "BUCKET");
                    try { p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f); } catch (Exception ignored) {}
                    sendHotbar(p, getMsg("AMETHYST.BUCKET-DRAINED").replace("{cleared}", String.valueOf(cleared)));
                }
                p.updateInventory();
            }
        }
    }

    @EventHandler
    public void onAmethystBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand != null && hand.hasItemMeta() && hand.getItemMeta().getPersistentDataContainer().has(toolKey, PersistentDataType.STRING)) {
            String type = hand.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
            if ("BUCKET".equals(type)) {
                e.setCancelled(true);
                p.updateInventory();
            }
        }
    }

    private boolean isWaterBlock(Block b) {
        if (b == null) return false;
        if (b.getType() == Material.WATER || b.getType() == Material.BUBBLE_COLUMN || b.getType() == Material.SEAGRASS || b.getType() == Material.TALL_SEAGRASS || b.getType() == Material.KELP || b.getType() == Material.KELP_PLANT) {
            return true;
        }
        if (b.getBlockData() instanceof org.bukkit.block.data.Waterlogged) {
            return ((org.bukkit.block.data.Waterlogged) b.getBlockData()).isWaterlogged();
        }
        return false;
    }

    private int drainWater4x4x2(Block startBlock) {
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy >= -1; dy--) {
                    Block b = startBlock.getRelative(dx, dy, dz);
                    if (b.getType() == Material.WATER || b.getType() == Material.BUBBLE_COLUMN || b.getType() == Material.SEAGRASS || b.getType() == Material.TALL_SEAGRASS || b.getType() == Material.KELP || b.getType() == Material.KELP_PLANT) {
                        b.setType(Material.AIR);
                        count++;
                    } else if (b.getBlockData() instanceof org.bukkit.block.data.Waterlogged) {
                        org.bukkit.block.data.Waterlogged wl = (org.bukkit.block.data.Waterlogged) b.getBlockData();
                        if (wl.isWaterlogged()) {
                            wl.setWaterlogged(false);
                            b.setBlockData(wl);
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    // ==========================================
    // INVENTORY GUIS & SELL SYSTEM
    // ==========================================
    @SuppressWarnings({"deprecation", "removal"})
    public String getExactKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "";
        if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
            if (item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta) {
                org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
                org.bukkit.potion.PotionType pt = null;
                try {
                    pt = pm.getBasePotionType();
                } catch (Throwable t) {
                    try { pt = pm.getBasePotionData().getType(); } catch (Throwable ignored) {}
                }
                if (pt != null && pt != org.bukkit.potion.PotionType.WATER && pt != org.bukkit.potion.PotionType.MUNDANE && pt != org.bukkit.potion.PotionType.THICK && pt != org.bukkit.potion.PotionType.AWKWARD) {
                    return item.getType().name() + "_" + pt.name();
                }
            }
        } else if (item.getType() == Material.ENCHANTED_BOOK) {
            if (item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
                org.bukkit.inventory.meta.EnchantmentStorageMeta em = (org.bukkit.inventory.meta.EnchantmentStorageMeta) item.getItemMeta();
                if (!em.getStoredEnchants().isEmpty()) {
                    org.bukkit.enchantments.Enchantment ench = em.getStoredEnchants().keySet().iterator().next();
                    String enchName = ench.getKey().getKey().toUpperCase();
                    return "ENCHANTED_BOOK_" + enchName;
                }
            }
        }
        return item.getType().name();
    }

    @SuppressWarnings({"deprecation", "removal"})
    public ItemStack resolveItem(String key) {
        if (key == null || key.isEmpty()) return null;
        Material mat = Material.matchMaterial(key);
        if (mat != null) {
            return new ItemStack(mat);
        }
        if (key.startsWith("POTION_") || key.startsWith("SPLASH_POTION_") || key.startsWith("LINGERING_POTION_")) {
            Material baseMat = Material.POTION;
            String typeStr = key;
            if (key.startsWith("SPLASH_POTION_")) {
                baseMat = Material.SPLASH_POTION;
                typeStr = key.substring(14);
            } else if (key.startsWith("LINGERING_POTION_")) {
                baseMat = Material.LINGERING_POTION;
                typeStr = key.substring(17);
            } else {
                typeStr = key.substring(7);
            }
            ItemStack item = new ItemStack(baseMat);
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.PotionMeta) {
                org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) meta;
                try {
                    org.bukkit.potion.PotionType pt = org.bukkit.potion.PotionType.valueOf(typeStr);
                    try {
                        pm.setBasePotionType(pt);
                    } catch (Throwable t) {
                        pm.setBasePotionData(new org.bukkit.potion.PotionData(pt, false, false));
                    }
                    item.setItemMeta(pm);
                } catch (Exception ignored) {
                    item.setItemMeta(pm);
                }
            }
            return item;
        } else if (key.startsWith("ENCHANTED_BOOK_")) {
            String enchStr = key.substring(15);
            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
                org.bukkit.inventory.meta.EnchantmentStorageMeta em = (org.bukkit.inventory.meta.EnchantmentStorageMeta) meta;
                try {
                    org.bukkit.enchantments.Enchantment ench = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchStr.toLowerCase()));
                    if (ench != null) {
                        em.addStoredEnchant(ench, 1, true);
                    }
                } catch (Exception ignored) {}
                item.setItemMeta(em);
            }
            return item;
        }
        return null;
    }

    private String formatItemName(String key) {
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    public String getCategory(ItemStack item) {
        return getCategoryByKey(getExactKey(item), item != null ? item.getType() : null);
    }

    public String getCategory(Material mat) {
        return getCategoryByKey(mat != null ? mat.name() : "", mat);
    }

    public String getCategoryByKey(String key, Material fallbackMat) {
        if (worthConfig != null) {
            String[] cats = {"CROPS", "ORES", "MOBS", "NATURAL", "ARMOR_AND_TOOLS", "FISH", "BOOK", "POTION", "BLOCKS"};
            for (String cat : cats) {
                if (worthConfig.contains("TYPE." + cat + "." + key) || (fallbackMat != null && worthConfig.contains("TYPE." + cat + "." + fallbackMat.name()))) {
                    return cat;
                }
            }
        }
        if (fallbackMat != null) {
            String name = fallbackMat.name();
            if (name.contains("COD") || name.contains("SALMON") || name.contains("TROPICAL_FISH") || name.contains("PUFFERFISH") || name.contains("NAUTILUS") || name.contains("KELP") || name.contains("SEA_PICKLE") || name.contains("FROGLIGHT")) {
                return "FISH";
            }
            if (name.contains("POTION") || name.equals("BREWING_STAND") || name.equals("CAULDRON") || name.equals("GLASS_BOTTLE") || name.equals("FERMENTED_SPIDER_EYE") || name.equals("GLISTERING_MELON_SLICE") || name.equals("MAGMA_CREAM") || name.equals("DRAGON_BREATH")) {
                return "POTION";
            }
            if (name.contains("BOOK") || name.equals("PAPER") || name.contains("MAP") || name.equals("LECTERN")) {
                return "BOOK";
            }
            if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS") || name.contains("SWORD") || name.contains("AXE") || name.contains("PICKAXE") || name.contains("SHOVEL") || name.contains("HOE") || name.contains("BOW") || name.contains("CROSSBOW") || name.contains("TRIDENT") || name.contains("MACE") || name.contains("SHIELD") || name.contains("FISHING_ROD") || name.contains("SHEARS") || name.contains("FLINT_AND_STEEL") || name.contains("BRUSH") || name.contains("SPYGLASS") || name.contains("HORSE_ARMOR") || name.contains("TRIM") || name.contains("TEMPLATE") || name.contains("SPEAR") || name.contains("HARNESS")) {
                return "ARMOR_AND_TOOLS";
            }
            if (name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("GOLD") || name.contains("IRON") || name.contains("COPPER") || name.contains("NETHERITE") || name.contains("COAL") || name.contains("REDSTONE") || name.contains("LAPIS") || name.contains("QUARTZ") || name.contains("AMETHYST") || name.contains("ANCIENT_DEBRIS") || name.contains("RAW_") || name.endsWith("_ORE") || name.contains("NUGGET")) {
                return "ORES";
            }
            if (name.contains("BONE") || name.contains("ROTTEN_FLESH") || name.contains("STRING") || name.contains("SPIDER_EYE") || name.contains("GUNPOWDER") || name.contains("ENDER_PEARL") || name.contains("BLAZE_ROD") || name.contains("BLAZE_POWDER") || name.contains("GHAST_TEAR") || name.contains("SLIME_BALL") || name.contains("FEATHER") || name.contains("LEATHER") || name.contains("INK_SAC") || name.contains("SCUTE") || name.contains("SHULKER_SHELL") || name.contains("PHANTOM_MEMBRANE") || name.contains("RABBIT_FOOT") || name.contains("RABBIT_HIDE") || name.contains("SKULL") || name.contains("HEAD") || name.equals("EGG") || name.contains("BREEZE_ROD") || name.contains("HEAVY_CORE")) {
                return "MOBS";
            }
            if (name.contains("WHEAT") || name.contains("CARROT") || name.contains("POTATO") || name.contains("BEETROOT") || name.contains("MELON") || name.contains("PUMPKIN") || name.contains("SUGAR_CANE") || name.contains("CACTUS") || name.contains("BAMBOO") || name.contains("COCOA") || name.contains("APPLE") || name.contains("SWEET_BERRIES") || name.contains("GLOW_BERRIES") || name.contains("CHORUS") || name.contains("SEEDS") || name.contains("NETHER_WART") || name.contains("BREAD") || name.contains("COOKIE") || name.contains("PIE") || name.contains("BEEF") || name.contains("PORKCHOP") || name.contains("CHICKEN") || name.contains("MUTTON") || name.contains("RABBIT") || name.contains("MUSHROOM") || name.contains("STEW") || name.contains("SOUP") || name.contains("HONEY")) {
                return "CROPS";
            }
            if (name.contains("DIRT") || name.contains("GRASS") || name.contains("SAND") || name.contains("GRAVEL") || name.contains("STONE") || name.contains("COBBLESTONE") || name.contains("ANDESITE") || name.contains("DIORITE") || name.contains("GRANITE") || name.contains("DEEPSLATE") || name.contains("TUFF") || name.contains("BASALT") || name.contains("BLACKSTONE") || name.contains("OBSIDIAN") || name.contains("LOG") || name.contains("WOOD") || name.contains("LEAVES") || name.contains("SAPLING") || name.contains("FLOWER") || name.contains("TULIP") || name.contains("ORCHID") || name.contains("DANDELION") || name.contains("POPPY") || name.contains("CORAL") || name.contains("ICE") || name.contains("SNOW") || name.contains("CLAY") || name.contains("MOSS") || name.contains("ROOTS") || name.contains("VINE") || name.contains("LILY")) {
                return "NATURAL";
            }
            return "BLOCKS";
        }
        return "CROPS";
    }

    public double getWorth(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0.0;
        if (isBlocked(item)) return 0.0;
        double base = getWorthByKey(getExactKey(item), item.getType());
        if (item.getType().name().endsWith("SHULKER_BOX") && item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta) {
            org.bukkit.inventory.meta.BlockStateMeta bsm = (org.bukkit.inventory.meta.BlockStateMeta) item.getItemMeta();
            if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox) {
                org.bukkit.block.ShulkerBox shulker = (org.bukkit.block.ShulkerBox) bsm.getBlockState();
                for (ItemStack inner : shulker.getInventory().getContents()) {
                    if (inner != null && inner.getType() != Material.AIR && !isBlocked(inner)) {
                        double innerWorth = getWorthByKey(getExactKey(inner), inner.getType());
                        if (innerWorth > 0) {
                            base += innerWorth * inner.getAmount();
                        }
                    }
                }
            }
        }
        return base;
    }

    public double getWorth(Material mat) {
        if (mat == null) return 0.0;
        return getWorthByKey(mat.name(), mat);
    }

    public double getWorthByKey(String key, Material fallbackMat) {
        if (worthConfig != null) {
            String[] cats = {"CROPS", "ORES", "MOBS", "NATURAL", "ARMOR_AND_TOOLS", "FISH", "BOOK", "POTION", "BLOCKS"};
            for (String cat : cats) {
                if (worthConfig.contains("TYPE." + cat + "." + key)) {
                    return worthConfig.getDouble("TYPE." + cat + "." + key);
                }
                if (fallbackMat != null && worthConfig.contains("TYPE." + cat + "." + fallbackMat.name())) {
                    return worthConfig.getDouble("TYPE." + cat + "." + fallbackMat.name());
                }
            }
            if (worthConfig.contains("PRICES." + key)) {
                return worthConfig.getDouble("PRICES." + key);
            }
            if (fallbackMat != null && worthConfig.contains("PRICES." + fallbackMat.name())) {
                return worthConfig.getDouble("PRICES." + fallbackMat.name());
            }
        }
        return getConfig().getDouble("WORTH." + (fallbackMat != null ? fallbackMat.name() : key), 0.0);
    }

    public boolean hasWorth(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (isBlocked(item)) return false;
        if (isShulkerBox(item)) {
            return getWorth(item) > 0.0;
        }
        return getWorthByKey(getExactKey(item), item.getType()) > 0.0;
    }

    public boolean isShulkerBox(ItemStack item) {
        return item != null && item.getType().name().endsWith("SHULKER_BOX") && item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta;
    }

    public boolean isLorePriceEnabled() {
        FileConfiguration cfg = getConfig();
        if (cfg.contains("LORE-PRICE.ENABLED")) return cfg.getBoolean("LORE-PRICE.ENABLED", true);
        if (cfg.contains("lore-price.enabled")) return cfg.getBoolean("lore-price.enabled", true);
        if (cfg.contains("LORE-PRICE") && cfg.isBoolean("LORE-PRICE")) return cfg.getBoolean("LORE-PRICE", true);
        if (cfg.contains("lore-price") && cfg.isBoolean("lore-price")) return cfg.getBoolean("lore-price", true);
        if (cfg.contains("WORTH-LORE.ENABLED")) return cfg.getBoolean("WORTH-LORE.ENABLED", true);
        if (cfg.contains("worth-lore.enabled")) return cfg.getBoolean("worth-lore.enabled", true);
        if (worthConfig != null) {
            if (worthConfig.contains("LORE-PRICE.ENABLED")) return worthConfig.getBoolean("LORE-PRICE.ENABLED", true);
            if (worthConfig.contains("lore-price.enabled")) return worthConfig.getBoolean("lore-price.enabled", true);
        }
        return true;
    }

    public String getLorePriceFormat() {
        FileConfiguration cfg = getConfig();
        if (cfg.contains("LORE-PRICE.FORMAT")) return cfg.getString("LORE-PRICE.FORMAT");
        if (cfg.contains("lore-price.format")) return cfg.getString("lore-price.format");
        if (cfg.contains("LORE-PRICE") && cfg.isString("LORE-PRICE")) return cfg.getString("LORE-PRICE");
        if (cfg.contains("lore-price") && cfg.isString("lore-price")) return cfg.getString("lore-price");
        if (cfg.contains("WORTH-LORE.FORMAT")) return cfg.getString("WORTH-LORE.FORMAT");
        if (cfg.contains("worth-lore.format")) return cfg.getString("worth-lore.format");
        if (worthConfig != null) {
            if (worthConfig.contains("LORE-PRICE.FORMAT")) return worthConfig.getString("LORE-PRICE.FORMAT");
            if (worthConfig.contains("lore-price.format")) return worthConfig.getString("lore-price.format");
            if (worthConfig.contains("LORE-FORMAT")) return worthConfig.getString("LORE-FORMAT");
        }
        return "&7Preço: &a${price}";
    }

    public List<String> getBlockedLoreItems() {
        List<String> list = new ArrayList<>();
        FileConfiguration cfg = getConfig();
        if (cfg.contains("LORE-PRICE.BLOCK-ITEMS")) list.addAll(cfg.getStringList("LORE-PRICE.BLOCK-ITEMS"));
        if (cfg.contains("lore-price.block-items")) list.addAll(cfg.getStringList("lore-price.block-items"));
        if (cfg.contains("LORE-PRICE.BLOCK-ITENS")) list.addAll(cfg.getStringList("LORE-PRICE.BLOCK-ITENS"));
        if (cfg.contains("lore-price.block-itens")) list.addAll(cfg.getStringList("lore-price.block-itens"));
        if (cfg.contains("LORE-PRICE.BLOCKED-ITEMS")) list.addAll(cfg.getStringList("LORE-PRICE.BLOCKED-ITEMS"));
        if (cfg.contains("WORTH-LORE.BLOCK-ITEMS")) list.addAll(cfg.getStringList("WORTH-LORE.BLOCK-ITEMS"));
        if (worthConfig != null) {
            if (worthConfig.contains("BLOCK-ITENS")) list.addAll(worthConfig.getStringList("BLOCK-ITENS"));
            if (worthConfig.contains("BLOCK-ITEMS")) list.addAll(worthConfig.getStringList("BLOCK-ITEMS"));
            if (worthConfig.contains("LORE-PRICE.BLOCK-ITEMS")) list.addAll(worthConfig.getStringList("LORE-PRICE.BLOCK-ITEMS"));
        }
        return list;
    }

    public int getTopInventorySize(Player player) {
        if (player == null) return 0;
        Integer cached = openTopInventorySizes.get(player.getUniqueId());
        if (cached != null && cached > 0) {
            return cached;
        }
        try {
            org.bukkit.inventory.InventoryView view = player.getOpenInventory();
            if (view != null) {
                org.bukkit.inventory.Inventory top = view.getTopInventory();
                if (top != null && !(top instanceof org.bukkit.inventory.PlayerInventory)) {
                    return top.getSize();
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public boolean shouldBlockCustomNbt() {
        FileConfiguration cfg = getConfig();
        if (cfg.contains("LORE-PRICE.BLOCK-CUSTOM-NBT-ITEMS")) {
            return cfg.getBoolean("LORE-PRICE.BLOCK-CUSTOM-NBT-ITEMS", true);
        }
        if (cfg.contains("lore-price.block-custom-nbt-items")) {
            return cfg.getBoolean("lore-price.block-custom-nbt-items", true);
        }
        if (cfg.contains("LORE-PRICE.BLOCK-CUSTOM-ITEMS")) {
            return cfg.getBoolean("LORE-PRICE.BLOCK-CUSTOM-ITEMS", true);
        }
        if (cfg.contains("SETTINGS.BLOCK-CUSTOM-NBT-ITEMS")) {
            return cfg.getBoolean("SETTINGS.BLOCK-CUSTOM-NBT-ITEMS", true);
        }
        if (worthConfig != null) {
            if (worthConfig.contains("BLOCK-CUSTOM-NBT-ITEMS")) {
                return worthConfig.getBoolean("BLOCK-CUSTOM-NBT-ITEMS", true);
            }
            if (worthConfig.contains("LORE-PRICE.BLOCK-CUSTOM-NBT-ITEMS")) {
                return worthConfig.getBoolean("LORE-PRICE.BLOCK-CUSTOM-NBT-ITEMS", true);
            }
        }
        return true;
    }

    public boolean hasCustomNbt(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        try {
            if (!meta.getPersistentDataContainer().getKeys().isEmpty()) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            if (meta.hasCustomModelData()) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            if (meta.hasDisplayName()) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            if (meta.hasLore()) {
                List<Component> lore = meta.lore();
                if (lore != null && !lore.isEmpty()) {
                    String format = getLorePriceFormat();
                    String formatPrefix = format != null ? format.split("(?i)[\\$%{]")[0] : "";
                    String strippedPrefix = org.bukkit.ChatColor.stripColor(color(formatPrefix)).trim();

                    for (Component comp : lore) {
                        String serialized = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(comp);
                        String plain = org.bukkit.ChatColor.stripColor(serialized).trim();
                        if (!plain.startsWith("Preço:") && !plain.startsWith("Preco:")
                                && !plain.startsWith("Price:") && !plain.startsWith("Worth:")
                                && !plain.startsWith("Valor:")
                                && (strippedPrefix.isEmpty() || !plain.startsWith(strippedPrefix))) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            if (item.getType() != Material.ENCHANTED_BOOK && meta.hasEnchants()) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            if (meta.isUnbreakable()) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            if (!meta.getItemFlags().isEmpty()) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public boolean isBlocked(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (shouldBlockCustomNbt() && hasCustomNbt(item)) {
            return true;
        }
        List<String> blockedList = getBlockedLoreItems();
        if (!blockedList.isEmpty()) {
            String name = item.getType().name();
            String key = getExactKey(item);
            for (String blocked : blockedList) {
                if (blocked == null) continue;
                String clean = blocked.trim().replace("minecraft:", "");
                if (clean.equalsIgnoreCase(name) || clean.equalsIgnoreCase(key)) {
                    return true;
                }
            }
        }
        if (isShulkerBox(item)) {
            org.bukkit.inventory.meta.BlockStateMeta bsm = (org.bukkit.inventory.meta.BlockStateMeta) item.getItemMeta();
            if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox) {
                org.bukkit.block.ShulkerBox shulker = (org.bukkit.block.ShulkerBox) bsm.getBlockState();
                for (ItemStack inner : shulker.getInventory().getContents()) {
                    if (inner != null && inner.getType() != Material.AIR && isBlocked(inner)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isMultiplierButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey key = new NamespacedKey(this, "multiplier_button");
        return meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    public ItemStack createPricedItem(Player player, ItemStack item) {
        if (!isLorePriceEnabled()) return null;
        if (item == null || item.getType() == Material.AIR) return null;
        if (isMultiplierButton(item)) return null;
        if (isBlocked(item)) return null;

        double unitPrice = getWorthByKey(getExactKey(item), item.getType());
        boolean isShulker = isShulkerBox(item);
        if (isShulker) {
            unitPrice = getWorth(item);
        }
        if (unitPrice <= 0.0) return null;

        PlayerProfile prof = getProfile(player);
        String category = getCategory(item);
        double mult = prof != null ? prof.getMultiplier(category) : 1.0;
        double totalPrice = (isShulker ? unitPrice : (unitPrice * item.getAmount())) * mult;

        String formatted = String.format(Locale.US, "%,.2f", totalPrice);
        String loreFormat = getLorePriceFormat();
        String loreLine = loreFormat
                .replace("${price}", formatted)
                .replace("{price}", formatted)
                .replace("%price%", formatted)
                .replace("${valor}", formatted)
                .replace("{valor}", formatted)
                .replace("%valor%", formatted)
                .replace("${value}", formatted)
                .replace("{value}", formatted)
                .replace("%value%", formatted);

        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;

        String formatPrefix = loreFormat.split("(?i)[\\$%{]")[0];
        String strippedPrefix = org.bukkit.ChatColor.stripColor(color(formatPrefix)).trim();

        List<Component> lore = meta.lore();
        List<Component> cleaned = new ArrayList<>();
        if (lore != null) {
            for (Component comp : lore) {
                String serialized = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(comp);
                String plain = org.bukkit.ChatColor.stripColor(serialized).trim();
                if (plain.startsWith("Preço:") || plain.startsWith("Preco:") || plain.startsWith("Price:") || plain.startsWith("Worth:") || plain.startsWith("Valor:") || (!strippedPrefix.isEmpty() && plain.startsWith(strippedPrefix))) {
                    continue;
                }
                cleaned.add(comp);
            }
        }
        cleaned.add(parseMiniMessageOrLegacy(loreLine));
        meta.lore(cleaned);
        clone.setItemMeta(meta);
        return clone;
    }

    public void stripPriceLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return;

        String format = getLorePriceFormat();
        String formatPrefix = format != null ? format.split("(?i)[\\$%{]")[0] : "";
        String strippedPrefix = org.bukkit.ChatColor.stripColor(color(formatPrefix)).trim();

        List<Component> cleaned = new ArrayList<>();
        boolean changed = false;
        for (Component comp : lore) {
            String serialized = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(comp);
            String plain = org.bukkit.ChatColor.stripColor(serialized).trim();
            if (plain.startsWith("Preço:") || plain.startsWith("Preco:") || plain.startsWith("Price:") || plain.startsWith("Worth:") || plain.startsWith("Valor:") || (!strippedPrefix.isEmpty() && plain.startsWith(strippedPrefix))) {
                changed = true;
            } else {
                cleaned.add(comp);
            }
        }

        if (changed) {
            meta.lore(cleaned.isEmpty() ? null : cleaned);
            item.setItemMeta(meta);
        }
    }

    public double sellInventory(Player p, Inventory inv) {
        double total = 0;
        PlayerProfile prof = getProfile(p);
        boolean isSellMenu = (inv.getHolder() instanceof GUIHolder && "SELL-MENU".equals(((GUIHolder) inv.getHolder()).getMenuId()));
        int sellLimit = (inv instanceof PlayerInventory) ? 36 : (isSellMenu ? 45 : inv.getSize());
        boolean returnedAny = false;

        for (int i = 0; i < sellLimit; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                stripPriceLore(item);
                boolean isShulker = item.getType().name().endsWith("SHULKER_BOX") && item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta;
                if (isShulker) {
                    org.bukkit.inventory.meta.BlockStateMeta bsm = (org.bukkit.inventory.meta.BlockStateMeta) item.getItemMeta();
                    if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox) {
                        org.bukkit.block.ShulkerBox shulker = (org.bukkit.block.ShulkerBox) bsm.getBlockState();
                        Inventory shulkerInv = shulker.getInventory();
                        boolean modifiedInner = false;

                        for (int s = 0; s < shulkerInv.getSize(); s++) {
                            ItemStack innerItem = shulkerInv.getItem(s);
                            if (innerItem != null && innerItem.getType() != Material.AIR) {
                                if (isBlocked(innerItem)) {
                                    continue;
                                }
                                double innerBasePrice = getWorthByKey(getExactKey(innerItem), innerItem.getType());
                                if (innerBasePrice > 0) {
                                    String cat = getCategory(innerItem);
                                    String exactKey = getExactKey(innerItem);
                                    double mult = prof.getMultiplier(cat);
                                    double earned = innerBasePrice * mult * innerItem.getAmount();
                                    total += earned;
                                    prof.addSellProgress(cat, earned);
                                    prof.soldPriceHistory.put(exactKey, prof.soldPriceHistory.getOrDefault(exactKey, 0.0) + earned);
                                    prof.soldAmountHistory.put(exactKey, prof.soldAmountHistory.getOrDefault(exactKey, 0) + innerItem.getAmount());
                                    prof.lastSoldTime.put(exactKey, System.currentTimeMillis());
                                    shulkerInv.setItem(s, null);
                                    modifiedInner = true;
                                }
                            }
                        }

                        if (modifiedInner) {
                            bsm.setBlockState(shulker);
                            item.setItemMeta(bsm);
                        }

                        boolean isEmptyNow = shulkerInv.isEmpty();
                        double boxBasePrice = (!isBlocked(item)) ? getWorthByKey(getExactKey(item), item.getType()) : 0.0;

                        if (isEmptyNow && boxBasePrice > 0) {
                            String cat = getCategory(item);
                            String exactKey = getExactKey(item);
                            double mult = prof.getMultiplier(cat);
                            double earned = boxBasePrice * mult * item.getAmount();
                            total += earned;
                            prof.addSellProgress(cat, earned);
                            prof.soldPriceHistory.put(exactKey, prof.soldPriceHistory.getOrDefault(exactKey, 0.0) + earned);
                            prof.soldAmountHistory.put(exactKey, prof.soldAmountHistory.getOrDefault(exactKey, 0) + item.getAmount());
                            prof.lastSoldTime.put(exactKey, System.currentTimeMillis());
                            inv.setItem(i, null);
                        } else if (isEmptyNow && modifiedInner) {
                            if (isSellMenu) {
                                inv.setItem(i, null);
                                Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
                                for (ItemStack drop : leftover.values()) {
                                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                                }
                                returnedAny = true;
                            } else {
                                inv.setItem(i, item);
                            }
                        } else if (!isEmptyNow) {
                            if (isSellMenu) {
                                inv.setItem(i, null);
                                Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
                                for (ItemStack drop : leftover.values()) {
                                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                                }
                                returnedAny = true;
                            } else {
                                inv.setItem(i, item);
                            }
                        } else if (isEmptyNow && boxBasePrice <= 0 && isSellMenu) {
                            inv.setItem(i, null);
                            Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
                            for (ItemStack drop : leftover.values()) {
                                p.getWorld().dropItemNaturally(p.getLocation(), drop);
                            }
                            returnedAny = true;
                        }
                        continue;
                    }
                }

                if (isBlocked(item)) {
                    if (isSellMenu) {
                        inv.setItem(i, null);
                        Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
                        for (ItemStack drop : leftover.values()) {
                            p.getWorld().dropItemNaturally(p.getLocation(), drop);
                        }
                        returnedAny = true;
                    }
                    continue;
                }

                double basePrice = getWorthByKey(getExactKey(item), item.getType());
                if (basePrice > 0) {
                    String cat = getCategory(item);
                    String exactKey = getExactKey(item);
                    double mult = prof.getMultiplier(cat);
                    double earned = basePrice * mult * item.getAmount();
                    total += earned;
                    prof.addSellProgress(cat, earned);
                    prof.soldPriceHistory.put(exactKey, prof.soldPriceHistory.getOrDefault(exactKey, 0.0) + earned);
                    prof.soldAmountHistory.put(exactKey, prof.soldAmountHistory.getOrDefault(exactKey, 0) + item.getAmount());
                    prof.lastSoldTime.put(exactKey, System.currentTimeMillis());
                    inv.setItem(i, null);
                } else if (isSellMenu) {
                    inv.setItem(i, null);
                    Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }
                    returnedAny = true;
                }
            }
        }
        if (returnedAny) {
            p.sendMessage(color("&c[Sell] Alguns itens não vendáveis foram devolvidos ao seu inventário."));
        }
        if (total > 0) {
            prof.money += total;
            prof.moneyMade += total;
            playSound(p, "SELL.LEVEL-UP");
            String msgStr = getConfig().getString("SETTINGS.SELL-MESSAGE", getConfig().getString("SELL-MESSAGE", "&a+$%price%"));
            String priceFormatted = String.format("%.2f", total);
            String msg = color(msgStr.replace("%price%", priceFormatted).replace("${price}", "$" + priceFormatted).replace("{price}", priceFormatted));
            sendHotbar(p, msg);
        }
        return total;
    }

    private double getStatValue(PlayerProfile prof, String typeKey) {
        if (typeKey.equalsIgnoreCase("money")) return prof.money;
        if (typeKey.equalsIgnoreCase("moneySpent")) return prof.moneySpent;
        if (typeKey.equalsIgnoreCase("moneyMade")) return prof.moneyMade;
        if (typeKey.equalsIgnoreCase("kills")) return prof.kills;
        if (typeKey.equalsIgnoreCase("deaths")) return prof.deaths;
        if (typeKey.equalsIgnoreCase("playtime")) return prof.playtimeSeconds;
        if (typeKey.equalsIgnoreCase("blocksPlaced")) return prof.blocksPlaced;
        if (typeKey.equalsIgnoreCase("blocksBroken")) return prof.blocksBroken;
        if (typeKey.equalsIgnoreCase("mobsKilled")) return prof.mobsKilled;
        if (typeKey.equalsIgnoreCase("killStreak")) return prof.killStreak;
        if (typeKey.equalsIgnoreCase("highestKillStreak")) return prof.highestKillStreak;
        if (typeKey.equalsIgnoreCase("shards")) return prof.shards;
        return 0.0;
    }

    public static String formatAbbreviated(double amount) {
        if (amount < 1000) {
            if (amount == (long) amount) {
                return String.valueOf((long) amount);
            }
            return String.format(Locale.US, "%.2f", amount);
        }
        String[] suffixes = {"", "k", "m", "b", "t", "qd", "qn", "sx", "sp", "oc", "no", "dc"};
        int index = 0;
        double value = amount;
        while (value >= 1000 && index < suffixes.length - 1) {
            value /= 1000.0;
            index++;
        }
        String formatted = String.format(Locale.US, "%.2f", value);
        if (formatted.endsWith(".00")) {
            formatted = formatted.substring(0, formatted.length() - 3);
        } else if (formatted.endsWith("0") && formatted.contains(".")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted + suffixes[index];
    }

    private String formatStatValue(PlayerProfile prof, String typeKey) {
        if (typeKey.equalsIgnoreCase("money") || typeKey.equalsIgnoreCase("moneySpent") || typeKey.equalsIgnoreCase("moneyMade")) {
            return "$" + formatAbbreviated(getStatValue(prof, typeKey));
        }
        if (typeKey.equalsIgnoreCase("playtime")) {
            return formatTime(prof.playtimeSeconds);
        }
        if (typeKey.equalsIgnoreCase("shards")) {
            return formatAbbreviated(getStatValue(prof, typeKey));
        }
        return String.valueOf((long) getStatValue(prof, typeKey));
    }

    private boolean matchesWorthFilter(String key, String cat, String filter) {
        if (filter == null || filter.equalsIgnoreCase("Todos")) return true;
        ItemStack item = resolveItem(key);
        Material mat = item != null ? item.getType() : Material.matchMaterial(key);
        if (filter.equalsIgnoreCase("Blocos")) {
            return cat.equalsIgnoreCase("BLOCKS") || cat.equalsIgnoreCase("ORES") || cat.equalsIgnoreCase("NATURAL") || key.contains("BLOCK") || (mat != null && mat.isBlock());
        } else if (filter.equalsIgnoreCase("Ferramentas")) {
            return (cat.equalsIgnoreCase("ARMOR_AND_TOOLS") || key.contains("PICKAXE") || key.contains("AXE") || key.contains("SHOVEL") || key.contains("HOE") || key.contains("FISHING_ROD") || key.contains("SHEARS") || key.contains("FLINT_AND_STEEL") || key.contains("COMPASS") || key.contains("CLOCK"))
                    && !key.contains("HELMET") && !key.contains("CHESTPLATE") && !key.contains("LEGGINGS") && !key.contains("BOOTS") && !key.contains("SWORD") && !key.contains("BOW") && !key.contains("ARROW") && !key.contains("SHIELD") && !key.contains("MACE") && !key.contains("TRIDENT") && !key.contains("TRIM");
        } else if (filter.equalsIgnoreCase("Comida")) {
            return cat.equalsIgnoreCase("FISH") || cat.equalsIgnoreCase("CROPS") || key.contains("BEEF") || key.contains("PORK") || key.contains("CHICKEN") || key.contains("MUTTON") || key.contains("RABBIT") || key.contains("APPLE") || key.contains("BREAD") || key.contains("POTATO") || key.contains("CARROT") || key.contains("BERRIES") || key.contains("MELON") || key.contains("COOKIE") || key.contains("PIE") || key.contains("STEW") || key.contains("SOUP") || (mat != null && mat.isEdible());
        } else if (filter.equalsIgnoreCase("Combate")) {
            return key.contains("SWORD") || key.contains("BOW") || key.contains("ARROW") || key.contains("SHIELD") || key.contains("HELMET") || key.contains("CHESTPLATE") || key.contains("LEGGINGS") || key.contains("BOOTS") || key.contains("TRIM") || key.contains("MACE") || key.contains("TRIDENT") || key.contains("CROSSBOW");
        } else if (filter.equalsIgnoreCase("Poções")) {
            return cat.equalsIgnoreCase("POTION") || key.contains("POTION") || key.contains("BREWING") || key.contains("CAULDRON") || key.contains("BOTTLE");
        } else if (filter.equalsIgnoreCase("Livros")) {
            return cat.equalsIgnoreCase("BOOK") || key.contains("BOOK") || key.contains("PAPER") || key.contains("MAP") || key.contains("LECTERN");
        } else if (filter.equalsIgnoreCase("Ingredientes")) {
            return cat.equalsIgnoreCase("ORES") || cat.equalsIgnoreCase("MOBS") || cat.equalsIgnoreCase("CROPS") || key.contains("INGOT") || key.contains("DIAMOND") || key.contains("EMERALD") || key.contains("COAL") || key.contains("REDSTONE") || key.contains("LAPIS") || key.contains("QUARTZ") || key.contains("NETHERITE") || key.contains("STICK") || key.contains("STRING") || key.contains("GUNPOWDER") || key.contains("SLIME") || key.contains("BONE") || key.contains("FEATHER") || key.contains("LEATHER");
        } else if (filter.equalsIgnoreCase("Utilidades")) {
            return key.contains("BUCKET") || key.contains("SADDLE") || key.contains("NAME_TAG") || key.contains("PEARL") || key.contains("ELYTRA") || key.contains("TORCH") || key.contains("CHEST") || key.contains("ANVIL") || key.contains("HOPPER") || key.contains("FURNACE") || key.contains("SIGN") || key.contains("BED") || key.contains("SHULKER") || key.contains("ROD") || key.contains("DISC") || key.contains("MINECART") || (!cat.equalsIgnoreCase("BLOCKS") && !cat.equalsIgnoreCase("POTION") && !cat.equalsIgnoreCase("BOOK") && !key.contains("SWORD") && !key.contains("HELMET") && !key.contains("PICKAXE") && !key.contains("INGOT"));
        }
        return true;
    }

    public void openGUI(Player p, String menuName) {
        if (!menuName.contains("TEAM-EDIT-MEMBER") && !menuName.contains("TEAM-KICK-MEMBER")) {
            editingTeammate.remove(p.getUniqueId());
        }
        FileConfiguration config = menuConfig;
        String prefix = menuName + ".";
        if (rtpConfig != null && (rtpConfig.contains(menuName) || menuName.equals("RTP-MENU"))) {
            config = rtpConfig;
        } else if (shopConfig != null && (shopConfig.contains(menuName) || menuName.equals("CATEGORIES") || (menuName.endsWith("-MENU") && shopConfig.contains(menuName)))) {
            config = shopConfig;
        } else if (worthConfig != null && worthConfig.contains(menuName)) {
            config = worthConfig;
        } else if (menuConfig != null && (menuConfig.contains(menuName) || menuConfig.contains("TEAM-MENUS." + menuName) || menuConfig.contains("LEADERBOARDS-MENU." + menuName))) {
            config = menuConfig;
            if (!menuConfig.contains(menuName)) {
                if (menuConfig.contains("TEAM-MENUS." + menuName)) {
                    menuName = "TEAM-MENUS." + menuName;
                    prefix = menuName + ".";
                } else if (menuConfig.contains("LEADERBOARDS-MENU." + menuName)) {
                    menuName = "LEADERBOARDS-MENU." + menuName;
                    prefix = menuName + ".";
                }
            }
        }

        String titleStr;
        if (menuName.equals("PROGRESS-MENU") || menuName.equals("PROGRESS-ITEMS-MENU")) {
            String cat = viewingProgressCategory.getOrDefault(p.getUniqueId(), "CROPS");
            titleStr = config.getString("PROGRESS-MENU.TITLE." + cat, "&8" + cat + " PROGRESS");
        } else {
            titleStr = config.getString(prefix + "TITLE", config.getString(prefix + "MENU-TITLE", "&8" + menuName));
            if (titleStr != null && titleStr.startsWith("MemorySection[")) {
                titleStr = "&8" + menuName;
            }
        }
        PlayerProfile initialProf = getProfile(p);
        if (menuName.equals("STATS-MENU") && viewingStatsProfile.containsKey(p.getUniqueId())) {
            OfflinePlayer top = Bukkit.getOfflinePlayer(viewingStatsProfile.get(p.getUniqueId()));
            if (top != null) initialProf = getProfile(top);
        } else if (menuName.equals("PLAYERMANAGER-MENU") && viewingPlayerManagerProfile.containsKey(p.getUniqueId())) {
            UUID targetUuid = viewingPlayerManagerProfile.get(p.getUniqueId());
            if (targetUuid != null) {
                PlayerProfile tp = profiles.get(targetUuid);
                if (tp == null) {
                    OfflinePlayer top = Bukkit.getOfflinePlayer(targetUuid);
                    if (top != null) tp = getProfile(top);
                }
                if (tp != null) initialProf = tp;
            }
        }
        final PlayerProfile prof = initialProf;
        String title = color(replaceGUIPlaceholders(titleStr, p, prof, "", ""));
        int size = config.getInt(prefix + "SIZE", config.getInt(prefix + "MENU-SIZE", menuName.equals("HOME-MENU") ? 36 : 54));
        if (size % 9 != 0 || size < 9 || size > 54) size = (menuName.equals("HOME-MENU") ? 36 : 54);
        Inventory inv = Bukkit.createInventory(new GUIHolder(menuName), size, title);

        // 1. Load Universal YAML Buttons if present
        if (config.contains(menuName)) {
            ConfigurationSection rootSec = config.getConfigurationSection(menuName);
            if (rootSec != null) {
                String[] subSecs = {"BUTTONS", "AREAS", "CATEGORIES", "TYPE-BUTTON", "TEAM_HOME", "TELEPORT", "CREATE"};
                for (String sub : subSecs) {
                    if (rootSec.contains(sub)) {
                        loadSectionItems(inv, rootSec.getConfigurationSection(sub), p, prof, config);
                    }
                }
                for (String key : rootSec.getKeys(false)) {
                    if (key.equalsIgnoreCase("BUTTONS") || key.equalsIgnoreCase("AREAS") || key.equalsIgnoreCase("CATEGORIES") || key.equalsIgnoreCase("TYPE-BUTTON") || key.equalsIgnoreCase("TEAM_HOME") || key.equalsIgnoreCase("TELEPORT") || key.equalsIgnoreCase("CREATE") || key.equalsIgnoreCase("TYPE-MENU") || key.equalsIgnoreCase("TYPE-NAMES")) continue;
                    if (rootSec.isConfigurationSection(key)) {
                        ConfigurationSection itemSec = rootSec.getConfigurationSection(key);
                        if (itemSec != null) {
                            if (menuName.equals("BOUNTIES-MENU") && key.equalsIgnoreCase("BOUNTY-BUTTON")) continue;
                            if (itemSec.contains("SLOT") || itemSec.contains("MATERIAL")) {
                                loadSingleItem(inv, key, itemSec, p, prof, config);
                            } else {
                                loadSectionItems(inv, itemSec, p, prof, config);
                            }
                        }
                    }
                }
            }
        }

        // 2. Custom Dynamic Overrides
        if (menuName.equals("BOUNTIES-MENU")) {
            String query = activeMenuSearch.get(p.getUniqueId());
            List<Map.Entry<UUID, Double>> bountyList = new ArrayList<>();
            for (Map.Entry<UUID, Double> entry : bounties.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                    String pName = op.getName() != null ? op.getName() : "Desconhecido";
                    if (query != null && !query.isEmpty() && !pName.toLowerCase().contains(query)) continue;
                    bountyList.add(entry);
                }
            }
            String sortMode = viewingBountySort.getOrDefault(p.getUniqueId(), "Maior valor");
            bountyList.sort((e1, e2) -> {
                if (sortMode.equalsIgnoreCase("Maior valor")) {
                    return Double.compare(e2.getValue(), e1.getValue());
                } else if (sortMode.equalsIgnoreCase("Menor valor")) {
                    return Double.compare(e1.getValue(), e2.getValue());
                } else if (sortMode.equalsIgnoreCase("Z - A")) {
                    String n1 = Bukkit.getOfflinePlayer(e1.getKey()).getName();
                    String n2 = Bukkit.getOfflinePlayer(e2.getKey()).getName();
                    return (n2 != null ? n2 : "").compareToIgnoreCase(n1 != null ? n1 : "");
                } else { // A - Z
                    String n1 = Bukkit.getOfflinePlayer(e1.getKey()).getName();
                    String n2 = Bukkit.getOfflinePlayer(e2.getKey()).getName();
                    return (n1 != null ? n1 : "").compareToIgnoreCase(n2 != null ? n2 : "");
                }
            });

            int page = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
            int maxPerPage = 45;
            int startIdx = page * maxPerPage;
            int endIdx = Math.min(bountyList.size(), startIdx + maxPerPage);
            int slot = 0;
            ConfigurationSection bountyBtnSec = config.getConfigurationSection("BOUNTIES-MENU.BOUNTY-BUTTON");
            for (int i = startIdx; i < endIdx; i++) {
                Map.Entry<UUID, Double> entry = bountyList.get(i);
                OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                String pName = op.getName() != null ? op.getName() : "Desconhecido";
                String rewardStr = formatAbbreviated(entry.getValue());
                ItemStack bMat;
                String bTitle;
                List<String> bLore = new ArrayList<>();
                if (bountyBtnSec != null) {
                    String bMatName = bountyBtnSec.getString("MATERIAL", "PLAYER_HEAD");
                    bMat = resolveItem(bMatName);
                    if (bMat == null) bMat = new ItemStack(Material.PLAYER_HEAD);
                    bTitle = bountyBtnSec.getString("NAME", bountyBtnSec.getString("DISPLAY-NAME", "&a" + pName)).replace("{player}", pName).replace("${price}", "$" + rewardStr).replace("{price}", rewardStr);
                    for (String l : bountyBtnSec.getStringList("LORE")) {
                        bLore.add(color(l.replace("{player}", pName).replace("${price}", "$" + rewardStr).replace("{price}", rewardStr)));
                    }
                } else {
                    bMat = new ItemStack(Material.PLAYER_HEAD);
                    bTitle = "&a" + pName;
                    bLore.add(color("&fBounty: &7$" + rewardStr));
                }
                ItemMeta meta = bMat.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                    setPlayerHead(((org.bukkit.inventory.meta.SkullMeta) meta), op);
                    bMat.setItemMeta(meta);
                }
                addItem(inv, slot++, bMat, bTitle, bLore.toArray(new String[0]));
            }
            if (page > 0) {
                addItem(inv, 45, Material.ARROW, "&cVoltar", "&fClique para voltar à página");
            }
            if (bountyList.size() > (page + 1) * 45) {
                addItem(inv, 53, Material.ARROW, "&aPróximo", "&fClique para ir para a próxima página");
            }
        } else if (menuName.equals("TEAM-MENUS.TEAM")) {
            if (prof.teamName.isEmpty()) {
                p.sendMessage(getMsg("TEAM.NO-TEAM"));
                return;
            }
            Team team = teams.get(prof.teamName.toLowerCase());
            if (team != null) {
                int slot = 0;
                ConfigurationSection pbSec = config.getConfigurationSection("TEAM-MENUS.TEAM.PLAYER-BUTTON");
                String onlineSym = pbSec != null ? pbSec.getString("ONLINE-SYMBOL", "&a■") : "&a■";
                String offlineSym = pbSec != null ? pbSec.getString("OFFLINE-SYMBOL", "&4■") : "&4■";
                String pLore = pbSec != null ? pbSec.getString("LORE", "&fClick to edit") : "&fClick to edit";
                String filter = viewingTeamFilter.getOrDefault(p.getUniqueId(), "Todos");
                for (UUID mUUID : team.members) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(mUUID);
                    boolean isOnline = op.isOnline();
                    if (filter.equalsIgnoreCase("Membros online") && !isOnline) continue;
                    if (filter.equalsIgnoreCase("Membros offline") && isOnline) continue;
                    String sym = isOnline ? onlineSym : offlineSym;
                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                    ItemMeta meta = skull.getItemMeta();
                    if (meta instanceof org.bukkit.inventory.meta.SkullMeta && op != null) {
                        setPlayerHead(((org.bukkit.inventory.meta.SkullMeta) meta), op);
                        skull.setItemMeta(meta);
                    }
                    addItem(inv, slot++, skull, sym + " &r" + (op.getName() != null ? op.getName() : "Desconhecido"), pLore);
                }
            }
        } else if (menuName.equals("LEADERBOARDS-MENU.TYPE-MENU") || menuName.equals("TYPE-MENU")) {
            String typeKey = viewingLeaderboardType.getOrDefault(p.getUniqueId(), "money");
            String typeName = menuConfig.getString("LEADERBOARDS-MENU.TYPE-NAMES." + typeKey, typeKey);
            ConfigurationSection btnSec = menuConfig.getConfigurationSection("LEADERBOARDS-MENU.TYPE-MENU.BUTTON");
            String matStr = btnSec != null ? btnSec.getString("MATERIAL", "PLAYER_HEAD") : "PLAYER_HEAD";
            ItemStack mat = resolveItem(matStr);
            if (mat == null) mat = new ItemStack(Material.PLAYER_HEAD);
            String nameFormat = btnSec != null ? btnSec.getString("DISPLAY-NAME", "&#6BF18D{player}") : "&#6BF18D{player}";
            String loreFormat = btnSec != null ? btnSec.getString("LORE", "&f{type}: &7{value} &#6BF18D(#{position})") : "&f{type}: &7{value} &#6BF18D(#{position})";

            String query = activeMenuSearch.get(p.getUniqueId());
            List<PlayerProfile> sorted = new ArrayList<>();
            for (PlayerProfile pProfile : profiles.values()) {
                if (query != null && !query.isEmpty()) {
                    if (!pProfile.username.toLowerCase().contains(query)) continue;
                }
                sorted.add(pProfile);
            }
            boolean asc = viewingLeaderboardAscending.getOrDefault(p.getUniqueId(), false);
            sorted.sort((o1, o2) -> {
                double v1 = getStatValue(o1, typeKey);
                double v2 = getStatValue(o2, typeKey);
                return asc ? Double.compare(v1, v2) : Double.compare(v2, v1);
            });

            int page = viewingLeaderboardPage.getOrDefault(p.getUniqueId(), 0);
            if (page * 45 >= sorted.size()) page = 0;
            int start = page * 45;
            int end = Math.min(start + 45, sorted.size());
            int slot = 0;
            for (int i = start; i < end; i++) {
                PlayerProfile sp = sorted.get(i);
                OfflinePlayer op = Bukkit.getOfflinePlayer(sp.uuid);
                if (op.getName() == null && sp.username != null) {
                    op = Bukkit.getOfflinePlayer(sp.username);
                }
                String pName = (op.getName() != null) ? op.getName() : (sp.username != null ? sp.username : "Desconhecido");
                String valStr = formatStatValue(sp, typeKey);
                String iName = color(nameFormat.replace("{player}", pName).replace("{type}", typeName).replace("{value}", valStr).replace("{position}", String.valueOf(i + 1)));
                String iLore = color(loreFormat.replace("{player}", pName).replace("{type}", typeName).replace("{value}", valStr).replace("{position}", String.valueOf(i + 1)));
                ItemStack skull = mat.clone();
                ItemMeta smeta = skull.getItemMeta();
                if (smeta instanceof org.bukkit.inventory.meta.SkullMeta && op != null) {
                    setPlayerHead(((org.bukkit.inventory.meta.SkullMeta) smeta), op);
                    skull.setItemMeta(smeta);
                }
                addItem(inv, slot++, skull, iName, iLore);
            }

            addItem(inv, 45, Material.RED_STAINED_GLASS_PANE, color("&cᴠᴏʟᴛᴀʀ"), color("&fClique para voltar"));
            if (page > 0) {
                addItem(inv, 47, Material.ARROW, color("&aᴘáɢɪɴᴀ ᴀɴᴛᴇʀɪᴏʀ"), color("&fClique para ver a página anterior"));
            }

            ConfigurationSection filterSec = menuConfig.getConfigurationSection("LEADERBOARDS-MENU.TYPE-MENU.FILTER-BUTTON");
            ItemStack filterMat = resolveItem(filterSec != null ? filterSec.getString("MATERIAL", "HOPPER") : "HOPPER");
            if (filterMat == null) filterMat = new ItemStack(Material.HOPPER);
            String filterName = color(filterSec != null ? filterSec.getString("NAME", "&#6BF18Dꜰɪʟᴛʀᴏ") : "&#6BF18Dꜰɪʟᴛʀᴏ");
            String selPrefix = menuConfig.getString("GLOBAL.SORTS.SELECTED-PREFIX", "&f");
            String unselPrefix = menuConfig.getString("GLOBAL.SORTS.UNSELECTED-PREFIX", "&7");
            String symbol = menuConfig.getString("GLOBAL.SORTS.SYMBOL", "▪ ");
            List<String> filterLore = new ArrayList<>();
            List<String> rawFilterLore = filterSec != null ? filterSec.getStringList("LORE") : Arrays.asList("&f- Clique para filtrar", "", "Top jogadores", "Piores jogadores");
            for (String l : rawFilterLore) {
                String cleanL = ChatColor.stripColor(color(l)).trim();
                if (cleanL.equalsIgnoreCase("Top jogadores")) {
                    if (!asc) filterLore.add(color(selPrefix + symbol + cleanL));
                    else filterLore.add(color(unselPrefix + symbol + cleanL));
                } else if (cleanL.equalsIgnoreCase("Piores jogadores")) {
                    if (asc) filterLore.add(color(selPrefix + symbol + cleanL));
                    else filterLore.add(color(unselPrefix + symbol + cleanL));
                } else {
                    String colored = color(l);
                    if (!colored.isEmpty() && !colored.startsWith("§")) colored = "§f" + colored;
                    filterLore.add(colored);
                }
            }
            addItem(inv, 48, filterMat, filterName, filterLore.toArray(new String[0]));

            ConfigurationSection refreshSec = menuConfig.getConfigurationSection("LEADERBOARDS-MENU.TYPE-MENU.REFRESH-BUTTON");
            ItemStack refreshMat = resolveItem(refreshSec != null ? refreshSec.getString("MATERIAL", "ANVIL") : "ANVIL");
            if (refreshMat == null) refreshMat = new ItemStack(Material.ANVIL);
            String rawRefreshName = refreshSec != null ? refreshSec.getString("NAME", "&#6BF18D{ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ}") : "&#6BF18D{ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ}";
            String refreshName = color(rawRefreshName.replace("{ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ}", typeName).replace("{leaderboard}", typeName));
            List<String> refreshLore = new ArrayList<>();
            List<String> rawRefreshLore = refreshSec != null ? refreshSec.getStringList("LORE") : Collections.singletonList("&f- Clique para atualizar");
            for (String l : rawRefreshLore) refreshLore.add(color(l));
            addItem(inv, 49, refreshMat, refreshName, refreshLore.toArray(new String[0]));

            ConfigurationSection searchSec = menuConfig.getConfigurationSection("LEADERBOARDS-MENU.TYPE-MENU.SEARCH-BUTTON");
            ItemStack searchMat = resolveItem(searchSec != null ? searchSec.getString("MATERIAL", "OAK_SIGN") : "OAK_SIGN");
            if (searchMat == null) searchMat = new ItemStack(Material.OAK_SIGN);
            String searchName = color(searchSec != null ? searchSec.getString("NAME", "&#6BF18Dᴘᴇѕǫᴜɪѕᴀʀ") : "&#6BF18Dᴘᴇѕǫᴜɪѕᴀʀ");
            List<String> searchLore = new ArrayList<>();
            List<String> rawSearchLore = searchSec != null ? searchSec.getStringList("LORE") : Arrays.asList("&f- Clique para pesquisar", "&fProcure por jogadores");
            for (String l : rawSearchLore) searchLore.add(color(l));
            addItem(inv, 50, searchMat, searchName, searchLore.toArray(new String[0]));

            if (end < sorted.size()) {
                addItem(inv, 51, Material.ARROW, color("&aᴘʀóxɪᴍᴀ ᴘáɢɪɴᴀ"), color("&fClique para ver a próxima página"));
            }
        } else if (menuName.equals("PROGRESS-MENU")) {
            for (int i = 0; i < inv.getSize(); i++) {
                addItem(inv, i, Material.GRAY_STAINED_GLASS_PANE, " ");
            }
            String cat = viewingProgressCategory.getOrDefault(p.getUniqueId(), "CROPS");
            ConfigurationSection typeSec = menuConfig.getConfigurationSection("PROGRESS-MENU.TYPE-BUTTON");
            if (typeSec != null) {
                String matName = typeSec.getString("MATERIAL." + cat, "WHEAT");
                ItemStack mat = resolveItem(matName);
                if (mat == null) mat = new ItemStack(Material.WHEAT);
                String tName = typeSec.getString("TITLE." + cat, "&a" + cat);
                List<String> loreList = typeSec.getStringList("LORE." + cat);
                List<String> coloredLore = new ArrayList<>();
                for (String l : loreList) coloredLore.add(color(l));
                coloredLore.add(" ");
                coloredLore.add(color("&7Clique para ver os itens que aumentam o progresso!"));
                addItem(inv, 1, mat, tName, coloredLore.toArray(new String[0]));
            }
            prof.addSellProgress(cat, 0.0);
            int currentLevel = prof.sellMultipliers.getOrDefault(cat, 1);
            double currentEarned = prof.categoryProgress.getOrDefault(cat, 0.0);

            int[] slots = {10, 19, 28, 37, 38, 39, 30, 21, 12, 13, 14, 23, 32, 41, 42, 43, 34, 25, 16};
            for (int i = 0; i < slots.length; i++) {
                int levelNum = i + 1;
                double goal = getCategoryGoal(levelNum);
                double mult = 1.0 + (levelNum * 0.25);
                boolean completed = currentLevel > levelNum;
                boolean working = currentLevel == levelNum;

                ConfigurationSection btnSec = completed ? menuConfig.getConfigurationSection("PROGRESS-MENU.COMPLETED-BUTTON") : (working ? menuConfig.getConfigurationSection("PROGRESS-MENU.WORKING-BUTTON") : menuConfig.getConfigurationSection("PROGRESS-MENU.LOCKED-BUTTON"));
                if (btnSec != null) {
                    Material defaultMat = completed ? Material.LIME_STAINED_GLASS_PANE : (working ? Material.YELLOW_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE);
                    ItemStack mat = resolveItem(btnSec.getString("MATERIAL", defaultMat.name()));
                    if (mat == null) mat = new ItemStack(defaultMat);
                    String defaultTitle = completed ? "&aᴄᴏᴍᴘʟᴇᴛᴇᴅ" : (working ? "&eᴡᴏʀᴋɪɴɢ" : "&7ʙʟᴏǫᴜᴇᴀᴅᴏ");
                    String btnTitle = color(btnSec.getString("TITLE", defaultTitle) + " &7(#" + levelNum + ")");
                    int percentage = completed ? 100 : (working ? (int) Math.min(100, Math.max(0, (currentEarned / goal) * 100)) : 0);
                    StringBuilder bar = new StringBuilder();
                    for (int b = 0; b < 10; b++) {
                        bar.append(b < (percentage / 10) ? "&a■" : "&7■");
                    }
                    List<String> lList = new ArrayList<>();
                    for (String l : btnSec.getStringList("LORE")) {
                        lList.add(color(l.replace("{porcentage_level}", bar.toString())
                                         .replace("{next_multiplier}", String.format("%.2fx", mult))
                                         .replace("{porcentage}", String.valueOf(percentage))
                                         .replace("{current_earned}", "$" + formatAbbreviated(completed ? goal : (working ? currentEarned : 0.0)))
                                         .replace("{next_goal}", "$" + formatAbbreviated(goal))));
                    }
                    addItem(inv, slots[i], mat, btnTitle, lList.toArray(new String[0]));
                }
            }
            addItem(inv, 45, Material.RED_STAINED_GLASS_PANE, "&cᴠᴏʟᴛᴀʀ", "&fClique para voltar á página");
        } else if (menuName.equals("PROGRESS-ITEMS-MENU")) {
            String cat = viewingProgressCategory.getOrDefault(p.getUniqueId(), "CROPS");
            String worthCat = worthConfig != null && worthConfig.contains("TYPE." + cat) ? cat : (worthConfig != null && worthConfig.contains("TYPE." + cat.replace("POTIONS", "POTION")) ? cat.replace("POTIONS", "POTION") : "CROPS");
            if (worthConfig != null && worthConfig.contains("TYPE." + worthCat)) {
                ConfigurationSection catSec = worthConfig.getConfigurationSection("TYPE." + worthCat);
                if (catSec != null) {
                    int page = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
                    List<String> keys = new ArrayList<>(catSec.getKeys(false));
                    int slot = 0;
                    double mult = prof.getMultiplier(cat);
                    for (int i = page * 45; i < Math.min(keys.size(), (page + 1) * 45); i++) {
                        String matKey = keys.get(i);
                        ItemStack dispItem = resolveItem(matKey);
                        if (dispItem != null) {
                            double price = catSec.getDouble(matKey);
                            String lore1 = "&fPreço: &a$" + String.format("%.2f", price);
                            addItem(inv, slot++, dispItem, null, lore1);
                        }
                    }
                    if (page > 0) {
                        addItem(inv, 48, Material.ARROW, "&cᴠᴏʟᴛᴀʀ", "&fClique para voltar á página");
                    }
                    if (keys.size() > (page + 1) * 45) {
                        addItem(inv, 50, Material.ARROW, "&aᴘʀóхɪᴍᴏ", "&fClique para ir para a próxima página");
                    }
                }
            }
            addItem(inv, 45, Material.RED_STAINED_GLASS_PANE, "&cᴠᴏʟᴛᴀʀ", "&fClique para voltar á página");
        } else if (menuName.equals("SELL-HISTORY-MENU")) {
            ConfigurationSection menuSec = menuConfig.getConfigurationSection("SELL-HISTORY-MENU");
            if (menuSec != null) {
                if (menuSec.contains("BUTTONS.FILTER-BUTTON")) {
                    loadSingleItem(inv, "FILTER-BUTTON", menuSec.getConfigurationSection("BUTTONS.FILTER-BUTTON"), p, prof, menuConfig);
                }
                if (menuSec.contains("BUTTONS.SORT-BUTTON")) {
                    loadSingleItem(inv, "SORT-BUTTON", menuSec.getConfigurationSection("BUTTONS.SORT-BUTTON"), p, prof, menuConfig);
                }
                if (menuSec.contains("BUTTONS.REFRESH-BUTTON")) {
                    loadSingleItem(inv, "REFRESH-BUTTON", menuSec.getConfigurationSection("BUTTONS.REFRESH-BUTTON"), p, prof, menuConfig);
                }
            }
            ConfigurationSection itemSec = menuSec != null ? menuSec.getConfigurationSection("BUTTONS.MATERIAL-ITEM") : null;
            List<String> defLore = Arrays.asList("&fTotal price: &a${price}", "&fTotal amount: {amount}");
            List<String> loreTemplate = itemSec != null && itemSec.contains("LORE") ? itemSec.getStringList("LORE") : defLore;

            String currentFilter = viewingHistoryFilter.getOrDefault(p.getUniqueId(), "Todos");
            Set<String> allMats = new HashSet<>();
            for (Map.Entry<String, Integer> entry : prof.soldAmountHistory.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    String matKey = entry.getKey();
                    if (currentFilter.equalsIgnoreCase("Todos")) {
                        allMats.add(matKey);
                    } else {
                        String foundCat = "";
                        if (worthConfig != null && worthConfig.contains("TYPE")) {
                            for (String cat : worthConfig.getConfigurationSection("TYPE").getKeys(false)) {
                                if (worthConfig.contains("TYPE." + cat + "." + matKey)) {
                                    foundCat = cat;
                                    break;
                                }
                            }
                        }
                        if (matchesWorthFilter(matKey, foundCat, currentFilter)) {
                            allMats.add(matKey);
                        }
                    }
                }
            }
            String sortMode = viewingHistorySort.getOrDefault(p.getUniqueId(), "A - Z");
            List<String> sortedMats = new ArrayList<>(allMats);
            sortedMats.sort((m1, m2) -> {
                if (sortMode.equalsIgnoreCase("Mais vendido")) {
                    int a1 = prof.soldAmountHistory.getOrDefault(m1, 0);
                    int a2 = prof.soldAmountHistory.getOrDefault(m2, 0);
                    if (a1 != a2) return Integer.compare(a2, a1);
                    return Double.compare(prof.soldPriceHistory.getOrDefault(m2, 0.0), prof.soldPriceHistory.getOrDefault(m1, 0.0));
                } else if (sortMode.equalsIgnoreCase("Menos vendido")) {
                    int a1 = prof.soldAmountHistory.getOrDefault(m1, 0);
                    int a2 = prof.soldAmountHistory.getOrDefault(m2, 0);
                    if (a1 != a2) return Integer.compare(a1, a2);
                    return Double.compare(prof.soldPriceHistory.getOrDefault(m1, 0.0), prof.soldPriceHistory.getOrDefault(m2, 0.0));
                } else if (sortMode.equalsIgnoreCase("Z - A")) {
                    return m2.compareTo(m1);
                } else { // "A - Z"
                    return m1.compareTo(m2);
                }
            });

            int page = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
            int slot = 0;
            for (int i = page * 45; i < sortedMats.size(); i++) {
                if (slot >= 45) break;
                String matName = sortedMats.get(i);
                ItemStack dispItem = resolveItem(matName);
                if (dispItem == null) continue;
                double price = prof.soldPriceHistory.getOrDefault(matName, 0.0);
                int amount = prof.soldAmountHistory.getOrDefault(matName, 0);
                String priceStr = String.format("%.2f", price);

                List<String> coloredLore = new ArrayList<>();
                for (String l : loreTemplate) {
                    coloredLore.add(color(l.replace("${price}", "$" + priceStr).replace("{price}", "$" + priceStr).replace("{amount}", String.valueOf(amount))));
                }
                addItem(inv, slot++, dispItem, null, coloredLore.toArray(new String[0]));
            }
            if (page > 0) {
                addItem(inv, 45, Material.ARROW, "&cᴠᴏʟᴛᴀʀ", "&fClique para voltar á página");
            }
            if (sortedMats.size() > (page + 1) * 45) {
                addItem(inv, 53, Material.ARROW, "&aᴘʀóхɪᴍᴏ", "&fClique para ir para a próxima página");
            }
        } else if (menuName.equals("WORTH-MENU")) {
            String currentFilter = viewingWorthFilter.getOrDefault(p.getUniqueId(), "Todos");
            List<Map.Entry<String, Double>> worthItems = new ArrayList<>();
            if (worthConfig != null && worthConfig.contains("TYPE")) {
                ConfigurationSection typeSec = worthConfig.getConfigurationSection("TYPE");
                if (typeSec != null) {
                    for (String cat : typeSec.getKeys(false)) {
                        ConfigurationSection catSec = typeSec.getConfigurationSection(cat);
                        if (catSec != null) {
                            for (String matKey : catSec.getKeys(false)) {
                                if (resolveItem(matKey) != null && matchesWorthFilter(matKey, cat, currentFilter)) {
                                    worthItems.add(new AbstractMap.SimpleEntry<>(matKey, catSec.getDouble(matKey)));
                                }
                            }
                        }
                    }
                }
            }
            String sortMode = viewingWorthSort.getOrDefault(p.getUniqueId(), "A - Z");
            if (sortMode.equalsIgnoreCase("Maior preço")) {
                worthItems.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
            } else if (sortMode.equalsIgnoreCase("Menor preço")) {
                worthItems.sort((e1, e2) -> Double.compare(e1.getValue(), e2.getValue()));
            } else if (sortMode.equalsIgnoreCase("Z - A")) {
                worthItems.sort((e1, e2) -> e2.getKey().compareTo(e1.getKey()));
            } else { // "A - Z"
                worthItems.sort((e1, e2) -> e1.getKey().compareTo(e2.getKey()));
            }
            String formatTemplate = menuConfig.getString("WORTH-MENU.FORMAT", "&fPreço: &a${price}");
            int page = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
            int slot = 0;
            for (int i = page * 45; i < Math.min(worthItems.size(), (page + 1) * 45); i++) {
                Map.Entry<String, Double> entry = worthItems.get(i);
                String priceStr = String.format("%.2f", entry.getValue());
                String lore = color(formatTemplate.replace("${price}", "$" + priceStr).replace("{price}", "$" + priceStr));
                ItemStack dispItem = resolveItem(entry.getKey());
                if (dispItem != null) {
                    addItem(inv, slot++, dispItem, null, lore);
                }
            }
            if (page > 0) {
                addItem(inv, 45, Material.ARROW, "&cᴠᴏʟᴛᴀʀ", "&fClique para voltar á página");
            }
            if (worthItems.size() > (page + 1) * 45) {
                addItem(inv, 53, Material.ARROW, "&aᴘʀóхɪᴍᴏ", "&fClique para ir para a próxima página");
            }
        } else if (menuName.equals("PURCHASE-SHOP-MENU")) {
            ShopPurchase pur = activePurchases.get(p.getUniqueId());
            if (pur != null) {
                double totalPrice = pur.pricePerUnit * pur.quantity;
                String totalStr = String.format("%.2f", totalPrice);
                ConfigurationSection mainSec = menuConfig.getConfigurationSection("PURCHASE-SHOP-MENU.BUTTONS.MAIN");
                if (mainSec != null) {
                    String loreTemplate = pur.currency.equalsIgnoreCase("SHARDS") || pur.currency.equalsIgnoreCase("SHARD") ?
                            mainSec.getString("LORE.SHARD", "&fʙᴜʏ ᴘʀɪᴄᴇ: &5${price}x &lShards") :
                            mainSec.getString("LORE.MONEY", "&fʙᴜʏ ᴘʀɪᴄᴇ: &a${price}");
                    String coloredLore = color(loreTemplate.replace("${price}", "$" + totalStr).replace("{price}", totalStr));
                    ItemStack icon = pur.itemStack != null ? pur.itemStack.clone() : new ItemStack(pur.material);
                    icon.setAmount(pur.quantity);
                    addItem(inv, mainSec.getInt("SLOT", 13), icon, null, coloredLore);
                }
                // Apply RESTRICTIONS - hide quantity buttons if configured
                ConfigurationSection resSec = menuConfig.getConfigurationSection("PURCHASE-SHOP-MENU.RESTRICTIONS." + pur.material.name());
                if (resSec == null) resSec = menuConfig.getConfigurationSection("PURCHASE-SHOP-MENU.RESTRICTIONS.DEFAULT");
                if (resSec != null && resSec.getBoolean("HIDE_QUANTITY_BUTTONS", false)) {
                    int[] quantitySlots = {9, 10, 11, 15, 16, 17};
                    for (int qs : quantitySlots) {
                        inv.setItem(qs, null);
                    }
                }
            }
        } else if (menuName.equals("PAY-CONFIRM-MENU") || menuName.equals("TPA-CONFIRM-MENU") || menuName.equals("BOUNTY-CONFIRM-MENU") || menuName.equals("CONFIRM-MENU")) {
            PendingAction pa = pendingActions.get(p.getUniqueId());
            if (pa != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(pa.targetUUID);
                String tName = op != null && op.getName() != null ? op.getName() : "Desconhecido";
                String amtStr = formatAbbreviated(pa.amount);
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.getItemMeta() != null) {
                        ItemMeta m = item.getItemMeta();
                        if (m instanceof org.bukkit.inventory.meta.SkullMeta && op != null) {
                            setPlayerHead(((org.bukkit.inventory.meta.SkullMeta) m), op);
                        }
                        if (m.getDisplayName() != null) {
                            m.setDisplayName(m.getDisplayName().replace("{player}", tName).replace("{amount}", amtStr).replace("${amount}", "$" + amtStr));
                        }
                        List<String> lore = m.getLore();
                        if (lore != null) {
                            List<String> nLore = new ArrayList<>();
                            for (String l : lore) nLore.add(l.replace("{player}", tName).replace("{amount}", amtStr).replace("${amount}", "$" + amtStr));
                            m.setLore(nLore);
                        }
                        item.setItemMeta(m);
                    }
                }
            }
        }

        if (shopConfig != null && menuName.endsWith("-MENU") && shopConfig.contains(menuName)) {
            String bName = menuConfig.getString("GLOBAL.PAGE-MENU.BACK-BUTTON", "&aʙᴀᴄᴋ");
            List<String> bLore = menuConfig.getStringList("GLOBAL.PAGE-MENU.BACK-LORE");
            if (bLore.isEmpty()) bLore = Collections.singletonList("&fClick to return");
            List<String> coloredLore = new ArrayList<>();
            for (String l : bLore) coloredLore.add(color(l));
            int backSlot = inv.getSize() - 9;
            addItem(inv, backSlot, Material.RED_STAINED_GLASS_PANE, bName, coloredLore.toArray(new String[0]));
        }

        // 3. Fill Placeholders if requested
        if (config.getBoolean(prefix + "PLACEHOLDER", false)) {
            String matStr = config.getString(prefix + "PLACEHOLDER-MATERIAL", "BLACK_STAINED_GLASS_PANE");
            Material pMat = Material.matchMaterial(matStr);
            if (pMat == null) pMat = Material.BLACK_STAINED_GLASS_PANE;
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                    addItem(inv, i, pMat, " ");
                }
            }
        }

        p.openInventory(inv);
        playSound(p, "MENUS.BUTTON-CLICK");
    }

    public void openShopSearchResults(Player p, String query) {
        if (shopConfig == null) return;
        Inventory inv = Bukkit.createInventory(new GUIHolder("SHOP-SEARCH-RESULTS"), 54, color("&8Pesquisa Loja: &e" + query));
        int slot = 0;
        String lowerQuery = query.toLowerCase();
        for (String menuKey : shopConfig.getKeys(false)) {
            if (menuKey.endsWith("-MENU")) {
                ConfigurationSection shopSec = shopConfig.getConfigurationSection(menuKey);
                if (shopSec != null) {
                    for (String itemKey : shopSec.getKeys(false)) {
                        ConfigurationSection itemSec = shopSec.getConfigurationSection(itemKey);
                        if (itemSec != null && itemSec.contains("SLOT") && itemSec.contains("MATERIAL")) {
                            String matName = itemSec.getString("MATERIAL", "STONE");
                            String dispName = itemSec.getString("DISPLAY-NAME", "");
                            if (matName.toLowerCase().contains(lowerQuery) || dispName.toLowerCase().contains(lowerQuery)) {
                                ItemStack mat = resolveItem(matName);
                                if (mat == null) mat = new ItemStack(Material.STONE);
                                List<String> lore = new ArrayList<>();
                                for (String l : itemSec.getStringList("LORE")) {
                                    lore.add(color(l));
                                }
                                if (slot < 45) {
                                    addItem(inv, slot++, mat, null, lore.toArray(new String[0]));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (slot == 0) {
            addItem(inv, 22, Material.BARRIER, "&c&lNenhum item encontrado", "&7Busca: &e" + query);
        }
        String bName = menuConfig.getString("GLOBAL.PAGE-MENU.BACK-BUTTON", "&aʙᴀᴄᴋ");
        List<String> bLore = menuConfig.getStringList("GLOBAL.PAGE-MENU.BACK-LORE");
        if (bLore.isEmpty()) bLore = Collections.singletonList("&fClick to return");
        List<String> coloredLore = new ArrayList<>();
        for (String l : bLore) coloredLore.add(color(l));
        ItemStack backMat = resolveItem(menuConfig.getString("GLOBAL.PAGE-MENU.MATERIAL", "ARROW"));
        if (backMat == null) backMat = new ItemStack(Material.ARROW);
        addItem(inv, 49, backMat, bName, coloredLore.toArray(new String[0]));
        p.openInventory(inv);
        playSound(p, "MENUS.BUTTON-CLICK");
    }

    private void loadSectionItems(Inventory inv, ConfigurationSection sec, Player p, PlayerProfile prof, FileConfiguration config) {
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            if (sec.isConfigurationSection(key)) {
                ConfigurationSection child = sec.getConfigurationSection(key);
                if (child != null) {
                    // Check if this section has child sub-sections with SLOT (e.g. ADD has ADD_1, ADD_10)
                    boolean hasChildSlots = false;
                    for (String subKey : child.getKeys(false)) {
                        if (child.isConfigurationSection(subKey)) {
                            ConfigurationSection sub = child.getConfigurationSection(subKey);
                            if (sub != null && sub.contains("SLOT")) {
                                hasChildSlots = true;
                                break;
                            }
                        }
                    }
                    if (hasChildSlots) {
                        loadSectionItems(inv, child, p, prof, config);
                    } else if (child.contains("SLOT") || child.contains("MATERIAL")) {
                        loadSingleItem(inv, key, child, p, prof, config);
                    } else {
                        loadSectionItems(inv, child, p, prof, config);
                    }
                }
            }
        }
    }

    private String getHomeNameForSlot(PlayerProfile prof, int slotNum) {
        if (prof == null || prof.homes == null || prof.homes.isEmpty()) return null;
        // 1. Direct match: home name equals the slot number
        if (prof.homes.containsKey(String.valueOf(slotNum))) return String.valueOf(slotNum);
        if (prof.homes.containsKey("home" + slotNum)) return "home" + slotNum;

        // 2. Collect homes that don't directly map to any slot (1-5)
        List<String> unassigned = new ArrayList<>();
        for (String key : prof.homes.keySet()) {
            boolean directlyMapped = false;
            try {
                int num = Integer.parseInt(key);
                if (num >= 1 && num <= 5) directlyMapped = true;
            } catch (Exception ignored) {}
            if (!directlyMapped && key.matches("(?i)home[1-5]")) directlyMapped = true;
            if (!directlyMapped) unassigned.add(key);
        }

        // 3. Assign unassigned homes to empty slots sequentially
        int uIdx = 0;
        for (int s = 1; s <= 5; s++) {
            if (prof.homes.containsKey(String.valueOf(s)) || prof.homes.containsKey("home" + s)) continue;
            if (uIdx >= unassigned.size()) break;
            if (s == slotNum) return unassigned.get(uIdx);
            uIdx++;
        }

        return null;
    }

    private void loadSingleItem(Inventory inv, String key, ConfigurationSection itemSec, Player p, PlayerProfile prof, FileConfiguration config) {
        if (itemSec == null) return;
        if (itemSec.contains("ENABLED") && !itemSec.getBoolean("ENABLED")) return;

        int slot = itemSec.getInt("SLOT", -1);

        String matName = "PAPER";
        if (itemSec.isConfigurationSection("MATERIALS") || itemSec.isConfigurationSection("MATERIAL")) {
            ConfigurationSection mSec = itemSec.getConfigurationSection("MATERIALS");
            if (mSec == null) mSec = itemSec.getConfigurationSection("MATERIAL");
            matName = resolveConditionalValue(mSec, key, prof, p);
        } else {
            matName = itemSec.getString("MATERIAL", itemSec.getString("MATERIALS", null));
            if (matName == null && itemSec.getParent() != null) {
                matName = itemSec.getParent().getString("MATERIAL", itemSec.getParent().getString("MATERIALS", "PAPER"));
            } else if (matName == null) {
                matName = "PAPER";
            }
        }
        ItemStack item = null;
        if (itemSec.getCurrentPath() != null && itemSec.getCurrentPath().contains("PLAYERMANAGER-MENU") && key.startsWith("HOME-")) {
            int hNum = 1;
            try { hNum = Integer.parseInt(key.replace("HOME-", "")); } catch (Exception ignored) {}
            Player targetP = Bukkit.getPlayer(prof.uuid);
            boolean hasPerm = targetP != null ? hasHomePermission(targetP, hNum) : (hNum <= getConfig().getInt("SETTINGS.HOME-DEFAULT", 2));
            boolean hasHome = getHomeNameForSlot(prof, hNum) != null;
            if (!hasPerm) {
                matName = config.getString("PLAYERMANAGER-MENU.HOME-NO-PERMISSION-MATERIAL", "RED_BED");
            } else if (hasHome) {
                matName = config.getString("PLAYERMANAGER-MENU.HOME-USED-MATERIAL", "YELLOW_BED");
            } else {
                matName = config.getString("PLAYERMANAGER-MENU.HOME-NO-USED", "LIGHT_GRAY_BED");
            }
            item = resolveItem(matName);
        } else if (itemSec.getCurrentPath() != null && itemSec.getCurrentPath().contains("PLAYERMANAGER-MENU") && key.equalsIgnoreCase("TEAM-HOME")) {
            if (prof.teamName.isEmpty()) {
                matName = config.getString("PLAYERMANAGER-MENU.TEAM-NO-TEAM", "RED_BANNER");
            } else if (!teams.containsKey(prof.teamName.toLowerCase()) || teams.get(prof.teamName.toLowerCase()).home == null) {
                matName = config.getString("PLAYERMANAGER-MENU.TEAM-NO-HOME", "WHITE_BANNER");
            } else {
                matName = config.getString("PLAYERMANAGER-MENU.TEAM-HAS-HOME", "YELLOW_BANNER");
            }
            item = resolveItem(matName);
        } else if (key.startsWith("HOME-") && !key.equalsIgnoreCase("HOME-BUTTON")) {
            int hNum = 1;
            try { hNum = Integer.parseInt(key.replace("HOME-", "")); } catch (Exception ignored) {}
            boolean hasPerm = hasHomePermission(p, hNum);
            boolean hasHome = getHomeNameForSlot(prof, hNum) != null;
            if (!hasPerm) {
                if (itemSec.getParent() != null && "CREATE".equalsIgnoreCase(itemSec.getParent().getName())) {
                    matName = config.getString("HOME-MENU.CREATE-NO-PERMISSION-MATERIAL", "RED_DYE");
                } else {
                    matName = config.getString("HOME-MENU.TELEPORT-NO-PERMISSION-MATERIAL", "RED_BED");
                }
            } else if (itemSec.getParent() != null && "CREATE".equalsIgnoreCase(itemSec.getParent().getName())) {
                matName = config.getString("HOME-MENU." + (hasHome ? "CREATE-USED-MATERIAL" : "CREATE-NO-USED-MATERIAL"), "GRAY_DYE");
            } else {
                matName = config.getString("HOME-MENU." + (hasHome ? "TELEPORT-USED-MATERIAL" : "TELEPORT-NO-USED-MATERIAL"), "LIGHT_GRAY_BED");
            }
            item = resolveItem(matName);
        } else if (key.equalsIgnoreCase("HOME-BUTTON")) {
            boolean hasTeamHome = !prof.teamName.isEmpty() && teams.containsKey(prof.teamName.toLowerCase()) && teams.get(prof.teamName.toLowerCase()).home != null;
            if (hasTeamHome) {
                matName = itemSec.getString("HAS-HOME-MATERIAL", itemSec.getString("HAS_HOME_MATERIAL", "YELLOW_BANNER"));
            } else {
                matName = itemSec.getString("MATERIAL", "WHITE_BANNER");
            }
            item = resolveItem(matName);
        } else {
            item = resolveItem(matName);
        }
        if (item == null) item = new ItemStack(Material.PAPER);
        if (item.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) meta;
                String texture = itemSec.getString("TEXTURE", itemSec.getString("SKULL_OWNER", null));
                if (texture != null && !texture.isEmpty()) {
                    applySkullTexture(sm, texture);
                } else {
                    OfflinePlayer targetSkull = p;
                    if (itemSec.getCurrentPath() != null && itemSec.getCurrentPath().contains("TEAM-EDIT-MEMBER") && editingTeammate.containsKey(p.getUniqueId())) {
                        targetSkull = Bukkit.getOfflinePlayer(editingTeammate.get(p.getUniqueId()));
                    } else if (itemSec.getCurrentPath() != null && itemSec.getCurrentPath().contains("PLAYERMANAGER-MENU") && viewingPlayerManagerProfile.containsKey(p.getUniqueId())) {
                        targetSkull = Bukkit.getOfflinePlayer(viewingPlayerManagerProfile.get(p.getUniqueId()));
                    }
                    setPlayerHead(sm, targetSkull);
                }
                item.setItemMeta(sm);
            }
        }

        String name = "&a" + key;
        if (itemSec.isConfigurationSection("DISPLAY-NAME") || itemSec.isConfigurationSection("DISPLAY_NAME") || itemSec.isConfigurationSection("NAME") || itemSec.isConfigurationSection("TITLE")) {
            ConfigurationSection nSec = itemSec.getConfigurationSection("DISPLAY-NAME");
            if (nSec == null) nSec = itemSec.getConfigurationSection("DISPLAY_NAME");
            if (nSec == null) nSec = itemSec.getConfigurationSection("NAME");
            if (nSec == null) nSec = itemSec.getConfigurationSection("TITLE");
            name = resolveConditionalValue(nSec, key, prof, p);
        } else {
            name = itemSec.getString("DISPLAY-NAME", itemSec.getString("DISPLAY_NAME", itemSec.getString("NAME", itemSec.getString("TITLE", "&a" + key))));
        }

        List<String> loreList = new ArrayList<>();
        if (itemSec.isConfigurationSection("LORE")) {
            ConfigurationSection lSec = itemSec.getConfigurationSection("LORE");
            loreList.addAll(resolveConditionalLore(lSec, key, prof, p));
        } else if (itemSec.isList("LORE")) {
            loreList.addAll(itemSec.getStringList("LORE"));
        } else if (itemSec.isString("LORE")) {
            loreList.add(itemSec.getString("LORE"));
        }

        if (key.equalsIgnoreCase("HOME-BUTTON")) {
            boolean hasTeamHome = !prof.teamName.isEmpty() && teams.containsKey(prof.teamName.toLowerCase()) && teams.get(prof.teamName.toLowerCase()).home != null;
            if (hasTeamHome && itemSec.contains("HOME-LORE")) {
                loreList.clear();
                loreList.add(itemSec.getString("HOME-LORE"));
            } else if (!hasTeamHome && itemSec.contains("NO-HOME-LORE")) {
                loreList.clear();
                loreList.add(itemSec.getString("NO-HOME-LORE"));
            }
        }

        String priceStr = itemSec.contains("PRICE-PER-UNIT") ? String.valueOf(itemSec.getDouble("PRICE-PER-UNIT")) : itemSec.getString("PRICE", "0");

        boolean isSortBtn = key.equalsIgnoreCase("SORT") || key.equalsIgnoreCase("SORT-BUTTON") || itemSec.contains("SELECTED-PREFIX");
        boolean isFilterBtn = key.equalsIgnoreCase("FILTER") || key.equalsIgnoreCase("FILTER-BUTTON") || key.equalsIgnoreCase("FILTRO");
        String holderId = (inv.getHolder() instanceof GUIHolder) ? ((GUIHolder) inv.getHolder()).getMenuId() : "";
        String currentSort = holderId.equals("WORTH-MENU") ? viewingWorthSort.getOrDefault(p.getUniqueId(), "A - Z") : (holderId.equals("BOUNTIES-MENU") ? viewingBountySort.getOrDefault(p.getUniqueId(), "Maior valor") : (holderId.equals("TEAM-MENUS.TEAM") ? viewingTeamFilter.getOrDefault(p.getUniqueId(), "Todos") : viewingHistorySort.getOrDefault(p.getUniqueId(), "A - Z")));
        String currentFilter = holderId.equals("WORTH-MENU") ? viewingWorthFilter.getOrDefault(p.getUniqueId(), "Todos") : viewingHistoryFilter.getOrDefault(p.getUniqueId(), "Todos");
        String selPrefix = itemSec.getString("SELECTED-PREFIX", config.getString("GLOBAL.SORTS.SELECTED-PREFIX", "&#FFF000"));
        String unselPrefix = itemSec.getString("UNSELECTED-PREFIX", config.getString("GLOBAL.SORTS.UNSELECTED-PREFIX", "&f"));
        String symbol = itemSec.getString("SYMBOL", config.getString("GLOBAL.SORTS.SYMBOL", "▪ "));

        String worldPlayersOverride = null;
        if (itemSec.contains("WORLD")) {
            World w = Bukkit.getWorld(itemSec.getString("WORLD"));
            worldPlayersOverride = String.valueOf(w != null ? w.getPlayers().size() : 0);
        }

        if (worldPlayersOverride != null) name = name.replace("{players}", worldPlayersOverride);
        name = replaceGUIPlaceholders(name, p, prof, key, priceStr, itemSec);
        List<String> finalLore = new ArrayList<>();
        for (String l : loreList) {
            String cleanL = ChatColor.stripColor(color(l)).trim();
            if (isSortBtn && (cleanL.equalsIgnoreCase("Todos") || cleanL.equalsIgnoreCase("Membros online") || cleanL.equalsIgnoreCase("Membros offline") || cleanL.equalsIgnoreCase("Recente") || cleanL.equalsIgnoreCase("Mais vendido") || cleanL.equalsIgnoreCase("Menos vendido") || cleanL.equalsIgnoreCase("Padrão") || cleanL.equalsIgnoreCase("Maior preço") || cleanL.equalsIgnoreCase("Menor preço") || cleanL.equalsIgnoreCase("A - Z") || cleanL.equalsIgnoreCase("Z - A") || cleanL.equalsIgnoreCase("Maior valor") || cleanL.equalsIgnoreCase("Menor valor"))) {
                if (cleanL.equalsIgnoreCase(currentSort)) {
                    l = selPrefix + symbol + l;
                } else {
                    l = unselPrefix + symbol + l;
                }
            } else if (isFilterBtn && (cleanL.equalsIgnoreCase("Todos") || cleanL.equalsIgnoreCase("Blocos") || cleanL.equalsIgnoreCase("Ferramentas") || cleanL.equalsIgnoreCase("Comida") || cleanL.equalsIgnoreCase("Combate") || cleanL.equalsIgnoreCase("Poções") || cleanL.equalsIgnoreCase("Livros") || cleanL.equalsIgnoreCase("Ingredientes") || cleanL.equalsIgnoreCase("Utilidades"))) {
                if (cleanL.equalsIgnoreCase(currentFilter)) {
                    l = selPrefix + symbol + l;
                } else {
                    l = unselPrefix + symbol + l;
                }
            }
            if (worldPlayersOverride != null) l = l.replace("{players}", worldPlayersOverride);
            finalLore.add(color(replaceGUIPlaceholders(l, p, prof, key, priceStr, itemSec)));
        }

        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta && p != null && !itemSec.contains("TEXTURE") && !itemSec.contains("SKULL_OWNER")) {
            UUID targetUUID = null;
            PendingAction pa = pendingActions.get(p.getUniqueId());
            if (pa != null && pa.targetUUID != null) {
                targetUUID = pa.targetUUID;
            } else if (editingTeammate.containsKey(p.getUniqueId())) {
                targetUUID = editingTeammate.get(p.getUniqueId());
            } else if (viewingPlayerManagerProfile.containsKey(p.getUniqueId())) {
                targetUUID = viewingPlayerManagerProfile.get(p.getUniqueId());
            } else if (viewingStatsProfile.containsKey(p.getUniqueId())) {
                targetUUID = viewingStatsProfile.get(p.getUniqueId());
            } else {
                targetUUID = p.getUniqueId();
            }
            if (targetUUID != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(targetUUID);
                if (op != null) {
                    ItemMeta sm = item.getItemMeta();
                    setPlayerHead(((org.bukkit.inventory.meta.SkullMeta) sm), op);
                    item.setItemMeta(sm);
                }
            }
        }
        boolean isShopItem = itemSec.contains("PRICE-PER-UNIT");
        if (slot >= 0 && slot < inv.getSize()) {
            ItemStack itemObj = item.clone();
            ItemMeta meta = itemObj.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(name));
                meta.setLore(finalLore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                itemObj.setItemMeta(meta);
            }
            inv.setItem(slot, itemObj);
        } else if (slot == -1) {
            int startSlot = 0;
            if (inv.getHolder() instanceof GUIHolder && "SELL-MENU".equals(((GUIHolder) inv.getHolder()).getMenuId())) {
                startSlot = 45;
            }
            for (int i = startSlot; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                    ItemStack itemObj = item.clone();
                    ItemMeta meta = itemObj.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(color(name));
                        meta.setLore(finalLore);
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                        itemObj.setItemMeta(meta);
                    }
                    inv.setItem(i, itemObj);
                    break;
                }
            }
        }
    }

    private void applySkullTexture(org.bukkit.inventory.meta.SkullMeta sm, String textureOrOwner) {
        if (textureOrOwner == null || textureOrOwner.isEmpty()) return;
        String base64Val = textureOrOwner;
        if (textureOrOwner.startsWith("http://") || textureOrOwner.startsWith("https://")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureOrOwner + "\"}}}";
            base64Val = java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } else if (textureOrOwner.length() < 30 && !textureOrOwner.startsWith("eyJ")) {
            try {
                setPlayerHead(sm, Bukkit.getOfflinePlayer(textureOrOwner));
            } catch (Throwable ignored) {}
            return;
        }

        try {
            com.destroystokyo.paper.profile.PlayerProfile paperProfile = Bukkit.createProfile(UUID.nameUUIDFromBytes(base64Val.getBytes()), null);
            paperProfile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", base64Val));
            sm.setPlayerProfile(paperProfile);
            return;
        } catch (Throwable ignored) {}

        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.nameUUIDFromBytes(base64Val.getBytes()), null);
            Object properties = gameProfileClass.getMethod("getProperties").invoke(gameProfile);
            Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", base64Val);
            properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);

            try {
                java.lang.reflect.Method setProfileMethod = sm.getClass().getDeclaredMethod("setProfile", gameProfileClass);
                setProfileMethod.setAccessible(true);
                setProfileMethod.invoke(sm, gameProfile);
            } catch (Throwable t1) {
                java.lang.reflect.Field profileField = sm.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(sm, gameProfile);
            }
        } catch (Throwable ignored) {}
    }

    private void setPlayerHead(org.bukkit.inventory.meta.SkullMeta meta, OfflinePlayer op) {
        if (op == null) return;
        try {
            if (op.isOnline() && op.getPlayer() != null) {
                try {
                    meta.setPlayerProfile(op.getPlayer().getPlayerProfile());
                    return;
                } catch (Throwable ignored) {}
            }
            meta.setOwningPlayer(op);
        } catch (Throwable ignored) {}
    }

    public boolean hasHomePermission(Player p, int hNum) {
        if (p == null || p.hasPermission("donutcore.admin") || p.isOp()) return true;
        if (p.hasPermission("donutcore.home." + hNum) ||
            p.hasPermission("donutcore.homes." + hNum) ||
            p.hasPermission("home." + hNum) ||
            p.hasPermission("homes." + hNum)) {
            return true;
        }
        int defaultHomes = getConfig().getInt("SETTINGS.HOME-DEFAULT", 0);
        return hNum <= defaultHomes;
    }

    private String resolveConditionalValue(ConfigurationSection sec, String key, PlayerProfile prof, Player p) {
        if (sec == null) return "";
        boolean hasTeam = !prof.teamName.isEmpty();
        boolean hasTeamHome = hasTeam && teams.containsKey(prof.teamName.toLowerCase()) && teams.get(prof.teamName.toLowerCase()).home != null;
        boolean hasHome = false;
        boolean hasPerm = true;
        if (key.startsWith("HOME-") && !key.equalsIgnoreCase("HOME-BUTTON")) {
            try {
                int idx = Integer.parseInt(key.replace("HOME-", "")) - 1;
                hasPerm = hasHomePermission(p, idx + 1);
                hasHome = getHomeNameForSlot(prof, idx + 1) != null;
            } catch (Exception ignored) {}
        }

        if (!hasPerm && (sec.contains("NO-PERMISSION") || sec.contains("NO_PERMISSION"))) {
            return sec.getString("NO-PERMISSION", sec.getString("NO_PERMISSION"));
        } else if (!hasTeam && (sec.contains("NO_TEAM") || sec.contains("NO-TEAM"))) {
            return sec.getString("NO_TEAM", sec.getString("NO-TEAM"));
        } else if (hasTeam && !hasTeamHome && (sec.contains("NO_HOME") || sec.contains("NO-HOME"))) {
            return sec.getString("NO_HOME", sec.getString("NO-HOME"));
        } else if (hasTeam && hasTeamHome && (sec.contains("HAS_HOME") || sec.contains("HAS-HOME"))) {
            return sec.getString("HAS_HOME", sec.getString("HAS-HOME"));
        } else if (hasHome && (sec.contains("USED") || sec.contains("HAS-USED"))) {
            return sec.getString("USED", sec.getString("HAS-USED"));
        } else if (!hasHome && (sec.contains("NO-USED") || sec.contains("NO_USED"))) {
            return sec.getString("NO-USED", sec.getString("NO_USED"));
        }
        for (String k : sec.getKeys(false)) {
            return sec.getString(k);
        }
        return "";
    }

    private String resolveConditionalValue(ConfigurationSection sec, String key, PlayerProfile prof) {
        return resolveConditionalValue(sec, key, prof, null);
    }

    private List<String> resolveConditionalLore(ConfigurationSection sec, String key, PlayerProfile prof, Player p) {
        List<String> result = new ArrayList<>();
        if (sec == null) return result;
        boolean hasTeam = !prof.teamName.isEmpty();
        boolean hasTeamHome = hasTeam && teams.containsKey(prof.teamName.toLowerCase()) && teams.get(prof.teamName.toLowerCase()).home != null;
        boolean hasHome = false;
        boolean hasPerm = true;
        if (key.startsWith("HOME-") && !key.equalsIgnoreCase("HOME-BUTTON")) {
            try {
                int idx = Integer.parseInt(key.replace("HOME-", "")) - 1;
                hasPerm = hasHomePermission(p, idx + 1);
                hasHome = getHomeNameForSlot(prof, idx + 1) != null;
            } catch (Exception ignored) {}
        }

        String chosenKey = null;
        if (!hasPerm && (sec.contains("NO-PERMISSION") || sec.contains("NO_PERMISSION"))) {
            chosenKey = sec.contains("NO-PERMISSION") ? "NO-PERMISSION" : "NO_PERMISSION";
        } else if (!hasTeam && (sec.contains("NO_TEAM") || sec.contains("NO-TEAM"))) {
            chosenKey = sec.contains("NO_TEAM") ? "NO_TEAM" : "NO-TEAM";
        } else if (hasTeam && !hasTeamHome && (sec.contains("NO_HOME") || sec.contains("NO-HOME"))) {
            chosenKey = sec.contains("NO_HOME") ? "NO_HOME" : "NO-HOME";
        } else if (hasTeam && hasTeamHome && (sec.contains("HAS_HOME") || sec.contains("HAS-HOME"))) {
            chosenKey = sec.contains("HAS_HOME") ? "HAS_HOME" : "HAS-HOME";
        } else if (hasHome && (sec.contains("USED") || sec.contains("HAS-USED"))) {
            chosenKey = sec.contains("USED") ? "USED" : "HAS-USED";
        } else if (!hasHome && (sec.contains("NO-USED") || sec.contains("NO_USED"))) {
            chosenKey = sec.contains("NO-USED") ? "NO-USED" : "NO_USED";
        } else if (!sec.getKeys(false).isEmpty()) {
            chosenKey = sec.getKeys(false).iterator().next();
        }

        if (chosenKey != null) {
            if (sec.isList(chosenKey)) {
                result.addAll(sec.getStringList(chosenKey));
            } else if (sec.isString(chosenKey)) {
                result.add(sec.getString(chosenKey));
            }
        }
        return result;
    }

    private String replaceGUIPlaceholders(String text, Player p, PlayerProfile prof, String key, String priceStr) {
        return replaceGUIPlaceholders(text, p, prof, key, priceStr, null);
    }

    private String replaceGUIPlaceholders(String text, Player p, PlayerProfile prof, String key, String priceStr, ConfigurationSection itemSec) {
        if (text == null) return "";
        if (text.contains("{players}")) {
            String cuboidName = null;
            if (itemSec != null && itemSec.contains("CUBOID")) {
                cuboidName = itemSec.getString("CUBOID");
            }
            if (cuboidName == null && p != null && p.getOpenInventory() != null && p.getOpenInventory().getTopInventory() != null && p.getOpenInventory().getTopInventory().getHolder() instanceof GUIHolder) {
                String openId = ((GUIHolder) p.getOpenInventory().getTopInventory().getHolder()).getMenuId();
                if (openId.equals("SPAWN-MENU")) {
                    cuboidName = menuConfig.getString("SPAWN-MENU.AREAS." + key + ".CUBOID", "spawn" + key);
                } else if (openId.equals("AFK-MENU")) {
                    cuboidName = menuConfig.getString("AFK-MENU.AREAS." + key + ".CUBOID", "afk" + key);
                }
            }
            if (cuboidName == null) cuboidName = menuConfig.getString("SPAWN-MENU.AREAS." + key + ".CUBOID", null);
            if (cuboidName == null) cuboidName = menuConfig.getString("AFK-MENU.AREAS." + key + ".CUBOID", "spawn" + key);
            text = text.replace("{players}", String.valueOf(countPlayersInCuboid(cuboidName)));
        }
        if (p != null) {
            PendingAction pa = pendingActions.get(p.getUniqueId());
            if (pa != null) {
                String paAmtStr = formatAbbreviated(pa.amount);
                OfflinePlayer op = Bukkit.getOfflinePlayer(pa.targetUUID);
                String paName = op != null && op.getName() != null ? op.getName() : "Desconhecido";
                text = text.replace("${amount}", "$" + paAmtStr)
                           .replace("{amount}", paAmtStr)
                           .replace("{player}", paName);
            }
        }
        String status = "&a&lON";
        UUID targetUUID = (p != null) ? editingTeammate.get(p.getUniqueId()) : null;
        PlayerProfile targetProf = (targetUUID != null) ? getProfile(targetUUID) : null;
        if (key.equalsIgnoreCase("SCOREBOARD_VISIBILITY")) status = prof.scoreboardVisible ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("CHAINMAIL_ON_RESPAWN")) status = prof.chainmailOnRespawn ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("TP_AUTO") || key.equalsIgnoreCase("TPAUTO")) status = prof.tpAuto ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("DISABLE_PHANTOM_SPAWN")) status = !prof.phantomDisabled ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("DISABLE_MOB_SPAWN")) status = !prof.disableMobSpawn ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("TPA_REQUESTS")) status = prof.tpaEnabled ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("PAY_ALERTS")) status = prof.payAlerts ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("HOTBAR_MESSAGES")) status = prof.hotbarMessages ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("CLEAR_ENTITIES_MESSAGES")) status = prof.clearEntitiesMessages ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("BOUNTY_ALERTS")) status = prof.bountyAlerts ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("TPA_CONFIRM_MENUS")) status = prof.tpaConfirmMenus ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("LUNAR_TEAMMATES")) status = prof.lunarTeammates ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("TPA_HERE_REQUESTS")) status = prof.tpaHereEnabled ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("TEAM_INVITES")) status = prof.teamInvitesEnabled ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("PAYMENTS")) status = prof.paymentsEnabled ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("TEAM_CHAT")) status = prof.teamChatEnabled ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("PAY_CONFIRM_MENUS")) status = prof.payConfirmMenus ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("TOTEM_PARTICLES")) status = prof.totemParticles ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("FAST_CRYSTALS")) status = prof.fastCrystals ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("EDIT-HOME-BUTTON")) status = (targetProf != null && targetProf.permEditHome) ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("MANAGE-TEAMMATES-BUTTON")) status = (targetProf != null && targetProf.permManageTeammates) ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("VISIT-HOME-BUTTON")) status = (targetProf == null || targetProf.permVisitHome) ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("TEAM-CHAT-BUTTON")) status = (targetProf == null || targetProf.permTeamChat) ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("HELPER-BUTTON")) status = (targetProf != null && targetProf.permHelper) ? "&a&lON" : "&c&lOFF";
        else if (key.equalsIgnoreCase("PVP-BUTTON") || key.equalsIgnoreCase("PVP")) {
            if (targetProf != null) {
                status = targetProf.permTogglePvp ? "&a&lON" : "&c&lOFF";
            } else {
                Team team = !prof.teamName.isEmpty() ? teams.get(prof.teamName.toLowerCase()) : null;
                status = (team != null && team.pvp) ? "&a&lON" : "&c&lOFF";
            }
        }

        String val = "0";
        if (key.equalsIgnoreCase("MONEY")) val = "$" + formatAbbreviated(prof.money);
        else if (key.equalsIgnoreCase("SHARDS")) val = formatAbbreviated(prof.shards);
        else if (key.equalsIgnoreCase("KILLS")) val = String.valueOf(prof.kills);
        else if (key.equalsIgnoreCase("DEATHS")) val = String.valueOf(prof.deaths);
        else if (key.equalsIgnoreCase("PLAYTIME")) val = formatTime(prof.playtimeSeconds);
        else if (key.equalsIgnoreCase("BLOCKS_PLACED") || key.equalsIgnoreCase("BLOCKS-PLACED")) val = String.valueOf(prof.blocksPlaced);
        else if (key.equalsIgnoreCase("BLOCKS_BROKEN") || key.equalsIgnoreCase("BLOCKS-BROKEN")) val = String.valueOf(prof.blocksBroken);
        else if (key.equalsIgnoreCase("MOBS_KILLED") || key.equalsIgnoreCase("MOBS-KILLED")) val = String.valueOf(prof.mobsKilled);
        else if (key.equalsIgnoreCase("KILL_STREAK") || key.equalsIgnoreCase("KILL-STREAK")) val = String.valueOf(prof.killStreak);
        else if (key.equalsIgnoreCase("HIGHEST_KILL_STREAK") || key.equalsIgnoreCase("HIGHEST-KILL-STREAK")) val = String.valueOf(prof.highestKillStreak);
        else if (key.equalsIgnoreCase("MONEY_SPENT") || key.equalsIgnoreCase("MONEY-SPENT")) val = "$" + formatAbbreviated(prof.moneySpent);
        else if (key.equalsIgnoreCase("MONEY_MADE") || key.equalsIgnoreCase("MONEY-MADE")) val = "$" + formatAbbreviated(prof.moneyMade);

        String cat = key.replace("-BUTTON", "").replace("_BUTTON", "").replace("-", "_").toUpperCase();
        if (cat.equals("POTIONS")) cat = "POTION";
        double mult = prof.getMultiplier(cat);
        double nextMult = mult + 0.25;
        int currentLevel = prof.sellMultipliers.getOrDefault(cat, 1);
        double currentProgress = prof.categoryProgress.getOrDefault(cat, 0.0);
        double nextGoal = getCategoryGoal(currentLevel);
        int percentage = (int) Math.min(100, Math.max(0, (currentProgress / nextGoal) * 100));

        String nextMultStr;
        String currentEarnedStr;
        String nextGoalStr;
        if (currentLevel >= 20) {
            percentage = 100;
            nextMultStr = String.format("%.2fx (MÁXIMO)", mult);
            double maxGoal = getCategoryGoal(19);
            currentEarnedStr = "$" + formatAbbreviated(maxGoal);
            nextGoalStr = "$" + formatAbbreviated(maxGoal);
        } else {
            nextMultStr = String.format("%.2fx", nextMult);
            currentEarnedStr = "$" + formatAbbreviated(currentProgress);
            nextGoalStr = "$" + formatAbbreviated(nextGoal);
        }

        StringBuilder bar = new StringBuilder();
        int greenBars = percentage / 10;
        bar.append("&a");
        for (int i = 0; i < greenBars; i++) bar.append("█");
        bar.append("&7");
        for (int i = greenBars; i < 10; i++) bar.append("█");

        Team team = !prof.teamName.isEmpty() ? teams.get(prof.teamName.toLowerCase()) : null;
        String tName = team != null ? team.name : (prof.teamName.isEmpty() ? "Nenhuma" : prof.teamName);
        int maxMemb = getConfig().getInt("TEAM.MAX-MEMBERS", 10);
        String onlineStr = String.valueOf(Bukkit.getOnlinePlayers().size());

        String openMenuId = "";
        if (p.getOpenInventory() != null && p.getOpenInventory().getTopInventory() != null && p.getOpenInventory().getTopInventory().getHolder() instanceof GUIHolder) {
            openMenuId = ((GUIHolder) p.getOpenInventory().getTopInventory().getHolder()).getMenuId();
        }
        String sortStateStr = openMenuId.equals("WORTH-MENU") ? viewingWorthSort.getOrDefault(p.getUniqueId(), "A - Z") : (openMenuId.equals("BOUNTIES-MENU") ? viewingBountySort.getOrDefault(p.getUniqueId(), "Maior valor") : viewingHistorySort.getOrDefault(p.getUniqueId(), "A - Z"));

        OfflinePlayer opTarget = (targetUUID != null) ? Bukkit.getOfflinePlayer(targetUUID) : null;
        if (opTarget == null && p != null && viewingStatsProfile.containsKey(p.getUniqueId())) {
            opTarget = Bukkit.getOfflinePlayer(viewingStatsProfile.get(p.getUniqueId()));
        } else if (opTarget == null && p != null && viewingPlayerManagerProfile.containsKey(p.getUniqueId())) {
            opTarget = Bukkit.getOfflinePlayer(viewingPlayerManagerProfile.get(p.getUniqueId()));
        }
        String playerName = (prof != null && prof.username != null && !prof.username.isEmpty() && !prof.username.equals("Unknown")) ? prof.username : ((opTarget != null && opTarget.getName() != null) ? opTarget.getName() : p.getName());
        String homeName = "";
        if (key != null && key.startsWith("HOME-") && !key.equalsIgnoreCase("HOME-BUTTON") && prof != null) {
            try {
                int hNum = Integer.parseInt(key.replace("HOME-", ""));
                String found = getHomeNameForSlot(prof, hNum);
                homeName = (found != null) ? found : String.valueOf(hNum);
            } catch (Exception ignored) {}
        } else if (key != null && key.equalsIgnoreCase("TEAM-HOME") && prof != null) {
            homeName = !prof.teamName.isEmpty() ? prof.teamName : "Equipe";
        }
        String lastCoordsStr = "Nenhuma";
        if (prof != null && prof.lastLocation != null && prof.lastLocation.getWorld() != null) {
            lastCoordsStr = String.format("%s, X: %d, Y: %d, Z: %d", prof.lastLocation.getWorld().getName(), prof.lastLocation.getBlockX(), prof.lastLocation.getBlockY(), prof.lastLocation.getBlockZ());
        }
        long boosterEnd = (p != null) ? shardBoosters.getOrDefault(p.getUniqueId(), 0L) : 0L;
        boolean hasBooster = boosterEnd > System.currentTimeMillis();
        if (p != null && !hasBooster) shardBoosters.remove(p.getUniqueId());
        String boosterTimeStr = hasBooster ? formatTime((boosterEnd - System.currentTimeMillis()) / 1000L) : "";
        PendingAction paGui = (p != null) ? pendingActions.get(p.getUniqueId()) : null;
        boolean isTpaHere = (paGui != null && "TPAHERE".equals(paGui.extraData));
        String tpaFull = isTpaHere ? "ᴛᴘᴀʜᴇʀᴇ" : "ᴛᴘᴀ";
        String herePart = isTpaHere ? "ʜᴇʀᴇ" : "";
        String replaced = text.replace("{home}", homeName)
                .replace("{HOME}", homeName)
                .replace("{name}", homeName)
                .replace("{lastcoordinates}", lastCoordsStr)
                .replace("{status}", status)
                .replace("{state}", status)
                .replace("{player}", playerName)
                .replace("{username}", playerName)
                .replace("{ping}", String.valueOf(p.getPing()))
                .replace("${ping}", String.valueOf(p.getPing()))
                .replace("{players}", onlineStr)
                .replace("{online}", onlineStr)
                .replace("%online%", onlineStr)
                .replace("{value}", val)
                .replace("${price}", "$" + priceStr)
                .replace("{price}", priceStr)
                .replace("{amount}", priceStr)
                .replace("{status}", color(status))
                .replace("{val}", val)
                .replace("{item-name}", key)
                .replace("{quantity}", "1")
                .replace("{type}", key)
                .replace("ᴛᴘᴀ {here}", tpaFull)
                .replace("ᴛᴘᴀ{here}", tpaFull)
                .replace("TPA {here}", tpaFull)
                .replace("TPA{here}", tpaFull)
                .replace("{tpa_type}", tpaFull)
                .replace("{here}", herePart)
                .replace("{world}", p.getWorld().getName())
                .replace("{sort_state}", sortStateStr)
                .replace("{next_multiplier}", nextMultStr)
                .replace("{porcentage_level}", bar.toString())
                .replace("{porcentage}", String.valueOf(percentage))
                .replace("{current_earned}", currentEarnedStr)
                .replace("{next_goal}", nextGoalStr)
                .replace("{team_name}", tName)
                .replace("{team}", tName)
                .replace("{TEAM_NAME}", tName)
                .replace("{TEAM}", tName)
                .replace("{Team_Name}", tName)
                .replace("{Team}", tName)
                .replace("{max_members}", String.valueOf(maxMemb))
                .replace("{shards}", formatAbbreviated(prof.shards))
                .replace("{kills}", String.valueOf(prof.kills))
                .replace("{deaths}", String.valueOf(prof.deaths))
                .replace("{playtime}", formatTime(prof.playtimeSeconds))
                .replace("{money}", formatAbbreviated(prof.money))
                .replace("${money}", "$" + formatAbbreviated(prof.money))
                .replace("{blocksbroken}", String.valueOf(prof.blocksBroken))
                .replace("{blocksplaced}", String.valueOf(prof.blocksPlaced))
                .replace("{highestkillstreak}", String.valueOf(prof.highestKillStreak))
                .replace("{killstreak}", String.valueOf(prof.killStreak))
                .replace("{moneyspent}", formatAbbreviated(prof.moneySpent))
                .replace("${moneyspent}", "$" + formatAbbreviated(prof.moneySpent))
                .replace("{moneymade}", formatAbbreviated(prof.moneyMade))
                .replace("${moneymade}", "$" + formatAbbreviated(prof.moneyMade))
                .replace("{mobskilled}", String.valueOf(prof.mobsKilled))
                .replace("%economy_booster_countdown%", boosterTimeStr)
                .replace("%donutcore_booster_countdown%", boosterTimeStr)
                .replace("{booster_countdown}", boosterTimeStr)
                .replace("%economy_booster%", boosterTimeStr)
                .replace("%donutcore_booster%", boosterTimeStr)
                .replace("{shard_booster}", hasBooster ? color(scoreboardConfig.getString("SCOREBOARD.SHARD-BOOSTER", "&dBooster Ativo").replace("%economy_booster_countdown%", boosterTimeStr).replace("%donutcore_booster_countdown%", boosterTimeStr).replace("{booster_countdown}", boosterTimeStr)) : "");

        replaced = applyDonutInvestPlaceholders(p, replaced);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && p != null) {
            try {
                replaced = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, replaced);
            } catch (Throwable ignored) {}
        }
        return replaced;
    }

    private void addItem(Inventory inv, int slot, Material mat, String name, String... lore) {
        if (mat == null) return;
        addItem(inv, slot, new ItemStack(mat), name, lore);
    }

    private void addItem(Inventory inv, int slot, ItemStack item, String name, String... lore) {
        if (slot >= inv.getSize() || item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(color(name));
            }
            List<String> lList = new ArrayList<>();
            for (String l : lore) {
                String colored = color(l);
                if (!colored.isEmpty() && !colored.startsWith("§")) colored = "§f" + colored;
                lList.add(colored);
            }
            meta.setLore(lList);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    public static class GUIHolder implements InventoryHolder {
        private final String menuId;
        public GUIHolder(String menuId) { this.menuId = menuId; }
        public String getMenuId() { return menuId; }
        @Override public Inventory getInventory() { return null; }
    }

    public void openCustomEnderChest(Player viewer, Player target) {
        boolean sixRows = getConfig().getBoolean("ENDER-CHEST.SIX-ROW", false) || target.hasPermission("donutcore.enderchest.sixrows") || target.hasPermission("donucore.enderchest.sixrows");
        if (!sixRows) {
            viewer.openInventory(target.getEnderChest());
            return;
        }
        PlayerProfile prof = getProfile(target);
        String title = (viewer.equals(target)) ? color("&8Ender Chest") : color("&8Ender Chest: " + target.getName());
        Inventory inv = Bukkit.createInventory(new GUIHolder("SIX-ROW-ENDERCHEST:" + target.getUniqueId().toString()), 54, title);
        boolean hasAnyItem = false;
        if (prof.enderchestContents != null) {
            for (int i = 0; i < 54; i++) {
                if (prof.enderchestContents[i] != null && prof.enderchestContents[i].getType() != Material.AIR) {
                    inv.setItem(i, prof.enderchestContents[i]);
                    hasAnyItem = true;
                }
            }
        }
        if (!hasAnyItem && target.getEnderChest() != null) {
            for (int i = 0; i < target.getEnderChest().getSize() && i < 54; i++) {
                ItemStack vanilla = target.getEnderChest().getItem(i);
                if (vanilla != null && vanilla.getType() != Material.AIR) {
                    inv.setItem(i, vanilla);
                }
            }
        }
        viewer.openInventory(inv);
        try { viewer.playSound(viewer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f); } catch (Throwable ignored) { playSound(viewer, "MENUS.BUTTON-CLICK"); }
        if (activeEnderChestBlocks.containsKey(viewer.getUniqueId())) {
            Location loc = activeEnderChestBlocks.get(viewer.getUniqueId());
            if (loc != null && loc.getWorld() != null) {
                Bukkit.getScheduler().runTask(this, () -> {
                    try {
                        if (loc.getBlock().getState() instanceof org.bukkit.block.Lidded) {
                            ((org.bukkit.block.Lidded) loc.getBlock().getState()).open();
                        }
                    } catch (Throwable ignored) {}
                });
            }
        }
    }

    public void openCustomEnderChest(Player p) {
        openCustomEnderChest(p, p);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof GUIHolder) {
            GUIHolder holder = (GUIHolder) e.getInventory().getHolder();
            if (holder.getMenuId().equals("SELL-MENU")) {
                for (int slot : e.getRawSlots()) {
                    if (slot >= 45 && slot < e.getInventory().getSize()) {
                        e.setCancelled(true);
                        return;
                    }
                }
            } else if (!holder.getMenuId().startsWith("SIX-ROW-ENDERCHEST")) {
                for (int slot : e.getRawSlots()) {
                    if (slot < e.getInventory().getSize()) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof GUIHolder) {
            GUIHolder holder = (GUIHolder) e.getInventory().getHolder();
            Player p = (Player) e.getWhoClicked();

            if (holder.getMenuId().equals("SELL-MENU")) {
                if (e.getClickedInventory() == e.getInventory() && e.getSlot() >= 45) {
                    e.setCancelled(true);
                } else if (e.isShiftClick() && e.getClickedInventory() != e.getInventory()) {
                    boolean hasSpace = false;
                    ItemStack clicked = e.getCurrentItem();
                    for (int i = 0; i < 45; i++) {
                        ItemStack item = e.getInventory().getItem(i);
                        if (item == null || item.getType() == Material.AIR) {
                            hasSpace = true;
                            break;
                        } else if (clicked != null && item.isSimilar(clicked) && item.getAmount() < item.getMaxStackSize()) {
                            hasSpace = true;
                            break;
                        }
                    }
                    if (!hasSpace) {
                        e.setCancelled(true);
                        return;
                    }
                }
            } else if (holder.getMenuId().startsWith("SIX-ROW-ENDERCHEST")) {
                return;
            } else {
                if (e.getClickedInventory() == e.getInventory() || e.isShiftClick()) {
                    e.setCancelled(true);
                } else {
                    return;
                }
            }

            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            String id = holder.getMenuId();
            String itemTitle = ChatColor.stripColor(e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().hasDisplayName() ? e.getCurrentItem().getItemMeta().getDisplayName() : "");

            if (id.equals("SOCIAL-MENU")) {
                p.closeInventory();
                playSound(p, "MENUS.BUTTON-CLICK");
                if (itemTitle.toLowerCase().contains("discord")) {
                    p.sendMessage(color("&b🌐 &lDISCORD &f• &aClique para entrar: &bhttps://discord.gg/seuservidor"));
                } else if (itemTitle.toLowerCase().contains("loja") || itemTitle.toLowerCase().contains("vip")) {
                    p.sendMessage(color("&a🛒 &lLOJA VIP &f• &aAcesse nossa loja: &ehttps://loja.seuservidor.com"));
                } else if (itemTitle.toLowerCase().contains("site") || itemTitle.toLowerCase().contains("web")) {
                    p.sendMessage(color("&3🖥 &lSITE &f• &aAcesse nosso site: &bhttps://www.seuservidor.com"));
                } else if (itemTitle.toLowerCase().contains("tiktok") || itemTitle.toLowerCase().contains("youtube")) {
                    p.sendMessage(color("&c📱 &lMÍDIA &f• &aAcompanhe: &c@seuservidor"));
                }
                return;
            }

            if (id.equals("PLAYERMANAGER-MENU")) {
                UUID targetUUID = viewingPlayerManagerProfile.get(p.getUniqueId());
                if (targetUUID == null) return;
                PlayerProfile targetProf = profiles.get(targetUUID);
                if (targetProf == null) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(targetUUID);
                    if (op != null) targetProf = getProfile(op);
                }
                if (targetProf == null) return;

                if (e.getSlot() == 13 || e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                    Player targetPlayer = Bukkit.getPlayer(targetUUID);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        p.closeInventory();
                        teleportInstant(p, targetPlayer.getLocation());
                    } else {
                        p.sendMessage(color("&cO jogador não está online no momento!"));
                        try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Throwable ignored) {}
                    }
                } else if (e.getSlot() == 14 || e.getCurrentItem().getType() == Material.ENDER_PEARL) {
                    if (targetProf.lastLocation != null && targetProf.lastLocation.getWorld() != null) {
                        p.closeInventory();
                        teleportInstant(p, targetProf.lastLocation);
                    } else {
                        p.sendMessage(color("&cO jogador não possui uma última coordenada registrada!"));
                        try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Throwable ignored) {}
                    }
                } else if (e.getSlot() >= 37 && e.getSlot() <= 41) {
                    int hNum = e.getSlot() - 36;
                    String hName = getHomeNameForSlot(targetProf, hNum);
                    if (hName != null && targetProf.homes.containsKey(hName) && targetProf.homes.get(hName) != null) {
                        p.closeInventory();
                        teleportInstant(p, targetProf.homes.get(hName));
                    } else {
                        p.sendMessage(color("&cO jogador não possui a Home " + hNum + "!"));
                        try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Throwable ignored) {}
                    }
                } else if (e.getSlot() == 43) {
                    if (!targetProf.teamName.isEmpty() && teams.containsKey(targetProf.teamName.toLowerCase()) && teams.get(targetProf.teamName.toLowerCase()).home != null) {
                        p.closeInventory();
                        teleportInstant(p, teams.get(targetProf.teamName.toLowerCase()).home);
                    } else {
                        p.sendMessage(color("&cO jogador não possui uma Team Home registrada!"));
                        try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Throwable ignored) {}
                    }
                }
                return;
            }


            if (id.equals("HOME-MENU")) {
                if (itemTitle.toLowerCase().contains("team") || itemTitle.toLowerCase().contains("ᴛᴇᴀᴍ") || itemTitle.toLowerCase().contains("equipe")) {
                    PlayerProfile prof = getProfile(p);
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                        return;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (team == null) return;

                    boolean isSaveOrDelete = e.getCurrentItem().getType().name().contains("DYE") || e.isRightClick() || e.getSlot() > 17;
                    PlayerProfile profSelf = getProfile(p);
                    if (isSaveOrDelete) {
                        if (!team.leader.equals(p.getUniqueId()) && !profSelf.permHelper && !profSelf.permEditHome) {
                            p.sendMessage(color("&cVocê não tem permissão para editar a home da equipe."));
                            try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                            return;
                        }
                        if (team.home == null) {
                            team.home = p.getLocation();
                            p.sendMessage(getMsg("TEAM.TEAM-HOME-SET"));
                            playSound(p, "SELL.LEVEL-UP");
                        } else {
                            team.home = null;
                            p.sendMessage(getMsg("TEAM.TEAM-HOME-DELETED"));
                            playSound(p, "MENUS.BUTTON-CLICK");
                        }
                        openGUI(p, "HOME-MENU");
                    } else {
                        if (!team.leader.equals(p.getUniqueId()) && !profSelf.permHelper && !profSelf.permVisitHome) {
                            p.sendMessage(color("&cVocê não tem permissão para visitar a home da equipe."));
                            try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                            return;
                        }
                        if (team.home == null) {
                            if (!team.leader.equals(p.getUniqueId()) && !profSelf.permHelper && !profSelf.permEditHome) {
                                p.sendMessage(getMsg("TEAM.NO-TEAM-HOME"));
                                try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                                return;
                            }
                            team.home = p.getLocation();
                            p.sendMessage(getMsg("TEAM.TEAM-HOME-SET"));
                            playSound(p, "SELL.LEVEL-UP");
                            openGUI(p, "HOME-MENU");
                            return;
                        }
                        p.closeInventory();
                        teleportWithCooldown(p, team.home, "TEAM-HOME");
                    }
                    return;
                }
                PlayerProfile prof = getProfile(p);
                String hName = null;
                Integer homeNumber = null;
                if (e.getSlot() >= 12 && e.getSlot() <= 16) {
                    homeNumber = e.getSlot() - 11;
                } else if (e.getSlot() >= 21 && e.getSlot() <= 25) {
                    homeNumber = e.getSlot() - 20;
                } else if (itemTitle.startsWith("Home: ") || itemTitle.startsWith("ʜᴏᴍᴇ ") || itemTitle.toLowerCase().startsWith("home ")) {
                    String numStr = itemTitle.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        try { homeNumber = Integer.parseInt(numStr); } catch (Exception ignored) {}
                    }
                }

                if (homeNumber != null) {
                    if (!hasHomePermission(p, homeNumber)) {
                        p.sendMessage(getMsg("HOMES.NO-PERMISSION"));
                        try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                        return;
                    }
                    hName = getHomeNameForSlot(prof, homeNumber);
                } else if (itemTitle.startsWith("Home: ") || itemTitle.startsWith("ʜᴏᴍᴇ ") || itemTitle.toLowerCase().startsWith("home ")) {
                    hName = itemTitle.replace("Home: ", "").replace("ʜᴏᴍᴇ ", "").trim();
                }

                String defaultName = (homeNumber != null) ? String.valueOf(homeNumber) : (hName != null ? hName : "1");
                boolean homeExists = (hName != null && prof.homes.containsKey(hName));

                boolean isDelete = e.isRightClick() || (e.getCurrentItem() != null && e.getCurrentItem().getType().name().contains("DYE")) || e.getSlot() > 17;
                if (homeExists) {
                    if (isDelete) {
                        pendingActions.put(p.getUniqueId(), new PendingAction(p.getUniqueId(), 0, "HOME_DELETE:" + hName));
                        openGUI(p, "CONFIRM-MENU");
                    } else {
                        p.closeInventory();
                        teleportWithCooldown(p, prof.homes.get(hName), "HOME");
                    }
                } else {
                    prof.homes.put(defaultName, p.getLocation());
                    saveProfile(prof);
                    p.sendMessage(getMsg("HOMES.SET-SUCCESS").replace("{home}", defaultName));
                    playSound(p, "SELL.LEVEL-UP");
                    openGUI(p, "HOME-MENU");
                }
            } else if (id.equals("TEAM-MENUS.TEAM")) {
                if (e.getSlot() >= 0 && e.getSlot() < 45 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                    PlayerProfile prof = getProfile(p);
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (team != null) {
                        UUID targetUUID = null;
                        if (e.getCurrentItem().getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta) {
                            OfflinePlayer op = ((org.bukkit.inventory.meta.SkullMeta) e.getCurrentItem().getItemMeta()).getOwningPlayer();
                            if (op != null && op.getUniqueId() != null) {
                                targetUUID = op.getUniqueId();
                            }
                        }
                        if (targetUUID == null && e.getSlot() < team.members.size()) {
                            targetUUID = new ArrayList<>(team.members).get(e.getSlot());
                        }
                        if (targetUUID != null) {
                            if (targetUUID.equals(team.leader) && !p.getUniqueId().equals(team.leader)) {
                                p.sendMessage(color("&cVocê não pode editar o líder da equipe!"));
                                try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                                return;
                            }
                            if (!team.leader.equals(p.getUniqueId()) && !prof.permHelper) {
                                p.sendMessage(color("&cApenas o líder ou ajudantes podem editar permissões da equipe!"));
                                try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                                return;
                            }
                            editingTeammate.put(p.getUniqueId(), targetUUID);
                            openGUI(p, "TEAM-MENUS.TEAM-EDIT-MEMBER");
                        }
                    }
                } else if (e.getSlot() == 45 || itemTitle.equalsIgnoreCase("ѕᴇᴀʀᴄʜ") || itemTitle.equalsIgnoreCase("search") || itemTitle.contains("Pesquisar")) {
                    p.closeInventory();
                    openSignGUI(p, "TEAM", "Digite a busca", "(ESC p/ cancelar)");
                } else if (e.getSlot() == 46 || itemTitle.equalsIgnoreCase("ᴛɪᴘᴏ") || itemTitle.contains("Tipo") || itemTitle.equalsIgnoreCase("Sort") || itemTitle.contains("Sort")) {
                    List<String> sortLore = menuConfig.getStringList("TEAM-MENUS.TEAM.SORT-BUTTON.LORE");
                    boolean hasTodos = sortLore.stream().anyMatch(l -> ChatColor.stripColor(color(l)).trim().equalsIgnoreCase("Todos"));
                    String cur = viewingTeamFilter.getOrDefault(p.getUniqueId(), hasTodos ? "Todos" : "Membros online");
                    String next;
                    if (hasTodos) {
                        next = cur.equalsIgnoreCase("Todos") ? "Membros online" : (cur.equalsIgnoreCase("Membros online") ? "Membros offline" : "Todos");
                    } else {
                        next = cur.equalsIgnoreCase("Membros online") ? "Membros offline" : "Membros online";
                    }
                    viewingTeamFilter.put(p.getUniqueId(), next);
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "TEAM-MENUS.TEAM");
                } else if (e.getSlot() == 52) {
                    p.closeInventory();
                    p.performCommand("team home");
                } else if (e.getSlot() == 53) {
                    p.performCommand("team pvp");
                    openGUI(p, "TEAM-MENUS.TEAM");
                }
            } else if (id.equals("SETTINGS-MENU")) {
                PlayerProfile prof = getProfile(p);
                String cleanTitle = ChatColor.stripColor(itemTitle).toLowerCase();
                int slot = e.getSlot();
                Material type = (e.getCurrentItem() != null) ? e.getCurrentItem().getType() : Material.AIR;

                if (slot == 10 || cleanTitle.contains("ᴘᴀʏ ᴀʟᴇʀᴛ") || cleanTitle.contains("pay alert") || type == Material.CHERRY_SIGN) {
                    prof.payAlerts = !prof.payAlerts;
                    p.sendMessage(getMsg(prof.payAlerts ? "SETTINGS.PAY-ALERTS-ENABLED" : "SETTINGS.PAY-ALERTS-DISABLED"));
                } else if (slot == 11 || cleanTitle.contains("ʜᴏᴛʙᴀʀ") || cleanTitle.contains("hotbar") || type == Material.CRIMSON_SIGN) {
                    prof.hotbarMessages = !prof.hotbarMessages;
                    p.sendMessage(getMsg(prof.hotbarMessages ? "SETTINGS.HOTBAR-MESSAGES-ENABLED" : "SETTINGS.HOTBAR-MESSAGES-DISABLED"));
                } else if (slot == 12 || cleanTitle.contains("ᴄʟᴇᴀʀ ᴇɴᴛɪᴛɪᴇ") || cleanTitle.contains("clear entit") || cleanTitle.contains("lixeiro") || type == Material.BAMBOO_SIGN) {
                    prof.clearEntitiesMessages = !prof.clearEntitiesMessages;
                    p.sendMessage(getMsg(prof.clearEntitiesMessages ? "SETTINGS.CLEAR-ENTITIES-ENABLED" : "SETTINGS.CLEAR-ENTITIES-DISABLED"));
                } else if (slot == 13 || cleanTitle.contains("ʙᴏᴜɴᴛʏ") || cleanTitle.contains("bounty") || type == Material.WARPED_SIGN) {
                    prof.bountyAlerts = !prof.bountyAlerts;
                    p.sendMessage(getMsg(prof.bountyAlerts ? "SETTINGS.BOUNTY-ALERTS-ENABLED" : "SETTINGS.BOUNTY-ALERTS-DISABLED"));
                } else if (slot == 19 || cleanTitle.contains("ᴄʜᴀɪɴᴍᴀɪʟ") || cleanTitle.contains("chainmail") || type == Material.CHAINMAIL_HELMET) {
                    prof.chainmailOnRespawn = !prof.chainmailOnRespawn;
                    p.sendMessage(getMsg(prof.chainmailOnRespawn ? "SETTINGS.CHAINMAIL-ENABLED" : "SETTINGS.CHAINMAIL-DISABLED"));
                } else if (slot == 20 || cleanTitle.contains("ѕᴄᴏʀᴇʙᴏᴀʀᴅ") || cleanTitle.contains("scoreboard") || type == Material.LECTERN) {
                    prof.scoreboardVisible = !prof.scoreboardVisible;
                    updateScoreboard(p, prof);
                    p.sendMessage(getMsg(prof.scoreboardVisible ? "SETTINGS.SCOREBOARD-ENABLED" : "SETTINGS.SCOREBOARD-DISABLED"));
                } else if (slot == 21 || cleanTitle.contains("ᴛᴘᴀ ᴄᴏɴꜰɪʀᴍ") || cleanTitle.contains("tpa confirm") || type == Material.FEATHER) {
                    prof.tpaConfirmMenus = !prof.tpaConfirmMenus;
                    p.sendMessage(getMsg(prof.tpaConfirmMenus ? "SETTINGS.TPA-CONFIRM-ENABLED" : "SETTINGS.TPA-CONFIRM-DISABLED"));
                } else if (slot == 22 || cleanTitle.contains("ʟᴜɴᴀʀ") || cleanTitle.contains("lunar") || type == Material.BLUE_CANDLE) {
                    prof.lunarTeammates = !prof.lunarTeammates;
                    p.sendMessage(getMsg(prof.lunarTeammates ? "SETTINGS.LUNAR-ENABLED" : "SETTINGS.LUNAR-DISABLED"));
                } else if (slot == 23 || (cleanTitle.contains("ᴛᴘᴀ ʀᴇǫᴜᴇѕᴛ") && !cleanTitle.contains("ʜᴇʀᴇ")) || (cleanTitle.contains("tpa request") && !cleanTitle.contains("here")) || type == Material.ENDER_PEARL) {
                    prof.tpaEnabled = !prof.tpaEnabled;
                    p.sendMessage(getMsg(prof.tpaEnabled ? "SETTINGS.TPA-REQUESTS-ENABLED" : "SETTINGS.TPA-REQUESTS-DISABLED"));
                } else if (slot == 24 || cleanTitle.contains("ᴛᴘᴀ ʜᴇʀᴇ") || cleanTitle.contains("tpa here") || type == Material.ENDER_EYE) {
                    prof.tpaHereEnabled = !prof.tpaHereEnabled;
                    p.sendMessage(getMsg(prof.tpaHereEnabled ? "SETTINGS.TPA-HERE-REQUESTS-ENABLED" : "SETTINGS.TPA-HERE-REQUESTS-DISABLED"));
                } else if (slot == 25 || cleanTitle.contains("ᴛᴇᴀᴍ ɪɴᴠɪᴛᴇ") || cleanTitle.contains("team invite") || type == Material.SHIELD) {
                    prof.teamInvitesEnabled = !prof.teamInvitesEnabled;
                    p.sendMessage(getMsg(prof.teamInvitesEnabled ? "SETTINGS.TEAM-INVITES-ENABLED" : "SETTINGS.TEAM-INVITES-DISABLED"));
                } else if (slot == 28 || cleanTitle.contains("ᴘᴀʏᴍᴇɴᴛ") || cleanTitle.contains("payment") || type == Material.EMERALD) {
                    prof.paymentsEnabled = !prof.paymentsEnabled;
                    p.sendMessage(getMsg(prof.paymentsEnabled ? "SETTINGS.PAYMENTS-ENABLED" : "SETTINGS.PAYMENTS-DISABLED"));
                } else if (slot == 29 || cleanTitle.contains("ᴛᴇᴀᴍ ᴄʜᴀᴛ") || cleanTitle.contains("team chat") || type == Material.BELL) {
                    prof.teamChatEnabled = !prof.teamChatEnabled;
                    p.sendMessage(getMsg(prof.teamChatEnabled ? "SETTINGS.TEAM-CHAT-ENABLED" : "SETTINGS.TEAM-CHAT-DISABLED"));
                } else if (slot == 30 || cleanTitle.contains("ᴅɪѕᴀʙʟᴇ ᴍᴏʙ") || cleanTitle.contains("disable mob") || type == Material.ZOMBIE_HEAD) {
                    prof.disableMobSpawn = !prof.disableMobSpawn;
                    p.sendMessage(getMsg(!prof.disableMobSpawn ? "SETTINGS.DISABLE-MOB-SPAWN-ENABLED" : "SETTINGS.DISABLE-MOB-SPAWN-DISABLED"));
                } else if (slot == 31 || cleanTitle.contains("ᴅɪѕᴀʙʟᴇ ᴘʜᴀɴᴛᴏᴍ") || cleanTitle.contains("disable phantom") || type == Material.PHANTOM_MEMBRANE) {
                    prof.phantomDisabled = !prof.phantomDisabled;
                    p.sendMessage(getMsg(!prof.phantomDisabled ? "SETTINGS.DISABLE-PHANTOM-ENABLED" : "SETTINGS.DISABLE-PHANTOM-DISABLED"));
                } else if (slot == 32 || cleanTitle.contains("ᴘᴀʏ ᴄᴏɴꜰɪʀᴍ") || cleanTitle.contains("pay confirm") || type == Material.PAPER) {
                    prof.payConfirmMenus = !prof.payConfirmMenus;
                    p.sendMessage(getMsg(prof.payConfirmMenus ? "SETTINGS.PAY-CONFIRM-ENABLED" : "SETTINGS.PAY-CONFIRM-DISABLED"));
                } else if (slot == 33 || cleanTitle.contains("ᴛᴘ ᴀᴜᴛᴏ") || cleanTitle.contains("tp auto") || cleanTitle.contains("tpauto") || type == Material.GOAT_HORN) {
                    prof.tpAuto = !prof.tpAuto;
                    p.sendMessage(getMsg(prof.tpAuto ? "SETTINGS.TP-AUTO-ENABLED" : "SETTINGS.TP-AUTO-DISABLED"));
                } else if (slot == 34 || cleanTitle.contains("ꜰᴀѕᴛ ᴄʀʏѕᴛᴀʟ") || cleanTitle.contains("fast crystal") || type == Material.END_CRYSTAL) {
                    prof.fastCrystals = !prof.fastCrystals;
                    p.sendMessage(getMsg(prof.fastCrystals ? "SETTINGS.FAST-CRYSTALS-ENABLED" : "SETTINGS.FAST-CRYSTALS-DISABLED"));
                } else if (slot == 38 || cleanTitle.contains("ᴛᴏᴛᴇᴍ ᴘᴀʀᴛɪᴄʟᴇ") || cleanTitle.contains("totem particle") || type == Material.TOTEM_OF_UNDYING) {
                    prof.totemParticles = !prof.totemParticles;
                    p.sendMessage(getMsg(prof.totemParticles ? "SETTINGS.TOTEM-PARTICLES-ENABLED" : "SETTINGS.TOTEM-PARTICLES-DISABLED"));
                }
                playSound(p, "MENUS.BUTTON-CLICK");
                openGUI(p, "SETTINGS-MENU");
            } else if (id.equals("CATEGORIES")) {
                playSound(p, "MENUS.BUTTON-CLICK");
                if (e.getSlot() == 11) openGUI(p, "END-MENU");
                else if (e.getSlot() == 12) openGUI(p, "NETHER-MENU");
                else if (e.getSlot() == 13) openGUI(p, "GEAR-MENU");
                else if (e.getSlot() == 14) openGUI(p, "FOOD-MENU");
                else if (e.getSlot() == 15) openGUI(p, "SHARD-MENU");
                else if (e.getSlot() == 22) openGUI(p, "EXTRA-1-MENU");
                else if (e.getSlot() == 23) openGUI(p, "EXTRA-2-MENU");
                else if (e.getSlot() == 24) openGUI(p, "EXTRA-3-MENU");
            } else if (id.endsWith("-MENU") && shopConfig != null && shopConfig.contains(id)) {
                if (e.getSlot() == e.getInventory().getSize() - 9 || e.getSlot() == 18 || e.getSlot() == e.getInventory().getSize() - 1 || itemTitle.equalsIgnoreCase("Voltar") || itemTitle.equalsIgnoreCase("ʙᴀᴄᴋ") || itemTitle.equalsIgnoreCase("back") || itemTitle.contains("ᴠᴏʟᴛᴀʀ")) {
                    openGUI(p, "CATEGORIES");
                    return;
                }
                ConfigurationSection shopSec = shopConfig.getConfigurationSection(id);
                if (shopSec != null) {
                    for (String key : shopSec.getKeys(false)) {
                        ConfigurationSection itemSec = shopSec.getConfigurationSection(key);
                        if (itemSec != null && itemSec.getInt("SLOT", -1) == e.getSlot()) {
                            double price = itemSec.getDouble("PRICE-PER-UNIT", 100.0);
                            String currency = itemSec.getString("CURRENCY", "MONEY");
                            ItemStack mat = resolveItem(itemSec.getString("MATERIAL", "STONE"));
                            if (mat == null) mat = new ItemStack(Material.STONE);
                            String dispName = itemSec.getString("DISPLAY-NAME", itemSec.getString("DISPLAY_NAME", itemSec.getString("NAME", itemSec.getString("TITLE", "&a" + key))));
                            ShopPurchase sp = new ShopPurchase(dispName, price, currency, mat, 1);
                            sp.menuName = id;
                            sp.command = itemSec.getString("COMMAND", "");
                            activePurchases.put(p.getUniqueId(), sp);
                            playSound(p, "MENUS.BUTTON-CLICK");
                            openGUI(p, "PURCHASE-SHOP-MENU");
                            break;
                        }
                    }
                }
            } else if (id.equals("SERVER-INFO-MENU")) {
                playSound(p, "MENUS.BUTTON-CLICK");
                if (e.getSlot() == 12) openGUI(p, "RULES-MENU");
                else if (e.getSlot() == 13) openGUI(p, "LEADERBOARDS-MENU");
                else if (e.getSlot() == 14) openGUI(p, "AFK-MENU");
                else if (e.getSlot() == 15) openGUI(p, "SETTINGS-MENU");
                else if (e.getSlot() == 16) openGUI(p, "RTP-MENU");
            } else if (id.equals("RTP-MENU")) {
                p.closeInventory();
                if (e.getSlot() == 11) executeRTP(p, "world");
                else if (e.getSlot() == 13) executeRTP(p, "world_nether");
                else if (e.getSlot() == 15) executeRTP(p, "world_the_end");
            } else if (id.equals("AFK-MENU")) {
                p.closeInventory();
                Location targetLoc = null;
                ConfigurationSection areasSec = menuConfig.getConfigurationSection("AFK-MENU.AREAS");
                if (areasSec != null) {
                    for (String k : areasSec.getKeys(false)) {
                        int slot = areasSec.getInt(k + ".SLOT", -1);
                        if (e.getSlot() == slot) {
                            String locId = areasSec.getString(k + ".LOCATION", k);
                            String cubId = areasSec.getString(k + ".CUBOID", k);
                            targetLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-SPAWNS." + locId, ""));
                            if (targetLoc == null) targetLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-SPAWNS." + cubId, ""));
                            break;
                        }
                    }
                }
                if (targetLoc == null) {
                    ConfigurationSection spawnsSec = getConfig().getConfigurationSection("LOCATIONS.AFK-SPAWNS");
                    if (spawnsSec != null && !spawnsSec.getKeys(false).isEmpty()) {
                        List<String> keys = new ArrayList<>(spawnsSec.getKeys(false));
                        String chosenKey = keys.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(keys.size()));
                        targetLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-SPAWNS." + chosenKey, ""));
                    }
                }
                if (targetLoc == null) targetLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-LOCATION", ""));
                if (targetLoc == null) targetLoc = p.getWorld().getSpawnLocation();
                teleportWithCooldown(p, targetLoc, "AFK");
            } else if (id.equals("SPAWN-MENU")) {
                p.closeInventory();
                Location targetLoc = null;
                ConfigurationSection areasSec = menuConfig.getConfigurationSection("SPAWN-MENU.AREAS");
                if (areasSec != null) {
                    for (String k : areasSec.getKeys(false)) {
                        int slot = areasSec.getInt(k + ".SLOT", -1);
                        if (e.getSlot() == slot) {
                            String locId = areasSec.getString(k + ".LOCATION", k);
                            String cubId = areasSec.getString(k + ".CUBOID", k);
                            targetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-SPAWNS." + locId, ""));
                            if (targetLoc == null) targetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-SPAWNS." + cubId, ""));
                            break;
                        }
                    }
                }
                if (targetLoc == null) {
                    ConfigurationSection spawnsSec = getConfig().getConfigurationSection("LOCATIONS.SPAWN-SPAWNS");
                    if (spawnsSec != null && !spawnsSec.getKeys(false).isEmpty()) {
                        List<String> keys = new ArrayList<>(spawnsSec.getKeys(false));
                        String chosenKey = keys.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(keys.size()));
                        targetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-SPAWNS." + chosenKey, ""));
                    }
                }
                if (targetLoc == null) targetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-LOCATION", ""));
                if (targetLoc == null) targetLoc = p.getWorld().getSpawnLocation();
                teleportWithCooldown(p, targetLoc, "SPAWN");
            } else if (id.equals("LEADERBOARDS-MENU")) {
                if (e.getSlot() == 10) { viewingLeaderboardType.put(p.getUniqueId(), "money"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 11) { viewingLeaderboardType.put(p.getUniqueId(), "shards"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 12) { viewingLeaderboardType.put(p.getUniqueId(), "kills"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 13) { viewingLeaderboardType.put(p.getUniqueId(), "deaths"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 14) { viewingLeaderboardType.put(p.getUniqueId(), "playtime"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 15) { viewingLeaderboardType.put(p.getUniqueId(), "blocksPlaced"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 16) { viewingLeaderboardType.put(p.getUniqueId(), "blocksBroken"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 19) { viewingLeaderboardType.put(p.getUniqueId(), "mobsKilled"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 20) { viewingLeaderboardType.put(p.getUniqueId(), "killStreak"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 21) { viewingLeaderboardType.put(p.getUniqueId(), "highestKillStreak"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 22) { viewingLeaderboardType.put(p.getUniqueId(), "moneySpent"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
                else if (e.getSlot() == 23) { viewingLeaderboardType.put(p.getUniqueId(), "moneyMade"); openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU"); }
            } else if (id.equals("LEADERBOARDS-MENU.TYPE-MENU") || id.equals("TYPE-MENU")) {
                if (e.getSlot() == 45 || itemTitle.equalsIgnoreCase("ʙᴀᴄᴋ") || itemTitle.equalsIgnoreCase("back") || itemTitle.contains("Voltar")) {
                    openGUI(p, "LEADERBOARDS-MENU");
                } else if (e.getSlot() == 47 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingLeaderboardPage.getOrDefault(p.getUniqueId(), 0);
                    if (pg > 0) viewingLeaderboardPage.put(p.getUniqueId(), pg - 1);
                    openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU");
                } else if (e.getSlot() == 48) {
                    boolean asc = viewingLeaderboardAscending.getOrDefault(p.getUniqueId(), false);
                    viewingLeaderboardAscending.put(p.getUniqueId(), !asc);
                    viewingLeaderboardPage.put(p.getUniqueId(), 0);
                    openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU");
                } else if (e.getSlot() == 49) {
                    activeMenuSearch.remove(p.getUniqueId());
                    openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU");
                } else if (e.getSlot() == 50) {
                    p.closeInventory();
                    openSignGUI(p, "LEADERBOARD", "Digite o nome", "(ESC p/ cancelar)");
                } else if (e.getSlot() == 51 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingLeaderboardPage.getOrDefault(p.getUniqueId(), 0);
                    viewingLeaderboardPage.put(p.getUniqueId(), pg + 1);
                    openGUI(p, "LEADERBOARDS-MENU.TYPE-MENU");
                }
            } else if (id.equals("SELL-MENU")) {
                if (e.getSlot() >= 45 && e.getSlot() <= 53) {
                    String cat = "CROPS";
                    if (e.getSlot() == 46 || itemTitle.contains("ᴏʀᴇ") || itemTitle.contains("Ore") || e.getCurrentItem().getType() == Material.DIAMOND) cat = "ORES";
                    else if (e.getSlot() == 47 || itemTitle.contains("ᴍᴏʙ") || itemTitle.contains("Mob") || e.getCurrentItem().getType() == Material.BONE || e.getCurrentItem().getType() == Material.ROTTEN_FLESH) cat = "MOBS";
                    else if (e.getSlot() == 48 || itemTitle.contains("ɴᴀᴛᴜʀᴀʟ") || itemTitle.contains("Natural") || e.getCurrentItem().getType() == Material.OAK_LEAVES || e.getCurrentItem().getType() == Material.OAK_LOG) cat = "NATURAL";
                    else if (e.getSlot() == 49 || itemTitle.contains("ᴀʀᴍᴏʀ") || itemTitle.contains("Armor") || itemTitle.contains("ᴛᴏᴏʟ") || itemTitle.contains("Tool") || e.getCurrentItem().getType() == Material.NETHERITE_HELMET || e.getCurrentItem().getType() == Material.DIAMOND_CHESTPLATE) cat = "ARMOR_AND_TOOLS";
                    else if (e.getSlot() == 50 || itemTitle.contains("ꜰɪѕʜ") || itemTitle.contains("Fish") || e.getCurrentItem().getType() == Material.TROPICAL_FISH || e.getCurrentItem().getType() == Material.COD) cat = "FISH";
                    else if (e.getSlot() == 51 || itemTitle.contains("ʙᴏᴏᴋ") || itemTitle.contains("Book") || itemTitle.contains("ᴇɴᴄʜᴀɴᴛᴇᴅ") || e.getCurrentItem().getType() == Material.BOOK || e.getCurrentItem().getType() == Material.ENCHANTED_BOOK) cat = "BOOK";
                    else if (e.getSlot() == 52 || itemTitle.contains("ᴘᴏᴛɪᴏɴ") || itemTitle.contains("Potion") || e.getCurrentItem().getType() == Material.BREWING_STAND || e.getCurrentItem().getType() == Material.POTION) cat = "POTIONS";
                    else if (e.getSlot() == 53 || itemTitle.contains("ʙʟᴏᴄᴋ") || itemTitle.contains("Block") || e.getCurrentItem().getType() == Material.BRICK || e.getCurrentItem().getType() == Material.GRASS_BLOCK) cat = "BLOCKS";
                    viewingProgressCategory.put(p.getUniqueId(), cat);
                    openGUI(p, "PROGRESS-MENU");
                }
            } else if (id.equals("PROGRESS-MENU")) {
                if (e.getSlot() == 45) {
                    openGUI(p, "SELL-MENU");
                } else if (e.getSlot() == 1) {
                    viewingProgressPage.put(p.getUniqueId(), 0);
                    openGUI(p, "PROGRESS-ITEMS-MENU");
                }
            } else if (id.equals("PROGRESS-ITEMS-MENU")) {
                if (e.getSlot() == 45) {
                    openGUI(p, "PROGRESS-MENU");
                } else if (e.getSlot() == 48 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
                    if (pg > 0) {
                        viewingProgressPage.put(p.getUniqueId(), pg - 1);
                        openGUI(p, "PROGRESS-ITEMS-MENU");
                    }
                } else if (e.getSlot() == 50 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
                    viewingProgressPage.put(p.getUniqueId(), pg + 1);
                    openGUI(p, "PROGRESS-ITEMS-MENU");
                }
            } else if (id.equals("SELL-HISTORY-MENU")) {
                if (e.getSlot() == 45 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
                    if (pg > 0) {
                        viewingProgressPage.put(p.getUniqueId(), pg - 1);
                        playSound(p, "MENUS.BUTTON-CLICK");
                        openGUI(p, "SELL-HISTORY-MENU");
                    }
                } else if (e.getSlot() == 53 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
                    viewingProgressPage.put(p.getUniqueId(), pg + 1);
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "SELL-HISTORY-MENU");
                } else if (e.getSlot() == 48 || itemTitle.equalsIgnoreCase("Tipo") || itemTitle.contains("Tipo") || itemTitle.equalsIgnoreCase("Filtrar") || itemTitle.contains("Filtrar")) {
                    String current = viewingHistorySort.getOrDefault(p.getUniqueId(), "A - Z");
                    String next;
                    if (current.equalsIgnoreCase("A - Z")) next = "Z - A";
                    else if (current.equalsIgnoreCase("Z - A")) next = "Mais vendido";
                    else if (current.equalsIgnoreCase("Mais vendido")) next = "Menos vendido";
                    else next = "A - Z";
                    viewingHistorySort.put(p.getUniqueId(), next);
                    viewingProgressPage.put(p.getUniqueId(), 0);
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "SELL-HISTORY-MENU");
                } else if (e.getSlot() == 49 || itemTitle.contains("Sell History") || itemTitle.contains("Atualizar")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "SELL-HISTORY-MENU");
                } else if (e.getSlot() == 50 || itemTitle.equalsIgnoreCase("Filtro") || itemTitle.contains("Filtro") || itemTitle.contains("filtrar")) {
                    String current = viewingHistoryFilter.getOrDefault(p.getUniqueId(), "Todos");
                    String next;
                    if (current.equalsIgnoreCase("Todos")) next = "Blocos";
                    else if (current.equalsIgnoreCase("Blocos")) next = "Ferramentas";
                    else if (current.equalsIgnoreCase("Ferramentas")) next = "Comida";
                    else if (current.equalsIgnoreCase("Comida")) next = "Combate";
                    else if (current.equalsIgnoreCase("Combate")) next = "Poções";
                    else if (current.equalsIgnoreCase("Poções")) next = "Livros";
                    else if (current.equalsIgnoreCase("Livros")) next = "Ingredientes";
                    else if (current.equalsIgnoreCase("Ingredientes")) next = "Utilidades";
                    else next = "Todos";
                    viewingHistoryFilter.put(p.getUniqueId(), next);
                    viewingProgressPage.put(p.getUniqueId(), 0);
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "SELL-HISTORY-MENU");
                }
            } else if (id.equals("WORTH-MENU")) {
                if (e.getSlot() == 45 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
                    if (pg > 0) {
                        viewingProgressPage.put(p.getUniqueId(), pg - 1);
                        playSound(p, "MENUS.BUTTON-CLICK");
                        openGUI(p, "WORTH-MENU");
                    }
                } else if (e.getSlot() == 53 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
                    viewingProgressPage.put(p.getUniqueId(), pg + 1);
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "WORTH-MENU");
                } else if (e.getSlot() == 48 || itemTitle.equalsIgnoreCase("Tipo") || itemTitle.contains("Tipo") || itemTitle.equalsIgnoreCase("Filtrar") || itemTitle.contains("Filtrar")) {
                    String current = viewingWorthSort.getOrDefault(p.getUniqueId(), "A - Z");
                    String next;
                    if (current.equalsIgnoreCase("A - Z")) {
                        next = "Z - A";
                    } else if (current.equalsIgnoreCase("Z - A")) {
                        next = "Maior preço";
                    } else if (current.equalsIgnoreCase("Maior preço")) {
                        next = "Menor preço";
                    } else {
                        next = "A - Z";
                    }
                    viewingWorthSort.put(p.getUniqueId(), next);
                    viewingProgressPage.put(p.getUniqueId(), 0);
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "WORTH-MENU");
                } else if (e.getSlot() == 49 || itemTitle.equalsIgnoreCase("Worth") || itemTitle.contains("Worth") || itemTitle.contains("Atualizar")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "WORTH-MENU");
                } else if (e.getSlot() == 50 || itemTitle.equalsIgnoreCase("Filtro") || itemTitle.contains("Filtro") || itemTitle.contains("filtrar")) {
                    String current = viewingWorthFilter.getOrDefault(p.getUniqueId(), "Todos");
                    String next;
                    if (current.equalsIgnoreCase("Todos")) next = "Blocos";
                    else if (current.equalsIgnoreCase("Blocos")) next = "Ferramentas";
                    else if (current.equalsIgnoreCase("Ferramentas")) next = "Comida";
                    else if (current.equalsIgnoreCase("Comida")) next = "Combate";
                    else if (current.equalsIgnoreCase("Combate")) next = "Poções";
                    else if (current.equalsIgnoreCase("Poções")) next = "Livros";
                    else if (current.equalsIgnoreCase("Livros")) next = "Ingredientes";
                    else if (current.equalsIgnoreCase("Ingredientes")) next = "Utilidades";
                    else next = "Todos";
                    viewingWorthFilter.put(p.getUniqueId(), next);
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "WORTH-MENU");
                }
            } else if (id.equals("BOUNTIES-MENU")) {
                if (e.getSlot() == 45 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
                    if (pg > 0) {
                        viewingProgressPage.put(p.getUniqueId(), pg - 1);
                        playSound(p, "MENUS.BUTTON-CLICK");
                        openGUI(p, "BOUNTIES-MENU");
                    }
                } else if (e.getSlot() == 53 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                    int pg = viewingProgressPage.getOrDefault(p.getUniqueId(), 0);
                    viewingProgressPage.put(p.getUniqueId(), pg + 1);
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "BOUNTIES-MENU");
                } else if (e.getSlot() == 48 || itemTitle.equalsIgnoreCase("Tipo") || itemTitle.contains("Tipo")) {
                    String current = viewingBountySort.getOrDefault(p.getUniqueId(), "Maior valor");
                    String next;
                    if (current.equalsIgnoreCase("Maior valor")) next = "Menor valor";
                    else if (current.equalsIgnoreCase("Menor valor")) next = "A - Z";
                    else if (current.equalsIgnoreCase("A - Z")) next = "Z - A";
                    else next = "Maior valor";
                    viewingBountySort.put(p.getUniqueId(), next);
                    viewingProgressPage.put(p.getUniqueId(), 0);
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "BOUNTIES-MENU");
                } else if (e.getSlot() == 49 || itemTitle.equalsIgnoreCase("Bounties") || itemTitle.contains("Bounties") || itemTitle.contains("Atualizar")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    openGUI(p, "BOUNTIES-MENU");
                } else if (e.getSlot() == 50 || itemTitle.equalsIgnoreCase("Pesquisar") || itemTitle.contains("Pesquisar") || itemTitle.contains("Search") || itemTitle.contains("Procure")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    p.closeInventory();
                    openSignGUI(p, "BOUNTY", "Digite o nome", "(ESC p/ cancelar)");
                }
            } else if (id.equals("SHOP-SEARCH-RESULTS")) {
                if (e.getSlot() == 49 || itemTitle.equalsIgnoreCase("Voltar") || itemTitle.equalsIgnoreCase("ʙᴀᴄᴋ") || itemTitle.equalsIgnoreCase("back")) {
                    openGUI(p, "CATEGORIES");
                    return;
                }
                if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR && e.getCurrentItem().getType() != Material.BARRIER && shopConfig != null) {
                    for (String menuKey : shopConfig.getKeys(false)) {
                        if (menuKey.endsWith("-MENU")) {
                            ConfigurationSection shopSec = shopConfig.getConfigurationSection(menuKey);
                            if (shopSec != null) {
                                for (String itemKey : shopSec.getKeys(false)) {
                                    if (itemKey.startsWith("ITEM-")) {
                                        ConfigurationSection itemSec = shopSec.getConfigurationSection(itemKey);
                                        if (itemSec != null) {
                                            String matName = itemSec.getString("MATERIAL", "STONE");
                                            ItemStack mat = resolveItem(matName);
                                            if (mat == null) mat = new ItemStack(Material.STONE);
                                            if (mat.getType() == e.getCurrentItem().getType()) {
                                                ShopPurchase sp = new ShopPurchase(itemSec.getString("DISPLAY-NAME", "&aItem"), itemSec.getDouble("PRICE-PER-UNIT", 10.0), itemSec.getString("CURRENCY", "MONEY"), mat, 1);
                                                sp.menuName = menuKey;
                                                sp.command = itemSec.getString("COMMAND", "");
                                                activePurchases.put(p.getUniqueId(), sp);
                                                openGUI(p, "PURCHASE-SHOP-MENU");
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (id.equals("PURCHASE-SHOP-MENU")) {
                ShopPurchase pur = activePurchases.get(p.getUniqueId());
                if (pur == null) { p.closeInventory(); return; }
                // Read RESTRICTIONS for this material
                ConfigurationSection resSec = menuConfig.getConfigurationSection("PURCHASE-SHOP-MENU.RESTRICTIONS." + pur.material.name());
                if (resSec == null) resSec = menuConfig.getConfigurationSection("PURCHASE-SHOP-MENU.RESTRICTIONS.DEFAULT");
                int maxQty = (resSec != null) ? resSec.getInt("MAX_QUANTITY", 64) : 64;
                int minQty = (resSec != null) ? resSec.getInt("MIN_QUANTITY", 1) : 1;
                boolean hideQty = (resSec != null) && resSec.getBoolean("HIDE_QUANTITY_BUTTONS", false);
                if (!hideQty) {
                    if (e.getSlot() == 15) pur.quantity = Math.min(maxQty, pur.quantity + 1);
                    else if (e.getSlot() == 16) pur.quantity = Math.min(maxQty, pur.quantity + 10);
                    else if (e.getSlot() == 17) pur.quantity = maxQty;
                    else if (e.getSlot() == 11) pur.quantity = Math.max(minQty, pur.quantity - 1);
                    else if (e.getSlot() == 10) pur.quantity = Math.max(minQty, pur.quantity - 10);
                    else if (e.getSlot() == 9) pur.quantity = minQty;
                }
                if (e.getSlot() == 21) {
                    String backMenu = (pur.menuName != null && !pur.menuName.isEmpty()) ? pur.menuName : "CATEGORIES";
                    openGUI(p, backMenu);
                    return;
                }
                if (e.getSlot() == 23) {
                    double totalCost = pur.pricePerUnit * pur.quantity;
                    PlayerProfile prof = getProfile(p);
                    boolean hasCommand = (pur.command != null && !pur.command.isEmpty());
                    if (!hasCommand && p.getInventory().firstEmpty() == -1) {
                        p.sendMessage(color(menuConfig.getString("PURCHASE-SHOP-MENU.MESSAGES.ERROR.FULL_INVENTORY", "&cʏᴏᴜʀ ɪɴᴠᴇɴᴛᴏʀʏ ɪѕ ꜰᴜʟʟ.")));
                        playSound(p, "SHOP.NO-MONEY");
                        return;
                    }
                    if (pur.currency.equalsIgnoreCase("SHARD") || pur.currency.equalsIgnoreCase("SHARDS")) {
                        if (prof.shards < (int) totalCost) {
                            p.sendMessage(color(menuConfig.getString("PURCHASE-SHOP-MENU.MESSAGES.ERROR.NO_SHARDS", "&cʏᴏᴜ ᴅᴏɴ'ᴛ ʜᴀᴠᴇ ᴇɴᴏᴜɢʜ ѕʜᴀʀᴅѕ.")));
                            playSound(p, "SHOP.NO-MONEY");
                            return;
                        }
                        prof.shards -= (int) totalCost;
                    } else {
                        if (prof.money < totalCost) {
                            p.sendMessage(color(menuConfig.getString("PURCHASE-SHOP-MENU.MESSAGES.ERROR.NO_MONEY", "&cʏᴏᴜ ᴅᴏɴ'ᴛ ʜᴀᴠᴇ ᴇɴᴏᴜɢʜ ᴍᴏɴᴇʏ.")));
                            playSound(p, "SHOP.NO-MONEY");
                            return;
                        }
                        prof.money -= totalCost;
                        prof.moneySpent += totalCost;
                    }
                    
                    if (hasCommand) {
                        String cmdToRun = pur.command.replace("%player%", p.getName()).replace("%amount%", String.valueOf(pur.quantity));
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdToRun);
                    } else {
                        ItemStack giveItem = pur.itemStack != null ? pur.itemStack.clone() : new ItemStack(pur.material);
                        giveItem.setAmount(pur.quantity);
                        p.getInventory().addItem(giveItem);
                    }
                    String successMsg = pur.currency.equalsIgnoreCase("SHARD") || pur.currency.equalsIgnoreCase("SHARDS") ?
                            menuConfig.getString("PURCHASE-SHOP-MENU.MESSAGES.SUCCESS.SHARDS", "&7You bought {item-name} for &5{amount} shards") :
                            menuConfig.getString("PURCHASE-SHOP-MENU.MESSAGES.SUCCESS.MONEY", "&7You bought &e{quantity} {item-name}&7 for &a${amount}");
                    p.sendMessage(color(successMsg.replace("{quantity}", String.valueOf(pur.quantity)).replace("{item-name}", pur.itemKey).replace("{amount}", String.format("%.2f", totalCost))));
                    playSound(p, "SHOP.BUY-SUCCESS");
                    updateScoreboard(p, prof);
                    openGUI(p, "PURCHASE-SHOP-MENU");
                    return;
                }
                playSound(p, "MENUS.BUTTON-CLICK");
                openGUI(p, "PURCHASE-SHOP-MENU");
            } else if (id.equals("PAY-CONFIRM-MENU")) {
                PendingAction pa = pendingActions.get(p.getUniqueId());
                if (pa == null || e.getSlot() == 10 || e.getSlot() == 11 || itemTitle.toLowerCase().contains("cancel") || itemTitle.contains("Cᴀɴᴄᴇʟ") || itemTitle.contains("Cancelar") || itemTitle.contains("ᴄᴀɴᴄᴇʟ") || itemTitle.contains("ᴄᴀɴᴄᴇʟᴀʀ")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    p.closeInventory();
                } else if (e.getSlot() == 15 || e.getSlot() == 16 || itemTitle.toLowerCase().contains("confirm") || itemTitle.contains("Cᴏɴꜰɪʀᴍ") || itemTitle.contains("Confirmar") || itemTitle.contains("ᴄᴏɴꜰɪʀᴍ") || itemTitle.contains("ᴄᴏɴꜰɪʀᴍᴀʀ")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    p.closeInventory();
                    PlayerProfile prof = getProfile(p);
                    if (prof.money < pa.amount) {
                        p.sendMessage(getMsg("ECONOMY.NOT-ENOUGH-MONEY"));
                        playSound(p, "COMMANDS.ERROR");
                        return;
                    }
                    if (pa.targetUUID.equals(p.getUniqueId())) {
                        p.sendMessage(color("&cVocê não pode enviar dinheiro para si mesmo."));
                        playSound(p, "COMMANDS.ERROR");
                        return;
                    }
                    OfflinePlayer target = Bukkit.getOfflinePlayer(pa.targetUUID);
                    PlayerProfile targetProf = getProfile(target.getUniqueId());
                    prof.money -= pa.amount;
                    targetProf.money += pa.amount;
                    saveProfile(prof);
                    saveProfile(targetProf);
                    updateScoreboard(p, prof);
                    if (target.isOnline()) {
                        Player tp = (Player) target;
                        updateScoreboard(tp, targetProf);
                        if (targetProf.paymentsEnabled) {
                            tp.sendMessage(getMsg("ECONOMY.MONEY-RECEIVED").replace("{player}", p.getName()).replace("{amount}", String.format("%.2f", pa.amount)));
                        }
                    }
                    p.sendMessage(getMsg("ECONOMY.MONEY-SENT").replace("{player}", target.getName() != null ? target.getName() : "Desconhecido").replace("{amount}", String.format("%.2f", pa.amount)));
                    playSound(p, "SELL.LEVEL-UP");
                }
            } else if (id.equals("TPA-CONFIRM-MENU")) {
                PendingAction pa = pendingActions.get(p.getUniqueId());
                if (pa == null || e.getSlot() == 10 || e.getSlot() == 11 || itemTitle.toLowerCase().contains("cancel") || itemTitle.contains("Cᴀɴᴄᴇʟ") || itemTitle.contains("Cancelar") || itemTitle.contains("ᴄᴀɴᴄᴇʟ") || itemTitle.contains("ᴄᴀɴᴄᴇʟᴀʀ")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    p.closeInventory();
                    tpaRequests.remove(p.getUniqueId());
                } else if (e.getSlot() == 15 || e.getSlot() == 16 || itemTitle.toLowerCase().contains("confirm") || itemTitle.contains("Cᴏɴꜰɪʀᴍ") || itemTitle.contains("Confirmar") || itemTitle.contains("ᴄᴏɴꜰɪʀᴍ") || itemTitle.contains("ᴄᴏɴꜰɪʀᴍᴀʀ")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    p.closeInventory();
                    tpaRequests.remove(p.getUniqueId());
                    Player tpaSender = Bukkit.getPlayer(pa.targetUUID);
                    if (tpaSender != null && tpaSender.isOnline()) {
                        tpaSender.sendMessage(getMsg("TPA.ACCEPTED").replace("{player}", p.getName()));
                        p.sendMessage(getMsg("TPA.YOU-ACCEPTED").replace("{player}", tpaSender.getName()));
                        if ("TPAHERE".equals(pa.extraData)) {
                            Location destLoc = pa.targetLocation != null ? pa.targetLocation : tpaSender.getLocation();
                            teleportWithCooldown(p, destLoc, "TPA");
                        } else {
                            teleportToPlayerWithCooldown(tpaSender, p, "TPA");
                        }
                    } else {
                        p.sendMessage(getMsg("TPA.PLAYER-OFFLINE"));
                    }
                }
            } else if (id.equals("BOUNTY-CONFIRM-MENU")) {
                PendingAction pa = pendingActions.get(p.getUniqueId());
                if (pa == null || e.getSlot() == 10 || itemTitle.equalsIgnoreCase("Cancelar") || itemTitle.contains("Cancelar") || itemTitle.contains("cancel") || itemTitle.contains("Cancel")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    p.closeInventory();
                } else if (e.getSlot() == 16 || itemTitle.equalsIgnoreCase("Confirmar") || itemTitle.contains("Confirmar") || itemTitle.contains("confirm") || itemTitle.contains("Confirm")) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    p.closeInventory();
                    PlayerProfile prof = getProfile(p);
                    if (prof.money < pa.amount) {
                        p.sendMessage(getMsg("BOUNTY.NOT-ENOUGH-MONEY"));
                        playSound(p, "COMMANDS.ERROR");
                        return;
                    }
                    prof.money -= pa.amount;
                    saveProfile(prof);
                    updateScoreboard(p, prof);
                    bounties.put(pa.targetUUID, bounties.getOrDefault(pa.targetUUID, 0.0) + pa.amount);
                    OfflinePlayer target = Bukkit.getOfflinePlayer(pa.targetUUID);
                    String addMsg = getMsg("BOUNTY.ADDED")
                            .replace("${amount}", "$" + formatAbbreviated(pa.amount))
                            .replace("{amount}", formatAbbreviated(pa.amount))
                            .replace("{player}", target.getName() != null ? target.getName() : "Desconhecido");
                    broadcastBounty(addMsg);
                    playSound(p, "SELL.LEVEL-UP");
                }
            } else if (id.equals("CONFIRM-MENU")) {
                PendingAction pa = pendingActions.get(p.getUniqueId());
                if (pa == null || e.getSlot() == 11) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    if (pa != null && pa.extraData != null && pa.extraData.startsWith("HOME_DELETE:")) openGUI(p, "HOME-MENU");
                    else p.closeInventory();
                } else if (e.getSlot() == 15) {
                    playSound(p, "MENUS.BUTTON-CLICK");
                    if (pa != null && pa.extraData != null && pa.extraData.startsWith("HOME_DELETE:")) {
                        String hName = pa.extraData.replace("HOME_DELETE:", "");
                        PlayerProfile prof = getProfile(p);
                        prof.homes.remove(hName);
                        saveProfile(prof);
                        p.sendMessage(getMsg("HOMES.DELETE-SUCCESS").replace("{home}", hName));
                        playSound(p, "SELL.LEVEL-UP");
                        openGUI(p, "HOME-MENU");
                    } else {
                        p.closeInventory();
                    }
                }
            } else if (id.equals("TEAM-MENUS.TEAM-EDIT-MEMBER") || id.equals("TEAM-EDIT-MEMBER")) {
                UUID targetUUID = editingTeammate.get(p.getUniqueId());
                PlayerProfile prof = getProfile(p);
                Team team = teams.get(prof.teamName.toLowerCase());
                if (e.getSlot() == 18 || itemTitle.contains("ʙᴀᴄᴋ") || itemTitle.contains("Back") || itemTitle.contains("Voltar")) {
                    openGUI(p, "TEAM-MENUS.TEAM");
                } else if (e.getSlot() == 11 || itemTitle.contains("ᴋɪᴄᴋ") || itemTitle.contains("Kick")) {
                    if (targetUUID != null && targetUUID.equals(p.getUniqueId())) {
                        p.sendMessage(color("&cVocê não pode expulsar a si mesmo!"));
                        return;
                    }
                    if (team != null && targetUUID != null && targetUUID.equals(team.leader)) {
                        p.sendMessage(color("&cVocê não pode expulsar o líder da equipe!"));
                        return;
                    }
                    openGUI(p, "TEAM-MENUS.TEAM-KICK-MEMBER");
                } else if (e.getSlot() == 10 || e.getSlot() == 12 || e.getSlot() == 13 || e.getSlot() == 14 || e.getSlot() == 15 || e.getSlot() == 16) {
                    if (targetUUID != null) {
                        PlayerProfile targetProf = getProfile(targetUUID);
                        if (targetProf != null) {
                            if (e.getSlot() == 10) targetProf.permEditHome = !targetProf.permEditHome;
                            else if (e.getSlot() == 12) targetProf.permManageTeammates = !targetProf.permManageTeammates;
                            else if (e.getSlot() == 13) targetProf.permTogglePvp = !targetProf.permTogglePvp;
                            else if (e.getSlot() == 14) targetProf.permVisitHome = !targetProf.permVisitHome;
                            else if (e.getSlot() == 15) targetProf.permTeamChat = !targetProf.permTeamChat;
                            else if (e.getSlot() == 16) {
                                if (team == null || !team.leader.equals(p.getUniqueId())) {
                                    p.sendMessage(color("&cApenas o líder do team pode alterar a permissão de Helper!"));
                                    return;
                                }
                                targetProf.permHelper = !targetProf.permHelper;
                            }
                            saveProfile(targetProf);
                            playSound(p, "MENUS.BUTTON-CLICK");
                            openGUI(p, "TEAM-MENUS.TEAM-EDIT-MEMBER");
                        }
                    }
                }
            } else if (id.equals("TEAM-MENUS.TEAM-KICK-MEMBER") || id.equals("TEAM-KICK-MEMBER")) {
                if (e.getSlot() == 11 || itemTitle.contains("ᴄᴀɴᴄᴇʟ") || itemTitle.contains("Cancel") || itemTitle.contains("Cancelar")) {
                    openGUI(p, "TEAM-MENUS.TEAM-EDIT-MEMBER");
                } else if (e.getSlot() == 15 || itemTitle.contains("ᴄᴏɴꜰɪʀᴍ") || itemTitle.contains("Confirm")) {
                    UUID targetUUID = editingTeammate.get(p.getUniqueId());
                    PlayerProfile prof = getProfile(p);
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (team != null && targetUUID != null) {
                        if (targetUUID.equals(p.getUniqueId())) {
                            p.sendMessage(color("&cVocê não pode expulsar a si mesmo!"));
                            p.closeInventory();
                            return;
                        }
                        if (targetUUID.equals(team.leader)) {
                            p.sendMessage(color("&cVocê não pode expulsar o líder da equipe!"));
                            p.closeInventory();
                            return;
                        }
                        team.members.remove(targetUUID);
                        PlayerProfile tp = profiles.get(targetUUID);
                        if (tp != null) tp.teamName = "";
                        OfflinePlayer op = Bukkit.getOfflinePlayer(targetUUID);
                        p.sendMessage(color("&c" + (op.getName() != null ? op.getName() : "Jogador") + " foi removido da equipe."));
                        if (op.isOnline()) ((Player)op).sendMessage(color("&cVocê foi removido da equipe " + team.name + "."));
                    }
                    openGUI(p, "TEAM-MENUS.TEAM");
                }
            } else if (id.equals("TEAM-MENUS.TEAM-DISBAND") || id.equals("TEAM-DISBAND")) {
                if (e.getSlot() == 11 || itemTitle.contains("ᴄᴀɴᴄᴇʟ") || itemTitle.contains("Cancel") || itemTitle.contains("Cancelar")) {
                    openGUI(p, "TEAM-MENUS.TEAM");
                } else if (e.getSlot() == 15 || itemTitle.contains("ᴄᴏɴꜰɪʀᴍ") || itemTitle.contains("Confirm")) {
                    p.closeInventory();
                    PlayerProfile prof = getProfile(p);
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (team != null) {
                        teams.remove(team.name.toLowerCase());
                        teams.remove(team.name);
                        deleteTeamFile(team.name);
                        saveTeams();
                        p.sendMessage(getMsg("TEAM.TEAM-DISBANDED"));
                        for (UUID m : team.members) {
                            PlayerProfile mp = profiles.get(m);
                            if (mp != null) mp.teamName = "";
                            Player mPlayer = Bukkit.getPlayer(m);
                            if (mPlayer != null && !mPlayer.equals(p)) mPlayer.sendMessage(getMsg("TEAM.TEAM-DISBANDED"));
                        }
                        updateAllTablists();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent e) {
        if (e.getEntity().getWorld() == null) return;
        boolean isPhantom = e.getEntityType() == org.bukkit.entity.EntityType.PHANTOM;
        boolean isMonster = e.getEntity() instanceof org.bukkit.entity.Monster || e.getEntity() instanceof org.bukkit.entity.Slime || isPhantom;
        if (!isMonster) return;

        for (Player p : e.getEntity().getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(e.getLocation()) <= 64 * 64) {
                PlayerProfile prof = getProfile(p);
                if (prof != null) {
                    if (isPhantom && prof.phantomDisabled) {
                        e.setCancelled(true);
                        return;
                    }
                    if (prof.disableMobSpawn && e.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        activeMenuSearch.remove(e.getPlayer().getUniqueId());
        viewingStatsProfile.remove(e.getPlayer().getUniqueId());
        viewingPlayerManagerProfile.remove(e.getPlayer().getUniqueId());

        if (e.getInventory().getHolder() instanceof GUIHolder) {
            GUIHolder holder = (GUIHolder) e.getInventory().getHolder();
            if (holder.getMenuId().equals("SELL-MENU")) {
                Player p = (Player) e.getPlayer();
                sellInventory(p, e.getInventory());
            } else if (holder.getMenuId().startsWith("SIX-ROW-ENDERCHEST")) {
                Player p = (Player) e.getPlayer();
                try { p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 1.0f); } catch (Throwable ignored) {}
                if (activeEnderChestBlocks.containsKey(p.getUniqueId())) {
                    Location loc = activeEnderChestBlocks.remove(p.getUniqueId());
                    if (loc != null && loc.getWorld() != null) {
                        Bukkit.getScheduler().runTask(this, () -> {
                            try {
                                if (loc.getBlock().getState() instanceof org.bukkit.block.Lidded) {
                                    ((org.bukkit.block.Lidded) loc.getBlock().getState()).close();
                                }
                            } catch (Throwable ignored) {}
                        });
                    }
                }
                UUID targetUuid = e.getPlayer().getUniqueId();
                if (holder.getMenuId().contains(":")) {
                    try { targetUuid = UUID.fromString(holder.getMenuId().split(":")[1]); } catch (Exception ignored) {}
                }
                PlayerProfile prof = profiles.get(targetUuid);
                if (prof != null) {
                    for (int i = 0; i < 54; i++) {
                        prof.enderchestContents[i] = e.getInventory().getItem(i);
                    }
                    Player targetPlayer = Bukkit.getPlayer(targetUuid);
                    if (targetPlayer != null && targetPlayer.getEnderChest() != null) {
                        for (int i = 0; i < 27 && i < e.getInventory().getSize(); i++) {
                            targetPlayer.getEnderChest().setItem(i, e.getInventory().getItem(i));
                        }
                    }
                    saveProfile(prof);
                }
            } else if (holder.getMenuId().equals("PAY-CONFIRM-MENU") || holder.getMenuId().equals("TPA-CONFIRM-MENU") || holder.getMenuId().equals("BOUNTY-CONFIRM-MENU") || holder.getMenuId().equals("CONFIRM-MENU")) {
                pendingActions.remove(e.getPlayer().getUniqueId());
            }
        }
    }

    private Location parseLocation(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            String[] pts = str.split(",");
            World w = Bukkit.getWorld(pts[0]);
            if (w == null) return null;
            double x = Double.parseDouble(pts[1]);
            double y = Double.parseDouble(pts[2]);
            double z = Double.parseDouble(pts[3]);
            float yaw = pts.length > 4 ? Float.parseFloat(pts[4]) : 0f;
            float pitch = pts.length > 5 ? Float.parseFloat(pts[5]) : 0f;
            return new Location(w, x, y, z, yaw, pitch);
        } catch (Exception e) { return null; }
    }

    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private void handleWarpManagerCommand(Player p, String[] args) {
        if (args.length == 0) {
            p.sendMessage(color("&cUso correto: /warpmanager <create|delete|list> [nome]"));
            return;
        }
        String action = args[0];
        if (action.equalsIgnoreCase("create")) {
            if (args.length < 2) {
                p.sendMessage(color("&cUso correto: /warpmanager create <nome>"));
                return;
            }
            String warpName = args[1].toLowerCase();
            getWarpsConfig().set("WARPS." + warpName, serializeLocation(p.getLocation()));
            saveWarps();
            p.sendMessage(color("&7Warp &#FFF000" + args[1] + " &7criada com sucesso!"));
        } else if (action.equalsIgnoreCase("delete") || action.equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                p.sendMessage(color("&cUso correto: /warpmanager delete <nome>"));
                return;
            }
            String warpName = args[1].toLowerCase();
            if (getWarpsConfig().getString("WARPS." + warpName) == null) {
                p.sendMessage(color("&cEsta warp não existe."));
                return;
            }
            getWarpsConfig().set("WARPS." + warpName, null);
            if (dbManager != null && dbManager.isSql()) {
                dbManager.deleteWarp(warpName);
            }
            saveWarps();
            p.sendMessage(color("&7Warp &#FFF000" + args[1] + " &7deletada com sucesso!"));
        } else if (action.equalsIgnoreCase("list")) {
            ConfigurationSection sec = getWarpsConfig().getConfigurationSection("WARPS");
            if (sec == null || sec.getKeys(false).isEmpty()) {
                p.sendMessage(color("&cNenhuma warp criada no momento."));
            } else {
                String list = String.join("&7, &#FFF000", sec.getKeys(false));
                p.sendMessage(color("&8&m----------------------------------------"));
                p.sendMessage(color("&b&lWarps criadas: &f(" + sec.getKeys(false).size() + ")"));
                p.sendMessage(color("&#FFF000" + list));
                p.sendMessage(color("&8&m----------------------------------------"));
            }
        } else {
            p.sendMessage(color("&cUso correto: /warpmanager <create|delete|list> [nome]"));
        }
    }

    private void handleWarpCommand(Player p, String[] args) {
        if (args.length == 0) {
            ConfigurationSection sec = getWarpsConfig().getConfigurationSection("WARPS");
            if (sec == null || sec.getKeys(false).isEmpty()) {
                p.sendMessage(color("&cNenhuma warp disponível no momento."));
                return;
            }
            String list = String.join("&7, &#FFF000", sec.getKeys(false));
            p.sendMessage(color("&8&m----------------------------------------"));
            p.sendMessage(color("&b&lWarps Disponíveis:"));
            p.sendMessage(color("&#FFF000" + list));
            p.sendMessage(color("&7Use &e/warp <nome> &7para se teleportar."));
            p.sendMessage(color("&8&m----------------------------------------"));
            return;
        }
        String warpName = args[0].toLowerCase();
        String locStr = getWarpsConfig().getString("WARPS." + warpName);
        if (locStr == null) {
            p.sendMessage(color("&cWarp não encontrada. Use &e/warp &cpara ver a lista de warps disponíveis."));
            return;
        }
        Location loc = parseLocation(locStr);
        if (loc == null) {
            p.sendMessage(color("&cO local desta warp está inválido ou o mundo foi deletado."));
            return;
        }
        teleportWithCooldown(p, loc, "WARP");
    }

    private void giveWandItem(Player p) {
        ItemStack wand = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&eWand"));
            meta.setLore(Arrays.asList(
                color("&eBotão esquerdo &f- Pos 1"),
                color("&eBotão direito &f- Pos 2")
            ));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "afk_wand");
            wand.setItemMeta(meta);
        }
        p.getInventory().addItem(wand);
        p.sendMessage(color("&a✔ Você recebeu a Wand (&eGolden Hoe&a)!"));
    }

    private void sendPrivateMessage(Player sender, Player target, String message) {
        PlayerProfile targetProf = getProfile(target);
        if (targetProf != null && !sender.hasPermission("donutcore.admin")) {
            if (targetProf.blockTell || targetProf.ignoredPlayers.contains(sender.getUniqueId())) {
                sender.sendMessage(color("&cEste jogador está com o tell bloqueado"));
                return;
            }
        }
        PlayerProfile senderProf = getProfile(sender);
        if (senderProf != null && senderProf.blockTell && !sender.hasPermission("donutcore.admin")) {
            sender.sendMessage(color("&cVocê está com o tell bloqueado! Use /blocktell para desbloquear."));
            return;
        }

        lastMessaged.put(sender.getUniqueId(), target.getUniqueId());
        lastMessaged.put(target.getUniqueId(), sender.getUniqueId());

        String sentFormat = getMsg("PRIVATE-MESSAGE.SENT");
        if (sentFormat == null || sentFormat.isEmpty() || sentFormat.equals("PRIVATE-MESSAGE.SENT")) {
            sentFormat = "&#FFF000Você &7→ &#FFF000&n{player}&f: {message}";
        }
        String recFormat = getMsg("PRIVATE-MESSAGE.RECEIVED");
        if (recFormat == null || recFormat.isEmpty() || recFormat.equals("PRIVATE-MESSAGE.RECEIVED")) {
            recFormat = "&#FFF000&n{player} &7→ &#FFF000Você &f: {message}";
        }

        sender.sendMessage(color(sentFormat.replace("{player}", target.getName()).replace("{message}", message)));
        target.sendMessage(color(recFormat.replace("{player}", sender.getName()).replace("{message}", message)));
    }

    // ==========================================
    // COMMAND EXECUTOR & TAB COMPLETER
    // ==========================================
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {


        if ((cmd.getName().equalsIgnoreCase("donutcore") || cmd.getName().equalsIgnoreCase("dcore") || label.equalsIgnoreCase("dcore") || label.equalsIgnoreCase("donutcore")) && args.length > 0 && args[0].equalsIgnoreCase("amethyst")) {
            if (args.length < 2) {
                sender.sendMessage(color("&cUso correto: /dcore amethyst <pickaxe|axe|sellaxe|shovel|bucket|booster> <jogador> <duração>"));
                sender.sendMessage(color("&7Exemplo: /dcore amethyst PICKAXE Theus 1d"));
                return true;
            }
            String toolType = args[1];
            Player target = null;
            long durationSeconds = 0;
            String durationText = "Permanente";

            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
            } else if (sender instanceof Player) {
                target = (Player) sender;
            }
            if (target == null && args.length == 3 && sender instanceof Player) {
                long parsed = parseDurationSeconds(args[2]);
                if (parsed > 0) {
                    target = (Player) sender;
                    durationSeconds = parsed;
                    durationText = args[2];
                }
            }
            if (target == null) {
                sender.sendMessage(color("&cJogador não encontrado ou não especificado! Uso: /dcore amethyst <tool> <player> <duration>"));
                return true;
            }
            if (args.length >= 4) {
                long parsed = parseDurationSeconds(args[3]);
                if (parsed > 0) {
                    durationSeconds = parsed;
                    durationText = args[3];
                } else {
                    durationText = args[3];
                }
            }

            ItemStack tool = createAmethystTool(toolType, durationSeconds, durationText);
            target.getInventory().addItem(tool);
            return true;
        }

        String cmdNameLower = cmd.getName().toLowerCase();
        boolean isDCore = cmdNameLower.equals("donutcore") || cmdNameLower.equals("dcore") || cmdNameLower.equals("dccore") || label.equalsIgnoreCase("dcore") || label.equalsIgnoreCase("donutcore") || label.equalsIgnoreCase("dccore");

        if (isDCore && args.length > 0 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
            if (!sender.hasPermission("donutcore.admin") && !sender.isOp()) {
                sender.sendMessage(getMsg("GENERAL.NO-PERMISSION"));
                return true;
            }
            reloadPlugin(sender);
            return true;
        }

        if (cmdNameLower.equals("shardmanager") || (isDCore && args.length > 0 && args[0].equalsIgnoreCase("shardmanager"))) {
            if (!sender.hasPermission("donutcore.admin") && !sender.isOp()) {
                sender.sendMessage(getMsg("GENERAL.NO-PERMISSION"));
                return true;
            }
            String[] subArgs = cmdNameLower.equals("shardmanager")
                    ? args
                    : Arrays.copyOfRange(args, 1, args.length);
            return handleShardManagerCommand(sender, subArgs);
        }

        if (cmdNameLower.equals("moneymanager") || (isDCore && args.length > 0 && args[0].equalsIgnoreCase("moneymanager"))) {
            if (!sender.hasPermission("donutcore.admin") && !sender.isOp()) {
                sender.sendMessage(getMsg("GENERAL.NO-PERMISSION"));
                return true;
            }
            String[] subArgs = cmdNameLower.equals("moneymanager")
                    ? args
                    : Arrays.copyOfRange(args, 1, args.length);
            return handleMoneyManagerCommand(sender, subArgs);
        }

        if (cmdNameLower.equals("playermanager") || (isDCore && args.length > 0 && args[0].equalsIgnoreCase("playermanager"))) {
            if (!sender.hasPermission("donutcore.admin") && !sender.hasPermission("donutcore.playermanager") && !sender.isOp()) {
                sender.sendMessage(getMsg("GENERAL.NO-PERMISSION"));
                return true;
            }
            String[] subArgs = cmdNameLower.equals("playermanager")
                    ? args
                    : Arrays.copyOfRange(args, 1, args.length);
            return handlePlayerManagerCommand(sender, subArgs);
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("GENERAL.PLAYER-ONLY"));
            return true;
        }
        Player p = (Player) sender;
        PlayerProfile prof = getProfile(p);
        String name = cmd.getName().toLowerCase();

        switch (name) {
            case "ec":
            case "echest":
                if (!p.hasPermission("donutcore.enderchest")) {
                    p.sendMessage(color("&cVocê não tem permissão para usar este comando."));
                    return true;
                }
                if (args.length > 0) {
                    if (p.hasPermission("donutcore.admin") || p.hasPermission("donutcore.endersee") || p.isOp()) {
                        p.sendMessage(color("&cUso correto para ver o Ender Chest de outro jogador: &e/endersee <jogador>"));
                    } else {
                        p.sendMessage(color("&cUso correto: /ec ou /echest"));
                    }
                    return true;
                }
                openCustomEnderChest(p, p);
                return true;

            case "endersee":
                if (!p.hasPermission("donutcore.admin") && !p.hasPermission("donutcore.endersee") && !p.isOp()) {
                    p.sendMessage(color("&cVocê não tem permissão para abrir o Ender Chest de outros jogadores."));
                    return true;
                }
                if (args.length < 1) {
                    p.sendMessage(color("&cUso correto: /endersee <jogador>"));
                    return true;
                }
                Player targetEC = Bukkit.getPlayer(args[0]);
                if (targetEC == null) {
                    p.sendMessage(color("&cJogador não encontrado online."));
                    return true;
                }
                openCustomEnderChest(p, targetEC);
                return true;

            case "warp":
                handleWarpCommand(p, args);
                break;

            case "warpmanager":
                if (!p.hasPermission("donutcore.admin") && !p.isOp()) {
                    p.sendMessage(getMsg("GENERAL.NO-PERMISSION"));
                    return true;
                }
                handleWarpManagerCommand(p, args);
                break;

            case "setspawn":
                if (!p.hasPermission("donutcore.admin")) {
                    p.sendMessage(color("&cVocê não tem permissão para usar este comando."));
                    return true;
                }
                setServerSpawn(p);
                break;

            case "spawn":
                if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
                    if (!p.hasPermission("donutcore.admin")) {
                        p.sendMessage(color("&cVocê não tem permissão para usar este comando."));
                        return true;
                    }
                    String spawnName = (args.length > 1) ? args[1] : "1";
                    setSpawnCommandPoint(p, spawnName);
                    return true;
                }
                if (args.length > 0 || !getConfig().getBoolean("SETTINGS.SPAWN-MENU", true)) {
                    Location targetLoc = null;
                    if (args.length > 0) {
                        String key = args[0].toLowerCase();
                        targetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-SPAWNS." + key, ""));
                        if (targetLoc == null && (key.equals("1") || key.equals("2"))) {
                            targetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-SPAWNS.spawn" + key, ""));
                        }
                        if (targetLoc == null && (key.equals("spawn1") || key.equals("spawn2"))) {
                            targetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-SPAWNS." + key.replace("spawn", ""), ""));
                        }
                        if (targetLoc == null && (key.equals("1") || key.equals("2") || key.startsWith("spawn"))) {
                            p.sendMessage(color("&cO ponto de spawn &f#" + args[0] + " &cainda não foi definido no servidor! Teleportando para o spawn principal..."));
                        }
                    }
                    if (targetLoc == null) {
                        ConfigurationSection spawnsSec = getConfig().getConfigurationSection("LOCATIONS.SPAWN-SPAWNS");
                        if (spawnsSec != null && !spawnsSec.getKeys(false).isEmpty()) {
                            List<String> keys = new ArrayList<>(spawnsSec.getKeys(false));
                            targetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-SPAWNS." + keys.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(keys.size())), ""));
                        }
                    }
                    if (targetLoc == null) targetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-LOCATION", ""));
                    if (targetLoc != null) {
                        teleportWithCooldown(p, targetLoc, "SPAWN");
                    } else {
                        p.sendMessage(color("&cO spawn ainda não foi definido."));
                    }
                } else {
                    openGUI(p, "SPAWN-MENU");
                }
                break;

            case "afk":
                if (args.length > 0 || !getConfig().getBoolean("SETTINGS.AFK-MENU", true)) {
                    Location targetLoc = null;
                    if (args.length > 0) targetLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-SPAWNS." + args[0], ""));
                    if (targetLoc == null) {
                        ConfigurationSection spawnsSec = getConfig().getConfigurationSection("LOCATIONS.AFK-SPAWNS");
                        if (spawnsSec != null && !spawnsSec.getKeys(false).isEmpty()) {
                            List<String> keys = new ArrayList<>(spawnsSec.getKeys(false));
                            targetLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-SPAWNS." + keys.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(keys.size())), ""));
                        }
                    }
                    if (targetLoc == null) targetLoc = parseLocation(getConfig().getString("LOCATIONS.AFK-LOCATION", ""));
                    if (targetLoc != null) {
                        teleportWithCooldown(p, targetLoc, "AFK");
                    } else {
                        p.sendMessage(color("&cA área AFK ainda não foi definida."));
                    }
                } else {
                    openGUI(p, "AFK-MENU");
                }
                break;

            case "team":
                if (args.length == 0) {
                    openGUI(p, "TEAM-MENUS.TEAM");
                    return true;
                }
                String sub = args[0].toLowerCase();
                if (sub.equals("create")) {
                    if (args.length < 2) {
                        p.sendMessage(color("&cUso: /team create <nome>"));
                        return true;
                    }
                    if (!prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.ALREADY-IN-TEAM"));
                        return true;
                    }
                    String tName = args[1];
                    int minLen = getConfig().getInt("TEAM.NAME-MIN-LENGTH", 3);
                    int maxLen = getConfig().getInt("TEAM.NAME-MAX-LENGTH", 5);
                    List<String> restricted = getConfig().getStringList("TEAM.RESTRICTED-NAMES");
                    boolean isRestricted = restricted.stream().anyMatch(r -> r.equalsIgnoreCase(tName));
                    if (tName.length() > maxLen || tName.length() < minLen || !tName.matches("^[a-zA-Z0-9_]+$") || isRestricted) {
                        p.sendMessage(getMsg("TEAM.RESTRICTED-NAME"));
                        return true;
                    }
                    if (teams.containsKey(tName.toLowerCase())) {
                        p.sendMessage(getMsg("TEAM.ALREADY-EXISTS"));
                        return true;
                    }
                    Team team = new Team(tName, p.getUniqueId());
                    teams.put(tName.toLowerCase(), team);
                    prof.teamName = tName;
                    p.sendMessage(getMsg("TEAM.TEAM-CREATED"));
                    updateAllTablists();
                } else if (sub.equals("home")) {
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        return true;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (!team.leader.equals(p.getUniqueId()) && !prof.permHelper && !prof.permVisitHome) {
                        p.sendMessage(color("&cVocê não tem permissão para visitar a home da equipe."));
                        return true;
                    }
                    if (team.home == null) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM-HOME"));
                    } else {
                        teleportWithCooldown(p, team.home, "TEAM-HOME");
                    }
                } else if (sub.equals("sethome")) {
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        return true;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (!team.leader.equals(p.getUniqueId()) && !prof.permHelper && !prof.permEditHome) {
                        p.sendMessage(color("&cVocê não tem permissão para definir a home da equipe."));
                        return true;
                    }
                    team.home = p.getLocation();
                    p.sendMessage(getMsg("TEAM.TEAM-HOME-SET"));
                } else if (sub.equals("delhome")) {
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        return true;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (!team.leader.equals(p.getUniqueId()) && !prof.permHelper && !prof.permEditHome) {
                        p.sendMessage(color("&cVocê não tem permissão para deletar a home da equipe."));
                        return true;
                    }
                    if (team.home == null) {
                        p.sendMessage(color("&cVocê não tem nenhuma home para deletar."));
                        return true;
                    }
                    team.home = null;
                    p.sendMessage(getMsg("TEAM.TEAM-HOME-DELETED"));
                } else if (sub.equals("pvp")) {
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        return true;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (!team.leader.equals(p.getUniqueId()) && !prof.permHelper && !prof.permTogglePvp) {
                        p.sendMessage(color("&cVocê não tem permissão para alternar o PvP da equipe."));
                        return true;
                    }
                    team.pvp = !team.pvp;
                } else if (sub.equals("invite")) {
                    if (args.length < 2) {
                        p.sendMessage(color("&cUso: /team invite <jogador>"));
                        return true;
                    }
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        return true;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (!team.leader.equals(p.getUniqueId()) && !prof.permHelper && !prof.permManageTeammates) {
                        p.sendMessage(color("&cVocê não tem permissão para convidar jogadores para a equipe."));
                        return true;
                    }
                    Player tPlayer = Bukkit.getPlayer(args[1]);
                    if (tPlayer == null) {
                        p.sendMessage(getMsg("GENERAL.PLAYER-NOT-FOUND"));
                        return true;
                    }
                    if (tPlayer.equals(p)) {
                        p.sendMessage(color("&cVocê não pode convidar a si mesmo."));
                        return true;
                    }
                    PlayerProfile targetProf = getProfile(tPlayer);
                    if (!targetProf.teamInvitesEnabled) {
                        p.sendMessage(getMsg("TEAM.PLAYER-NO-INVITES"));
                        return true;
                    }
                    if (!targetProf.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.PLAYER-IN-TEAM").replace("{player}", tPlayer.getName()));
                        return true;
                    }
                    int maxMembers = getConfig().getInt("TEAM.LIMIT-MEMBERS", getConfig().getInt("SETTINGS.TEAM-MAX-MEMBERS", 10));
                    if (team.members.size() >= maxMembers) {
                        p.sendMessage(getMsg("TEAM.TEAM-FULL"));
                        return true;
                    }
                    teamInvites.put(tPlayer.getUniqueId(), team.name);
                    p.sendMessage(getMsg("TEAM.INVITE-SENT").replace("{player}", tPlayer.getName()));
                    tPlayer.sendMessage(getMsg("TEAM.INVITED-TO-JOIN").replace("{team}", team.name));
                    try {
                        net.kyori.adventure.text.Component clickMsg = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(getMsg("TEAM.CLICK-TO-JOIN"))
                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/team join " + team.name))
                                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(getMsg("TEAM.HOVER-JOIN").replace("{team}", team.name))));
                        tPlayer.sendMessage(clickMsg);
                    } catch (Throwable ex) {
                        try {
                            net.md_5.bungee.api.chat.TextComponent clickMsg = new net.md_5.bungee.api.chat.TextComponent(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', getMsg("TEAM.CLICK-TO-JOIN")));
                            clickMsg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/team join " + team.name));
                            clickMsg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', getMsg("TEAM.HOVER-JOIN").replace("{team}", team.name))).create()));
                            tPlayer.spigot().sendMessage(clickMsg);
                        } catch (Throwable ex2) {
                            tPlayer.sendMessage(color(getMsg("TEAM.CLICK-TO-JOIN") + " - /team join " + team.name));
                        }
                    }
                    tPlayer.sendMessage(getMsg("TEAM.OR-TYPE-COMMAND").replace("{command}", "/team join " + team.name));
                } else if (sub.equals("join")) {
                    if (args.length < 2) {
                        p.sendMessage(color("&cUso: /team join <equipe>"));
                        return true;
                    }
                    if (!prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.ALREADY-IN-TEAM"));
                        return true;
                    }
                    String tName = args[1];
                    Team team = teams.get(tName.toLowerCase());
                    if (team == null) {
                        p.sendMessage(getMsg("TEAM.TEAM-NOT-EXIST"));
                        return true;
                    }
                    String invTeam = teamInvites.get(p.getUniqueId());
                    if (invTeam == null || !invTeam.equalsIgnoreCase(tName)) {
                        p.sendMessage(getMsg("TEAM.NO-PENDING-INVITES").replace("{team}", tName));
                        return true;
                    }
                    int maxMembers = getConfig().getInt("TEAM.LIMIT-MEMBERS", getConfig().getInt("SETTINGS.TEAM-MAX-MEMBERS", 10));
                    if (team.members.size() >= maxMembers) {
                        p.sendMessage(getMsg("TEAM.TEAM-FULL"));
                        return true;
                    }
                    teamInvites.remove(p.getUniqueId());
                    team.members.add(p.getUniqueId());
                    prof.teamName = team.name;
                    p.sendMessage(getMsg("TEAM.JOIN-SUCCESS").replace("{team}", team.name));
                    for (UUID m : team.members) {
                        Player mp = Bukkit.getPlayer(m);
                        if (mp != null && !mp.equals(p)) mp.sendMessage(getMsg("TEAM.JOINED-BROADCAST").replace("{player}", p.getName()));
                    }
                    updateAllTablists();
                } else if (sub.equals("leave")) {
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        return true;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (team != null) {
                        team.members.remove(p.getUniqueId());
                        prof.teamName = "";
                        if (team.members.isEmpty() || team.leader.equals(p.getUniqueId())) {
                            teams.remove(team.name.toLowerCase());
                            teams.remove(team.name);
                            deleteTeamFile(team.name);
                            saveTeams();
                            p.sendMessage(getMsg("TEAM.TEAM-DISBANDED"));
                            for (UUID m : team.members) {
                                PlayerProfile mp = profiles.get(m);
                                if (mp != null) mp.teamName = "";
                                Player mPlayer = Bukkit.getPlayer(m);
                                if (mPlayer != null && !mPlayer.equals(p)) mPlayer.sendMessage(getMsg("TEAM.TEAM-DISBANDED"));
                            }
                        } else {
                            p.sendMessage(color("&7Você saiu da equipe."));
                            for (UUID m : team.members) {
                                Player mp = Bukkit.getPlayer(m);
                                if (mp != null) mp.sendMessage(color("&c" + p.getName() + " saiu da equipe."));
                            }
                        }
                        updateAllTablists();
                    }
                } else if (sub.equals("disband")) {
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        return true;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (!team.leader.equals(p.getUniqueId())) {
                        p.sendMessage(getMsg("TEAM.NOT-LEADER"));
                        return true;
                    }
                    openGUI(p, "TEAM-MENUS.TEAM-DISBAND");
                } else if (sub.equals("kick")) {
                    if (args.length < 2) {
                        p.sendMessage(color("&cUso: /team kick <jogador>"));
                        return true;
                    }
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        return true;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (!team.leader.equals(p.getUniqueId()) && !prof.permHelper && !prof.permManageTeammates) {
                        p.sendMessage(color("&cVocê não tem permissão para expulsar membros da equipe."));
                        return true;
                    }
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                    if (target.getUniqueId().equals(p.getUniqueId())) {
                        p.sendMessage(getMsg("TEAM.CANT-KICK-SELF"));
                        return true;
                    }
                    if (target.getUniqueId().equals(team.leader)) {
                        p.sendMessage(color("&cVocê não pode expulsar o líder da equipe!"));
                        return true;
                    }
                    if (!team.members.contains(target.getUniqueId())) {
                        p.sendMessage(getMsg("TEAM.PLAYER-NOT-IN-TEAM").replace("{player}", args[1]));
                        return true;
                    }
                    team.members.remove(target.getUniqueId());
                    PlayerProfile tp = profiles.get(target.getUniqueId());
                    if (tp != null) tp.teamName = "";
                    String tName = target.getName() != null ? target.getName() : args[1];
                    p.sendMessage(getMsg("TEAM.KICK-SUCCESS").replace("{player}", tName));
                    if (target.isOnline()) ((Player)target).sendMessage(getMsg("TEAM.KICKED-FROM-TEAM"));
                    updateAllTablists();
                } else if (sub.equals("chat") || sub.equals("c")) {
                    if (prof.teamName.isEmpty()) {
                        p.sendMessage(getMsg("TEAM.NO-TEAM"));
                        return true;
                    }
                    Team team = teams.get(prof.teamName.toLowerCase());
                    if (!team.teamChat && !team.leader.equals(p.getUniqueId()) && !prof.permHelper && !prof.permTeamChat) {
                        p.sendMessage(color("&cVocê não tem permissão para falar no chat do team"));
                        return true;
                    }
                    team.teamChat = !team.teamChat;
                    if (team.teamChat) {
                        p.sendMessage(getMsg("TEAM.TEAM-CHAT-ENABLED"));
                    } else {
                        p.sendMessage(getMsg("TEAM.TEAM-CHAT-DISABLED"));
                    }
                } else {
                    p.sendMessage(color("&e&lComandos da Equipe:"));
                    p.sendMessage(color("&7/team create <nome> &f- Criar equipe"));
                    p.sendMessage(color("&7/team invite <jogador> &f- Convidar jogador"));
                    p.sendMessage(color("&7/team join <equipe> &f- Entrar na equipe"));
                    p.sendMessage(color("&7/team leave &f- Sair da equipe"));
                    p.sendMessage(color("&7/team kick <jogador> &f- Expulsar jogador"));
                    p.sendMessage(color("&7/team disband &f- Desfazer equipe (Líder)"));
                    p.sendMessage(color("&7/team home &f- Teleportar para home da equipe"));
                    p.sendMessage(color("&7/team sethome &f- Definir home da equipe"));
                    p.sendMessage(color("&7/team delhome &f- Deletar home da equipe"));
                    p.sendMessage(color("&7/team pvp &f- Alternar PvP da equipe"));
                    p.sendMessage(color("&7/team chat &f- Alternar chat da equipe"));
                }
                break;

            case "home":
                if (args.length == 0) {
                    openGUI(p, "HOME-MENU");
                    break;
                }
                String searchHome = args[0];
                Location targetLoc = null;
                if (prof.homes.containsKey(searchHome)) {
                    targetLoc = prof.homes.get(searchHome);
                } else {
                    for (Map.Entry<String, Location> entry : prof.homes.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(searchHome)) {
                            targetLoc = entry.getValue();
                            break;
                        }
                    }
                }
                if (targetLoc == null) {
                    try {
                        int index = Integer.parseInt(searchHome) - 1;
                        List<String> keys = new ArrayList<>(prof.homes.keySet());
                        if (index >= 0 && index < keys.size()) {
                            targetLoc = prof.homes.get(keys.get(index));
                        }
                    } catch (Exception ignored) {}
                }
                if (targetLoc != null) {
                    teleportWithCooldown(p, targetLoc, "HOME");
                } else {
                    p.sendMessage(getMsg("HOMES.NOT-FOUND").replace("{home}", searchHome));
                }
                break;

            case "homes":
                openGUI(p, "HOME-MENU");
                break;

            case "sethome":
                if (args.length == 0) {
                    p.sendMessage(getMsg("HOMES.USAGE-SETHOME"));
                    return true;
                }
                String homeName = args[0];
                for (String existing : prof.homes.keySet()) {
                    if (existing.equalsIgnoreCase(homeName)) {
                        p.sendMessage(getMsg("HOMES.ALREADY-EXISTS"));
                        playSound(p, "COMMANDS.ERROR");
                        return true;
                    }
                }
                if (!hasHomePermission(p, prof.homes.size() + 1)) {
                    p.sendMessage(getMsg("HOMES.LIMIT-REACHED"));
                    playSound(p, "MENUS.BUTTON-CLICK");
                    return true;
                }
                prof.homes.put(homeName, p.getLocation());
                saveProfile(prof);
                p.sendMessage(getMsg("HOMES.SET-SUCCESS").replace("{home}", homeName));
                playSound(p, "SELL.LEVEL-UP");
                break;

            case "delhome":
                if (args.length == 0) {
                    p.sendMessage(getMsg("HOMES.USAGE-DELHOME"));
                    return true;
                }
                String targetHome = args[0];
                String actualKey = null;
                if (prof.homes.containsKey(targetHome)) {
                    actualKey = targetHome;
                } else {
                    for (String key : prof.homes.keySet()) {
                        if (key.equalsIgnoreCase(targetHome)) {
                            actualKey = key;
                            break;
                        }
                    }
                }
                if (actualKey == null) {
                    p.sendMessage(getMsg("HOMES.NOT-FOUND").replace("{home}", targetHome));
                    return true;
                }
                prof.homes.remove(actualKey);
                saveProfile(prof);
                p.sendMessage(getMsg("HOMES.DELETE-SUCCESS").replace("{home}", actualKey));
                playSound(p, "MENUS.BUTTON-CLICK");
                break;

            case "rtp":
                if (rtpConfig != null && !rtpConfig.getBoolean("ENABLED", true)) {
                    p.sendMessage(color(rtpConfig.getString("MESSAGES.DISABLED", "&cRTP is currently disabled.")));
                    return true;
                }
                if (args.length == 0) {
                    openGUI(p, "RTP-MENU");
                } else {
                    executeRTP(p, args[0]);
                }
                break;

            case "sell":
                if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
                    double total = sellInventory(p, p.getInventory());
                    if (total <= 0) {
                        sendHotbar(p, color("&cNenhum item vendável no seu inventário."));
                        try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); } catch (Exception ignored) {}
                    }
                } else {
                    openGUI(p, "SELL-MENU");
                }
                break;

            case "worth":
                viewingProgressPage.put(p.getUniqueId(), 0);
                openGUI(p, "WORTH-MENU");
                break;

            case "stats":
            case "status":
            case "perfil":
            case "profile":
                if (args.length > 0) {
                    OfflinePlayer targetOp = Bukkit.getOfflinePlayer(args[0]);
                    if (!targetOp.hasPlayedBefore() && !targetOp.isOnline()) {
                        p.sendMessage(color("&cJogador não encontrado!"));
                        return true;
                    }
                    viewingStatsProfile.put(p.getUniqueId(), targetOp.getUniqueId());
                } else {
                    viewingStatsProfile.remove(p.getUniqueId());
                }
                openGUI(p, "STATS-MENU");
                break;

            case "settings":
            case "config":
                openGUI(p, "SETTINGS-MENU");
                break;

            case "leaderboards":
            case "top":
            case "lb":
                openGUI(p, "LEADERBOARDS-MENU");
                break;

            case "tpa":
                if (args.length == 0) {
                    p.sendMessage(color("&cUso: /tpa <jogador>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    p.sendMessage(getMsg("GENERAL.PLAYER-NOT-FOUND"));
                    return true;
                }
                PlayerProfile tpaProf = getProfile(target);
                if (!tpaProf.tpaEnabled) {
                    p.sendMessage(color("&c" + target.getName() + " desativou os pedidos de TPA."));
                    return true;
                }
                if (tpaProf.tpAuto) {
                    p.sendMessage(getMsg("TPA.INVITE-SENT").replace("{player}", target.getName()));
                    target.sendMessage(color("&#FFF000" + p.getName() + " &7teleportando para você automaticamente (/tpauto)."));
                    teleportWithCooldown(p, target.getLocation(), "tpa");
                    return true;
                }
                tpaRequests.put(target.getUniqueId(), p.getUniqueId());
                pendingActions.put(target.getUniqueId(), new PendingAction(p.getUniqueId(), 0, "TPA"));
                p.sendMessage(getMsg("TPA.INVITE-SENT").replace("{player}", target.getName()));
                target.sendMessage(getMsg("TPA.INVITE-RECEIVED").replace("{player}", p.getName()));
                sendHotbar(target, color("&#FFF000" + p.getName() + " &7quer ir até você."));
                if (tpaProf.tpaConfirmMenus) {
                    openGUI(target, "TPA-CONFIRM-MENU");
                }
                try {
                    net.kyori.adventure.text.Component acceptMsg = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(getMsg("TPA.CLICK-TO-ACCEPT"))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tpaccept"))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize("&aClique para aceitar!")));
                    target.sendMessage(acceptMsg);
                    net.kyori.adventure.text.Component denyMsg = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(getMsg("TPA.CLICK-TO-DENY"))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tpdeny"))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize("&cClique para recusar!")));
                    target.sendMessage(denyMsg);
                } catch (Throwable ex) {
                    try {
                        net.md_5.bungee.api.chat.TextComponent acceptMsg = new net.md_5.bungee.api.chat.TextComponent(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', getMsg("TPA.CLICK-TO-ACCEPT")));
                        acceptMsg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
                        acceptMsg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', "&aClique para aceitar!")).create()));
                        target.spigot().sendMessage(acceptMsg);
                        net.md_5.bungee.api.chat.TextComponent denyMsg = new net.md_5.bungee.api.chat.TextComponent(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', getMsg("TPA.CLICK-TO-DENY")));
                        denyMsg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/tpdeny"));
                        denyMsg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', "&cClique para recusar!")).create()));
                        target.spigot().sendMessage(denyMsg);
                    } catch (Throwable ex2) {
                        target.sendMessage(getMsg("TPA.CLICK-TO-ACCEPT"));
                        target.sendMessage(getMsg("TPA.CLICK-TO-DENY"));
                    }
                }
                break;

            case "tpahere":
                if (args.length == 0) {
                    p.sendMessage(color("&cUso: /tpahere <jogador>"));
                    return true;
                }
                Player targetHere = Bukkit.getPlayer(args[0]);
                if (targetHere == null) {
                    p.sendMessage(getMsg("GENERAL.PLAYER-NOT-FOUND"));
                    return true;
                }
                PlayerProfile targetHereProf = getProfile(targetHere);
                if (!targetHereProf.tpaHereEnabled) {
                    p.sendMessage(color("&c" + targetHere.getName() + " desativou os pedidos de TPAHERE."));
                    return true;
                }
                if (targetHereProf.tpAuto) {
                    p.sendMessage(getMsg("TPA.INVITE-SENT").replace("{player}", targetHere.getName()));
                    targetHere.sendMessage(color("&#FFF000" + p.getName() + " &7puxando você automaticamente (/tpauto)."));
                    teleportWithCooldown(targetHere, p.getLocation(), "tpa");
                    return true;
                }
                tpaRequests.put(targetHere.getUniqueId(), p.getUniqueId());
                pendingActions.put(targetHere.getUniqueId(), new PendingAction(p.getUniqueId(), 0, "TPAHERE", p.getLocation().clone()));
                p.sendMessage(getMsg("TPA.INVITE-SENT").replace("{player}", targetHere.getName()));
                targetHere.sendMessage(getMsg("TPA.INVITE-RECEIVED").replace("{player}", p.getName()));
                sendHotbar(targetHere, color("&#FFF000" + p.getName() + " &7quer que você vá até ele."));
                if (targetHereProf.tpaConfirmMenus) {
                    openGUI(targetHere, "TPA-CONFIRM-MENU");
                }
                try {
                    net.kyori.adventure.text.Component acceptMsg = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(getMsg("TPA.CLICK-TO-ACCEPT"))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tpaccept"))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize("&aClique para aceitar!")));
                    targetHere.sendMessage(acceptMsg);
                    net.kyori.adventure.text.Component denyMsg = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(getMsg("TPA.CLICK-TO-DENY"))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tpdeny"))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize("&cClique para recusar!")));
                    targetHere.sendMessage(denyMsg);
                } catch (Throwable ex) {
                    try {
                        net.md_5.bungee.api.chat.TextComponent acceptMsg = new net.md_5.bungee.api.chat.TextComponent(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', getMsg("TPA.CLICK-TO-ACCEPT")));
                        acceptMsg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
                        acceptMsg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', "&aClique para aceitar!")).create()));
                        targetHere.spigot().sendMessage(acceptMsg);
                        net.md_5.bungee.api.chat.TextComponent denyMsg = new net.md_5.bungee.api.chat.TextComponent(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', getMsg("TPA.CLICK-TO-DENY")));
                        denyMsg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/tpdeny"));
                        denyMsg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', "&cClique para recusar!")).create()));
                        targetHere.spigot().sendMessage(denyMsg);
                    } catch (Throwable ex2) {
                        targetHere.sendMessage(getMsg("TPA.CLICK-TO-ACCEPT"));
                        targetHere.sendMessage(getMsg("TPA.CLICK-TO-DENY"));
                    }
                }
                break;

            case "tpaccept":
                UUID senderUUID = tpaRequests.remove(p.getUniqueId());
                if (senderUUID == null) {
                    p.sendMessage(getMsg("TPA.NO-PENDING"));
                    return true;
                }
                Player tpaSender = Bukkit.getPlayer(senderUUID);
                if (tpaSender != null) {
                    String reqType = "TPA";
                    Location savedLoc = null;
                    PendingAction existingPa = pendingActions.get(p.getUniqueId());
                    if (existingPa != null && "TPAHERE".equals(existingPa.extraData)) {
                        reqType = "TPAHERE";
                        savedLoc = existingPa.targetLocation;
                    }
                    pendingActions.put(p.getUniqueId(), new PendingAction(senderUUID, 0, reqType, savedLoc));
                    openGUI(p, "TPA-CONFIRM-MENU");
                } else {
                    p.sendMessage(getMsg("TPA.PLAYER-OFFLINE"));
                }
                break;

            case "tpdeny":
                if (tpaRequests.remove(p.getUniqueId()) != null) {
                    p.sendMessage(getMsg("TPA.DENIED"));
                } else {
                    p.sendMessage(getMsg("TPA.NO-PENDING"));
                }
                break;

            case "bounty":
                if (args.length >= 3 && args[0].equalsIgnoreCase("add")) {
                    Player bTarget = Bukkit.getPlayer(args[1]);
                    if (bTarget == null) {
                        p.sendMessage(getMsg("GENERAL.PLAYER-NOT-FOUND"));
                        return true;
                    }
                    if (bTarget.getUniqueId().equals(p.getUniqueId())) {
                        String msg = messagesConfig != null && messagesConfig.contains("BOUNTY.CANNOT-SET-SELF") ? getMsg("BOUNTY.CANNOT-SET-SELF") : color("&cVocê não pode setar bounty em si mesmo.");
                        p.sendMessage(msg);
                        return true;
                    }
                    try {
                        double amt = Double.parseDouble(args[2]);
                        if (amt < 100) {
                            p.sendMessage(getMsg("BOUNTY.MINIMUM-AMOUNT"));
                            return true;
                        }
                        if (prof.money < amt) {
                            p.sendMessage(getMsg("BOUNTY.NOT-ENOUGH-MONEY"));
                            return true;
                        }
                        pendingActions.put(p.getUniqueId(), new PendingAction(bTarget.getUniqueId(), amt, "BOUNTY"));
                        openGUI(p, "BOUNTY-CONFIRM-MENU");
                    } catch (NumberFormatException ex) {
                        p.sendMessage(color("&cValor inválido."));
                    }
                } else {
                    openGUI(p, "BOUNTIES-MENU");
                }
                break;

            case "shop":
                openGUI(p, "CATEGORIES");
                break;

            case "rules":
            case "regras":
                openGUI(p, "RULES-MENU");
                break;

            case "tpauto":
                prof.tpAuto = !prof.tpAuto;
                p.sendMessage(prof.tpAuto ? getMsg("TPAUTO.ENABLED") : getMsg("TPAUTO.DISABLED"));
                break;

            case "phantom":
                prof.phantomDisabled = !prof.phantomDisabled;
                p.sendMessage(prof.phantomDisabled ? getMsg("PHANTOM.ENABLED") : getMsg("PHANTOM.DISABLED"));
                break;

            case "clearlag":
            case "lixeiro":
                String clMsg = getMsg("CLEAR-LAG.STATUS");
                if (clMsg == null || clMsg.isEmpty() || clMsg.equals("CLEAR-LAG.STATUS")) {
                    clMsg = "&7Próxima passada do &#FFCB78Lixeiro &7vai ser daqui &#FFCB78{time}";
                }
                p.sendMessage(color(clMsg.replace("{time}", formatFullTime(clearlagCountdown)).replace("{seconds}", String.valueOf(clearlagCountdown))));
                break;

            case "pay":
                if (args.length < 2) {
                    p.sendMessage(color("&cUso: /pay <jogador> <valor>"));
                    return true;
                }
                Player payTarget = Bukkit.getPlayer(args[0]);
                if (payTarget == null) {
                    p.sendMessage(getMsg("GENERAL.PLAYER-NOT-FOUND"));
                    return true;
                }
                if (payTarget.equals(p) || payTarget.getUniqueId().equals(p.getUniqueId())) {
                    p.sendMessage(color("&cVocê não pode enviar dinheiro para si mesmo."));
                    return true;
                }
                try {
                    double amt = Double.parseDouble(args[1]);
                    if (amt <= 0 || prof.money < amt) {
                        p.sendMessage(getMsg("ECONOMY.NOT-ENOUGH-MONEY"));
                        return true;
                    }
                    if (prof.payConfirmMenus) {
                        pendingActions.put(p.getUniqueId(), new PendingAction(payTarget.getUniqueId(), amt, "PAY"));
                        openGUI(p, "PAY-CONFIRM-MENU");
                    } else {
                        PlayerProfile payProf = getProfile(payTarget.getUniqueId());
                        prof.money -= amt;
                        payProf.money += amt;
                        saveProfile(prof);
                        saveProfile(payProf);
                        updateScoreboard(p, prof);
                        updateScoreboard(payTarget, payProf);
                        if (payProf.paymentsEnabled) {
                            payTarget.sendMessage(getMsg("ECONOMY.MONEY-RECEIVED").replace("{player}", p.getName()).replace("{amount}", String.format("%.2f", amt)));
                        }
                        p.sendMessage(getMsg("ECONOMY.MONEY-SENT").replace("{player}", payTarget.getName() != null ? payTarget.getName() : "Desconhecido").replace("{amount}", String.format("%.2f", amt)));
                        playSound(p, "SELL.LEVEL-UP");
                    }
                } catch (NumberFormatException ex) {
                    p.sendMessage(getMsg("ECONOMY.INVALID-AMOUNT"));
                }
                break;

            case "donutcore":
            case "dcore":
                if (!p.hasPermission("donutcore.admin") && args.length > 0 && !args[0].equalsIgnoreCase("help") && !args[0].equalsIgnoreCase("comandos") && !args[0].equalsIgnoreCase("commands")) {
                    p.sendMessage(getMsg("GENERAL.NO-PERMISSION"));
                    return true;
                }
                if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("comandos") || args[0].equalsIgnoreCase("commands")) {
                    p.sendMessage(color("&8&m----------------------------------------"));
                    p.sendMessage(color("&b&lDonutCore &f- &7Lista de Comandos:"));
                    p.sendMessage(color("&e/spawn &f- Teleporta para o spawn"));
                    p.sendMessage(color("&e/afk &f- Abre o menu AFK ou teleporta"));
                    p.sendMessage(color("&e/team &f- Abre menu ou gerencia equipe"));
                    p.sendMessage(color("&e/home & /homes &f- Gerencia suas casas"));
                    p.sendMessage(color("&e/rtp (ou /wild) &f- Teleporte aleatório (Abre menu RTP)"));
                    p.sendMessage(color("&e/sell &f- Abre o menu de vendas"));
                    p.sendMessage(color("&e/worth &f- Tabela de preços dos itens"));
                    p.sendMessage(color("&e/stats (ou /perfil) &f- Veja seus status"));
                    p.sendMessage(color("&e/settings (ou /config) &f- Suas preferências"));
                    p.sendMessage(color("&e/leaderboards (ou /top) &f- Veja o ranking"));
                    p.sendMessage(color("&e/tpa, /tpaccept, /tpdeny &f- Pedidos de teleporte"));
                    p.sendMessage(color("&e/bounty &f- Menu de recompensas por cabeças"));
                    p.sendMessage(color("&e/shop &f- Loja do servidor"));
                    p.sendMessage(color("&e/rules &f- Regras do servidor"));
                    p.sendMessage(color("&e/tpauto &f- Aceitar TP automaticamente"));
                    p.sendMessage(color("&e/phantom &f- Ativar/desativar spawn de phantoms"));
                    p.sendMessage(color("&e/pay <jogador> <valor> &f- Enviar dinheiro"));
                    p.sendMessage(color("&e/media &f- Nossas redes sociais"));
                    p.sendMessage(color("&e/sellhistory &f- Seu histórico de vendas"));
                    p.sendMessage(color("&e/help &f- Menu de informações do servidor"));
                    p.sendMessage(color("&c/dcore reload &f- Recarregar configs"));
                    p.sendMessage(color("&c/dcore wand &f- Receber a Golden Hoe de seleção AFK"));
                    p.sendMessage(color("&c/dcore cuboid create/delete <nome> &f- Gerenciar área Cuboid AFK"));
                    p.sendMessage(color("&c/dcore afk list &f- Listar todos os pontos de spawn AFK criados"));
                    p.sendMessage(color("&c/dcore afk set <nome> &f- Definir ponto de spawn AFK (dentro da área cuboid)"));
                    p.sendMessage(color("&c/dcore afk delete <nome> &f- Deletar um ponto de spawn AFK"));
                    p.sendMessage(color("&c/dcore setspawn / setafk &f- Definir posições"));
                    p.sendMessage(color("&c/dcore amethyst <tool> <player> <duração> &f- Dar ferramenta Ametista"));
                    p.sendMessage(color("&c/dcore shardmanager (ou /shardmanager) &f- Gerenciar shards"));
                    p.sendMessage(color("&c/dcore moneymanager (ou /moneymanager) &f- Gerenciar money"));
                    p.sendMessage(color("&c/dcore warpmanager (ou /warpmanager) &f- Gerenciar warps"));
                    p.sendMessage(color("&8&m----------------------------------------"));
                } else if (args.length > 0 && args[0].equalsIgnoreCase("amethyst")) {
                    if (args.length < 2) {
                        p.sendMessage(color("&cUso correto: /dcore amethyst <pickaxe|axe|sellaxe|shovel|bucket|booster> <jogador> <duração>"));
                        return true;
                    }
                    String toolType = args[1];
                    Player amethystTarget = p;
                    long durationSeconds = 0;
                    String durationText = "Permanente";
                    if (args.length >= 3) {
                        Player found = Bukkit.getPlayer(args[2]);
                        if (found != null) amethystTarget = found;
                        else {
                            long parsed = parseDurationSeconds(args[2]);
                            if (parsed > 0) { durationSeconds = parsed; durationText = args[2]; }
                        }
                    }
                    if (args.length >= 4) {
                        long parsed = parseDurationSeconds(args[3]);
                        if (parsed > 0) { durationSeconds = parsed; durationText = args[3]; }
                        else durationText = args[3];
                    }
                    ItemStack tool = createAmethystTool(toolType, durationSeconds, durationText);
                    amethystTarget.getInventory().addItem(tool);
                } else if (args.length > 0 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))) {
                    reloadPlugin(p);
                } else if (args.length > 0 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("setspawn"))) {
                    if (args[0].equalsIgnoreCase("setspawn")) {
                        setServerSpawn(p);
                        return true;
                    }
                    if (args.length == 1 && args[0].equalsIgnoreCase("spawn")) {
                        if (getConfig().getBoolean("SETTINGS.SPAWN-MENU", true)) {
                            openGUI(p, "SPAWN-MENU");
                        } else {
                            Location spawnTargetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-LOCATION", ""));
                            if (spawnTargetLoc == null) spawnTargetLoc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-SPAWNS.1", ""));
                            if (spawnTargetLoc == null) spawnTargetLoc = p.getWorld().getSpawnLocation();
                            teleportWithCooldown(p, spawnTargetLoc, "SPAWN");
                        }
                        return true;
                    }
                    String action = args[1];
                    String spawnName = (args.length > 2) ? args[2] : "spawn1";
                    if (action.equalsIgnoreCase("list")) {
                        ConfigurationSection spawnsSec = getConfig().getConfigurationSection("LOCATIONS.SPAWN-SPAWNS");
                        if (spawnsSec == null || spawnsSec.getKeys(false).isEmpty()) {
                            p.sendMessage(color("&cNenhum ponto de spawn configurado no momento."));
                        } else {
                            p.sendMessage(color("&8&m----------------------------------------"));
                            p.sendMessage(color("&b&lSpawns configurados:"));
                            for (String key : spawnsSec.getKeys(false)) {
                                Location loc = parseLocation(getConfig().getString("LOCATIONS.SPAWN-SPAWNS." + key, ""));
                                if (loc != null) {
                                    String containing = getContainingCuboidName(loc);
                                    String inCub = (containing != null) ? " &a(Dentro do Cuboid '" + containing + "')" : " &7(Sem Cuboid ou fora dele)";
                                    p.sendMessage(color("&e- &f" + key + "&7: [" + loc.getWorld().getName() + " X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ() + "]" + inCub));
                                }
                            }
                            p.sendMessage(color("&8&m----------------------------------------"));
                        }
                    } else if (action.equalsIgnoreCase("set")) {
                        setSpawnCommandPoint(p, spawnName);
                    } else if (action.equalsIgnoreCase("delete") || action.equalsIgnoreCase("del") || action.equalsIgnoreCase("remove")) {
                        if (args.length < 3) {
                            p.sendMessage(color("&cUso correto: /dcore spawn delete <nome> (ex: spawn1, spawn2)"));
                            return true;
                        }
                        getConfig().set("LOCATIONS.SPAWN-SPAWNS." + spawnName, null);
                        if (spawnName.equals("1") || spawnName.equalsIgnoreCase("spawn1")) getConfig().set("LOCATIONS.SPAWN-LOCATION", null);
                        saveConfig();
                        p.sendMessage(color("&a✔ Ponto de Spawn &#00A4FC" + spawnName + " &aremovido com sucesso!"));
                    } else {
                        p.sendMessage(color("&cUso correto: /dcore spawn <list|set <nome>|delete <nome>>"));
                    }
                } else if (args.length > 0 && args[0].equalsIgnoreCase("wand")) {
                    giveWandItem(p);
                } else if (args.length > 0 && args[0].equalsIgnoreCase("warpmanager")) {
                    handleWarpManagerCommand(p, Arrays.copyOfRange(args, 1, args.length));
                } else if (args.length > 0 && args[0].equalsIgnoreCase("endersee")) {
                    if (!p.hasPermission("donutcore.admin") && !p.hasPermission("donutcore.endersee") && !p.isOp()) {
                        p.sendMessage(color("&cVocê não tem permissão para abrir o Ender Chest de outros jogadores."));
                        return true;
                    }
                    if (args.length < 2) {
                        p.sendMessage(color("&cUso correto: /dcore endersee <jogador>"));
                        return true;
                    }
                    targetEC = Bukkit.getPlayer(args[1]);
                    if (targetEC == null) {
                        p.sendMessage(color("&cJogador não encontrado online."));
                        return true;
                    }
                    openCustomEnderChest(p, targetEC);
                    return true;
                } else if (args.length > 0 && args[0].equalsIgnoreCase("cuboid")) {
                    if (args.length < 2) {
                        p.sendMessage(color("&cUso correto: /dcore cuboid <list|create <nome>|delete <nome>>"));
                        return true;
                    }
                    String action = args[1];
                    if (action.equalsIgnoreCase("list")) {
                        ConfigurationSection cubSec = getCuboidsConfig().getConfigurationSection("CUBOIDS");
                        if (cubSec == null || cubSec.getKeys(false).isEmpty()) {
                            p.sendMessage(color("&cNenhuma área Cuboid criada no momento."));
                        } else {
                            p.sendMessage(color("&8&m----------------------------------------"));
                            p.sendMessage(color("&b&lÁreas Cuboid criadas:"));
                            for (String key : cubSec.getKeys(false)) {
                                ConfigurationSection sec = cubSec.getConfigurationSection(key);
                                if (sec != null) {
                                    String world = sec.getString("WORLD", "world");
                                    int minX = (int) sec.getDouble("MIN-X");
                                    int minY = (int) sec.getDouble("MIN-Y");
                                    int minZ = (int) sec.getDouble("MIN-Z");
                                    int maxX = (int) sec.getDouble("MAX-X");
                                    int maxY = (int) sec.getDouble("MAX-Y");
                                    int maxZ = (int) sec.getDouble("MAX-Z");
                                    p.sendMessage(color("&e- &f" + key + "&7: [" + world + " (" + minX + ", " + minY + ", " + minZ + ") -> (" + maxX + ", " + maxY + ", " + maxZ + ")]"));
                                }
                            }
                            p.sendMessage(color("&8&m----------------------------------------"));
                        }
                    } else if (action.equalsIgnoreCase("create")) {
                        if (args.length < 3) {
                            p.sendMessage(color("&cUso correto: /dcore cuboid create <nome> (ex: spawn1, spawn2, afk1, afk2)"));
                            return true;
                        }
                        String areaName = args[2];
                        Location p1 = wandPos1.get(p.getUniqueId());
                        Location p2 = wandPos2.get(p.getUniqueId());
                        if (p1 == null || p2 == null) {
                            p.sendMessage(color("&cVocê precisa selecionar a Posição #1 e #2 com a &e/dcore wand &cprimeiro!"));
                            return true;
                        }
                        if (!p1.getWorld().getName().equals(p2.getWorld().getName())) {
                            p.sendMessage(color("&cAs duas posições precisam estar no mesmo mundo!"));
                            return true;
                        }
                        String path = "CUBOIDS." + areaName;
                        getCuboidsConfig().set(path + ".WORLD", p1.getWorld().getName());
                        getCuboidsConfig().set(path + ".MIN-X", Math.min(p1.getBlockX(), p2.getBlockX()));
                        getCuboidsConfig().set(path + ".MIN-Y", Math.min(p1.getBlockY(), p2.getBlockY()));
                        getCuboidsConfig().set(path + ".MIN-Z", Math.min(p1.getBlockZ(), p2.getBlockZ()));
                        getCuboidsConfig().set(path + ".MAX-X", Math.max(p1.getBlockX(), p2.getBlockX()));
                        getCuboidsConfig().set(path + ".MAX-Y", Math.max(p1.getBlockY(), p2.getBlockY()));
                        getCuboidsConfig().set(path + ".MAX-Z", Math.max(p1.getBlockZ(), p2.getBlockZ()));
                        saveCuboids();
                        p.sendMessage(color("&a✔ Área Cuboid &#00A4FC" + areaName + " &acriada com sucesso!"));
                        if (areaName.toLowerCase().contains("spawn")) {
                            p.sendMessage(color("&eDica: Agora fique dentro dessa área e digite &f/dcore spawn set " + areaName + " &epara definir onde os jogadores irão nascer!"));
                        } else if (areaName.toLowerCase().contains("afk")) {
                            p.sendMessage(color("&eDica: Agora fique dentro dessa área e digite &f/dcore afk set " + areaName + " &epara definir onde os jogadores do AFK irão nascer!"));
                        }
                    } else if (action.equalsIgnoreCase("delete") || action.equalsIgnoreCase("remove")) {
                        if (args.length < 3) {
                            p.sendMessage(color("&cUso correto: /dcore cuboid delete <nome>"));
                            return true;
                        }
                        String areaName = args[2];
                        getCuboidsConfig().set("CUBOIDS." + areaName, null);
                        saveCuboids();
                        p.sendMessage(color("&a✔ Área Cuboid &#00A4FC" + areaName + " &adeletada com sucesso!"));
                    } else {
                        p.sendMessage(color("&cUso correto: /dcore cuboid <list|create <nome>|delete <nome>>"));
                    }
                } else if (args.length > 0 && args[0].equalsIgnoreCase("afk")) {
                    String action = (args.length > 1) ? args[1] : "help";
                    String afkName = (args.length > 2) ? args[2] : "afk1";
                    if (action.equalsIgnoreCase("list")) {
                        ConfigurationSection spawnsSec = getConfig().getConfigurationSection("LOCATIONS.AFK-SPAWNS");
                        if (spawnsSec == null || spawnsSec.getKeys(false).isEmpty()) {
                            p.sendMessage(color("&cNenhum ponto de spawn AFK configurado no momento."));
                        } else {
                            p.sendMessage(color("&8&m----------------------------------------"));
                            p.sendMessage(color("&b&lSpawns AFK configurados:"));
                            for (String key : spawnsSec.getKeys(false)) {
                                Location loc = parseLocation(getConfig().getString("LOCATIONS.AFK-SPAWNS." + key, ""));
                                if (loc != null) {
                                    String containing = getContainingCuboidName(loc);
                                    String inCub = (containing != null) ? " &a(Dentro do Cuboid '" + containing + "')" : " &7(Sem Cuboid ou fora dele)";
                                    p.sendMessage(color("&e- &f" + key + "&7: [" + loc.getWorld().getName() + " X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ() + "]" + inCub));
                                }
                            }
                            p.sendMessage(color("&8&m----------------------------------------"));
                        }
                    } else if (action.equalsIgnoreCase("set")) {
                        if (args.length < 3) {
                            p.sendMessage(color("&cUso correto: /dcore afk set <nome> (ex: afk1, afk2)"));
                            return true;
                        }
                        getConfig().set("LOCATIONS.AFK-SPAWNS." + afkName, serializeLocation(p.getLocation()));
                        if (afkName.equals("1") || afkName.equalsIgnoreCase("afk1")) {
                            getConfig().set("LOCATIONS.AFK-LOCATION", serializeLocation(p.getLocation()));
                        }
                        saveConfig();
                        p.sendMessage(color("&a✔ Ponto de Spawn AFK &#A303F9" + afkName + " &adefinido com sucesso no seu local!"));
                        String containing = getContainingCuboidName(p.getLocation());
                        if (containing == null) {
                            p.sendMessage(color("&eAviso: Este local está fora do Cuboid '" + afkName + "'. Para contar tempo AFK, certifique-se de demarcar a área com `/dcore wand` e `/dcore cuboid create " + afkName + "`!"));
                        } else {
                            p.sendMessage(color("&a✔ Ponto de spawn AFK está dentro da região Cuboid '&f" + containing + "&a'!"));
                        }
                    } else if (action.equalsIgnoreCase("delete") || action.equalsIgnoreCase("del") || action.equalsIgnoreCase("remove")) {
                        if (args.length < 3) {
                            p.sendMessage(color("&cUso correto: /dcore afk delete <nome> (ex: afk1, afk2)"));
                            return true;
                        }
                        getConfig().set("LOCATIONS.AFK-SPAWNS." + afkName, null);
                        if (afkName.equals("1") || afkName.equalsIgnoreCase("afk1")) getConfig().set("LOCATIONS.AFK-LOCATION", null);
                        saveConfig();
                        p.sendMessage(color("&a✔ Ponto de Spawn AFK &#A303F9" + afkName + " &aremovido com sucesso!"));
                    } else {
                        p.sendMessage(color("&cUso correto: /dcore afk <list|set <nome>|delete <nome>>"));
                    }
                } else {
                    p.sendMessage(color("&8&m----------------------------------------"));
                    p.sendMessage(color("&b&lComandos Administrativos DonutCore:"));
                    p.sendMessage(color("&c/dcore reload &f- Recarregar configs"));
                    p.sendMessage(color("&c/dcore wand &f- Receber a Wand de Seleção"));
                    p.sendMessage(color("&c/dcore cuboid <list|create|delete> <nome> &f- Gerenciar áreas Cuboid"));
                    p.sendMessage(color("&c/dcore afk <list|set|delete> <nome> &f- Gerenciar pontos AFK"));
                    p.sendMessage(color("&c/dcore spawn <list|set|delete> <nome> &f- Gerenciar pontos de Spawn"));
                    p.sendMessage(color("&c/dcore amethyst <tool> <player> <duracao> &f- Dar ferramenta Ametista"));
                    p.sendMessage(color("&c/dcore shardmanager &f- Gerenciar shards dos jogadores"));
                    p.sendMessage(color("&c/dcore moneymanager &f- Gerenciar money dos jogadores"));
                    p.sendMessage(color("&8&m----------------------------------------"));
                }
                break;

            case "media":
            case "midia":
                openGUI(p, "MEDIA-MENU");
                break;

            case "sellhistory":
                viewingProgressPage.put(p.getUniqueId(), 0);
                openGUI(p, "SELL-HISTORY-MENU");
                break;

            case "help":
            case "ajuda":
                openGUI(p, "SERVER-INFO-MENU");
                break;

            case "fragmentos":
            case "fragmento":
            case "shards":
            case "shard":
                if (args.length > 0) {
                    OfflinePlayer targetOp = Bukkit.getOfflinePlayer(args[0]);
                    if (!targetOp.hasPlayedBefore() && !targetOp.isOnline()) {
                        p.sendMessage(color("&cJogador não encontrado!"));
                        return true;
                    }
                    PlayerProfile targetProf = getProfile(targetOp);
                    p.sendMessage(color("&#FFF000" + targetProf.username + " &7tem &#DF9FFF" + formatAbbreviated(targetProf.shards) + " fragmentos"));
                } else {
                    p.sendMessage(color("&7Você tem &#DF9FFF" + formatAbbreviated(prof.shards) + " fragmentos"));
                }
                break;

            case "findplayer":
                if (args.length == 0) {
                    p.sendMessage(color("&cUso correto: /findplayer <player>"));
                    return true;
                }
                Player targetFind = Bukkit.getPlayer(args[0]);
                if (targetFind == null) {
                    p.sendMessage(color("&cJogador não encontrado ou offline!"));
                    return true;
                }
                p.sendMessage(color("&#FFF000" + targetFind.getName() + " &7Está localizado em " + getPlayerLocationType(targetFind)));
                break;

            case "fragmentopay":
                if (args.length < 2) {
                    p.sendMessage(color("&cUso correto: /fragmentopay <player> <quantidade>"));
                    return true;
                }
                OfflinePlayer targetPayOp = Bukkit.getOfflinePlayer(args[0]);
                if (!targetPayOp.hasPlayedBefore() && !targetPayOp.isOnline()) {
                    p.sendMessage(color("&cJogador não encontrado!"));
                    return true;
                }
                if (targetPayOp.getUniqueId().equals(p.getUniqueId())) {
                    p.sendMessage(color("&cVocê não pode enviar fragmentos para si mesmo!"));
                    return true;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[1]);
                } catch (Exception ex) {
                    p.sendMessage(color("&cQuantidade inválida!"));
                    return true;
                }
                if (amount <= 0) {
                    p.sendMessage(color("&cA quantidade deve ser maior que zero!"));
                    return true;
                }
                if (prof.shards < amount) {
                    p.sendMessage(color("&cVocê não tem fragmentos suficientes!"));
                    return true;
                }
                PlayerProfile targetPayProf = getProfile(targetPayOp);
                prof.shards -= amount;
                targetPayProf.shards += amount;
                saveProfile(prof);
                saveProfile(targetPayProf);
                p.sendMessage(color("&7Você enviou &#DF9FFF" + formatAbbreviated(amount) + " &7para &#FFF000" + targetPayProf.username));
                Player targetPayOnline = Bukkit.getPlayer(targetPayOp.getUniqueId());
                if (targetPayOnline != null) {
                    targetPayOnline.sendMessage(color("&7Você recebeu &#DF9FFF" + formatAbbreviated(amount) + " &7fragmentos de &#FFF000" + p.getName()));
                }
                break;

            case "discord":
                if (p != null) {
                    String link = messagesConfig.getString("DISCORD.LINK", "dc.gg/aureliumshields");
                    List<String> lore = messagesConfig.getStringList("DISCORD.LORE");
                    for (String line : lore) {
                        p.sendMessage(color(line.replace("{link}", link)));
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                break;

            case "loja":
                if (p != null) {
                    String link = messagesConfig.getString("LOJA.LINK", "loja.aureliumshields.net");
                    List<String> lore = messagesConfig.getStringList("LOJA.LORE");
                    for (String line : lore) {
                        p.sendMessage(color(line.replace("{link}", link)));
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                break;

            case "ping":
                if (args.length > 0) {
                    Player targetPing = Bukkit.getPlayer(args[0]);
                    if (targetPing != null) {
                        p.sendMessage(color("&7Ping de &e" + targetPing.getName() + "&7: &#FFF000" + targetPing.getPing() + "&7!"));
                    } else {
                        p.sendMessage(color("&cJogador não encontrado!"));
                    }
                } else {
                    p.sendMessage(color("&7Seu ping &#FFF000" + p.getPing() + "&7!"));
                }
                break;

            case "tell":
            case "msg":
            case "w":
                if (args.length < 2) {
                    p.sendMessage(color("&cUso correto: /" + name + " <player> <mensagem>"));
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer == null) {
                    p.sendMessage(color("&cJogador não encontrado ou offline!"));
                    return true;
                }
                if (targetPlayer.getUniqueId().equals(p.getUniqueId())) {
                    p.sendMessage(color("&cVocê não pode enviar mensagem privada para si mesmo!"));
                    return true;
                }
                String msgContent = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                sendPrivateMessage(p, targetPlayer, msgContent);
                break;

            case "r":
            case "reply":
                if (args.length == 0) {
                    p.sendMessage(color("&cUso correto: /" + name + " <mensagem>"));
                    return true;
                }
                UUID replyTargetUUID = lastMessaged.get(p.getUniqueId());
                if (replyTargetUUID == null) {
                    p.sendMessage(color("&cVocê não tem para quem responder!"));
                    return true;
                }
                Player replyTarget = Bukkit.getPlayer(replyTargetUUID);
                if (replyTarget == null || !replyTarget.isOnline()) {
                    p.sendMessage(color("&cO jogador para quem você iria responder está offline!"));
                    return true;
                }
                String replyMsg = String.join(" ", args);
                sendPrivateMessage(p, replyTarget, replyMsg);
                break;

            case "ignore":
            case "ignorar":
                if (args.length < 1) {
                    p.sendMessage(color("&cUso correto: /" + name + " <player>"));
                    return true;
                }
                Player ignoreOnline = Bukkit.getPlayer(args[0]);
                UUID ignoreId = null;
                String ignoreName = null;
                if (ignoreOnline != null) {
                    ignoreId = ignoreOnline.getUniqueId();
                    ignoreName = ignoreOnline.getName();
                } else {
                    PlayerProfile ignoreProf = profiles.values().stream()
                            .filter(pr -> pr.username.equalsIgnoreCase(args[0]))
                            .findFirst().orElse(null);
                    if (ignoreProf != null) {
                        ignoreId = ignoreProf.uuid;
                        ignoreName = ignoreProf.username;
                    }
                }
                if (ignoreId == null) {
                    p.sendMessage(color("&cJogador não encontrado!"));
                    return true;
                }
                if (ignoreId.equals(p.getUniqueId())) {
                    p.sendMessage(color("&cVocê não pode ignorar a si mesmo!"));
                    return true;
                }
                if (prof.ignoredPlayers.contains(ignoreId)) {
                    prof.ignoredPlayers.remove(ignoreId);
                    p.sendMessage(color("&7Você voltou a ouvir &#FFF000" + ignoreName));
                } else {
                    prof.ignoredPlayers.add(ignoreId);
                    p.sendMessage(color("&7Você ignorou &#FFF000" + ignoreName));
                }
                saveProfile(prof);
                break;

            case "blocktell":
            case "bloqueartell":
                if (args.length == 0) {
                    prof.blockTell = !prof.blockTell;
                    p.sendMessage(color(prof.blockTell ? "&7Você bloqueou o tell" : "&7Você desbloqueou o tell"));
                    saveProfile(prof);
                    break;
                }
                Player blockOnline = Bukkit.getPlayer(args[0]);
                UUID blockId = null;
                if (blockOnline != null) {
                    blockId = blockOnline.getUniqueId();
                } else {
                    PlayerProfile blockProf = profiles.values().stream()
                            .filter(pr -> pr.username.equalsIgnoreCase(args[0]))
                            .findFirst().orElse(null);
                    if (blockProf != null) {
                        blockId = blockProf.uuid;
                    }
                }
                if (blockId == null) {
                    p.sendMessage(color("&cJogador não encontrado!"));
                    return true;
                }
                if (blockId.equals(p.getUniqueId())) {
                    p.sendMessage(color("&cVocê não pode bloquear a si mesmo!"));
                    return true;
                }
                if (prof.ignoredPlayers.contains(blockId)) {
                    prof.ignoredPlayers.remove(blockId);
                    p.sendMessage(color("&7Você desbloqueou o tell"));
                } else {
                    prof.ignoredPlayers.add(blockId);
                    p.sendMessage(color("&7Você bloqueou o tell"));
                }
                saveProfile(prof);
                break;


            case "nightvision":
            case "luz":
                prof.nightVisionEnabled = !prof.nightVisionEnabled;
                if (prof.nightVisionEnabled) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                    p.sendMessage(color("&7Você &aativou &7a visão noturna"));
                } else {
                    p.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
                    p.sendMessage(color("&7Você &cdesativou &7a visão noturna"));
                }
                saveProfile(prof);
                break;

            case "stash":
                if (!p.hasPermission("donutcore.admin") && !p.isOp()) {
                    p.sendMessage(getMsg("GENERAL.NO-PERMISSION"));
                    return true;
                }
                int stashType = 0;
                if (args.length > 0 && args[0].equalsIgnoreCase("summon")) {
                    if (args.length > 1) {
                        if (args[1].equals("1")) stashType = 1;
                        else if (args[1].equals("2")) stashType = 2;
                        else stashType = new Random().nextBoolean() ? 1 : 2;
                    } else {
                        stashType = new Random().nextBoolean() ? 1 : 2;
                    }
                } else if (args.length > 0 && (args[0].equals("1") || args[0].equals("2"))) {
                    stashType = Integer.parseInt(args[0]);
                } else {
                    stashType = new Random().nextBoolean() ? 1 : 2;
                }

                summonStash(p.getLocation().getBlock().getLocation(), stashType);
                p.sendMessage(color("&aStash #" + stashType + " invocada com sucesso na sua localização!"));
                try { p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f); } catch (Exception ignored) {}
                break;
        }
        return true;
    }

    public void summonStash(Location baseLoc, int type) {
        World w = baseLoc.getWorld();
        if (w == null) return;

        int bx = baseLoc.getBlockX();
        int by = baseLoc.getBlockY();
        int bz = baseLoc.getBlockZ();

        Material under = baseLoc.clone().subtract(0, 1, 0).getBlock().getType();
        Material baseMat;
        if (under.name().contains("DEEPSLATE")) {
            baseMat = Material.DEEPSLATE;
        } else if (under.name().contains("STONE")) {
            baseMat = Material.STONE;
        } else if (baseLoc.getBlockY() < 0) {
            baseMat = Material.DEEPSLATE;
        } else {
            baseMat = Material.STONE;
        }

        if (type == 1) {
            for (int x = -1; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    w.getBlockAt(bx + x, by, bz + z).setType(baseMat);
                }
            }
            for (int y = 1; y <= 2; y++) {
                for (int x = -1; x <= 2; x++) {
                    w.getBlockAt(bx + x, by + y, bz - 1).setType(baseMat);
                }
                for (int z = -1; z <= 1; z++) {
                    w.getBlockAt(bx - 1, by + y, bz + z).setType(baseMat);
                }
            }
            w.getBlockAt(bx, by + 1, bz).setType(Material.AIR);
            w.getBlockAt(bx + 1, by + 1, bz).setType(Material.AIR);

            org.bukkit.block.Block amethyst = w.getBlockAt(bx, by + 1, bz);
            amethyst.setType(Material.AMETHYST_CLUSTER);
            org.bukkit.block.data.BlockData amData = amethyst.getBlockData();
            if (amData instanceof org.bukkit.block.data.Directional) {
                ((org.bukkit.block.data.Directional) amData).setFacing(org.bukkit.block.BlockFace.UP);
                amethyst.setBlockData(amData);
            }

            w.getBlockAt(bx + 1, by + 1, bz).setType(Material.SPAWNER);

        } else if (type == 2) {
            for (int x = -1; x <= 2; x++) {
                for (int z = -1; z <= 2; z++) {
                    if (x != 1 || z != 1) {
                        w.getBlockAt(bx + x, by, bz + z).setType(baseMat);
                    }
                    if (x == -1 || x == 2 || z == -1 || z == 2) {
                        w.getBlockAt(bx + x, by, bz + z).setType(baseMat);
                        w.getBlockAt(bx + x, by + 1, bz + z).setType(baseMat);
                        w.getBlockAt(bx + x, by + 2, bz + z).setType(baseMat);
                    }
                }
            }
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    w.getBlockAt(bx + x, by + 1, bz + z).setType(Material.AIR);
                    w.getBlockAt(bx + x, by + 2, bz + z).setType(Material.AIR);
                }
            }

            w.getBlockAt(bx + 1, by, bz + 1).setType(Material.SPAWNER);
            w.getBlockAt(bx, by + 1, bz).setType(Material.SPAWNER);

            org.bukkit.block.Block drip1 = w.getBlockAt(bx + 1, by + 1, bz);
            drip1.setType(Material.POINTED_DRIPSTONE);
            org.bukkit.block.data.BlockData dData1 = drip1.getBlockData();
            if (dData1 instanceof org.bukkit.block.data.type.PointedDripstone) {
                org.bukkit.block.data.type.PointedDripstone pd = (org.bukkit.block.data.type.PointedDripstone) dData1;
                pd.setVerticalDirection(org.bukkit.block.BlockFace.UP);
                pd.setThickness(org.bukkit.block.data.type.PointedDripstone.Thickness.TIP);
                drip1.setBlockData(pd);
            }

            org.bukkit.block.Block drip2 = w.getBlockAt(bx, by + 1, bz + 1);
            drip2.setType(Material.POINTED_DRIPSTONE);
            org.bukkit.block.data.BlockData dData2 = drip2.getBlockData();
            if (dData2 instanceof org.bukkit.block.data.type.PointedDripstone) {
                org.bukkit.block.data.type.PointedDripstone pd = (org.bukkit.block.data.type.PointedDripstone) dData2;
                pd.setVerticalDirection(org.bukkit.block.BlockFace.UP);
                pd.setThickness(org.bukkit.block.data.type.PointedDripstone.Thickness.TIP);
                drip2.setBlockData(pd);
            }
        }
    }

    public void executeRTP(Player p, String worldName) {
        if (rtpConfig != null && !rtpConfig.getBoolean("ENABLED", true)) {
            p.sendMessage(color(rtpConfig.getString("MESSAGES.DISABLED", "&cRTP is currently disabled.")));
            return;
        }

        if (rtpConfig != null && rtpConfig.getStringList("DENIED-WORLDS").contains(p.getWorld().getName())) {
            p.sendMessage(color(rtpConfig.getString("MESSAGES.DISABLED", "&cRTP is currently disabled.")));
            return;
        }

        String lowerW = worldName.toLowerCase();
        String worldKey = worldName;
        String buttonKey = "OVERWORLD";
        if (lowerW.equals("overworld") || lowerW.equals("world")) {
            worldKey = "world";
            buttonKey = "OVERWORLD";
        } else if (lowerW.equals("nether") || lowerW.equals("world_nether")) {
            worldKey = "world_nether";
            buttonKey = "NETHER";
        } else if (lowerW.equals("end") || lowerW.equals("the_end") || lowerW.equals("world_the_end")) {
            worldKey = "world_the_end";
            buttonKey = "THE_END";
        }

        if (rtpConfig != null && rtpConfig.contains("RTP-MENU.BUTTONS." + buttonKey + ".ENABLED") && !rtpConfig.getBoolean("RTP-MENU.BUTTONS." + buttonKey + ".ENABLED", true)) {
            p.sendMessage(color(rtpConfig.getString("MESSAGES.DESTINATION-DISABLED", "&cThis RTP destination is currently disabled.")));
            return;
        }

        int maxSimultaneous = rtpConfig != null ? rtpConfig.getInt("SETTINGS.PLAYERS-IN-RTP", 10) : 10;
        if (activeRtpPlayers.size() >= maxSimultaneous) {
            p.sendMessage(color(rtpConfig.getString("MESSAGES.MAX-PLAYERS", "&cToo many players are using RTP right now. Please try again later.")));
            return;
        }

        if (rtpCooldowns.containsKey(p.getUniqueId())) {
            long remainingMillis = rtpCooldowns.get(p.getUniqueId()) - System.currentTimeMillis();
            if (remainingMillis > 0) {
                long seconds = (remainingMillis / 1000L) + 1;
                String cdMsg = rtpConfig != null ? rtpConfig.getString("MESSAGES.COOLDOWN", "&cYou can't rtp for another {remaining}s.") : "&cYou can't rtp for another {remaining}s.";
                p.sendMessage(color(cdMsg.replace("{remaining}", String.valueOf(seconds))));
                return;
            } else {
                rtpCooldowns.remove(p.getUniqueId());
            }
        }

        World world = Bukkit.getWorld(worldKey);
        if (world == null) {
            String notExist = rtpConfig != null ? rtpConfig.getString("MESSAGES.WORLD-NOT-EXIST", "&cThe world does not exist.") : "&cThe world does not exist.";
            p.sendMessage(color(notExist));
            return;
        }

        int min = rtpConfig != null ? rtpConfig.getInt("WORLD-SETTINGS." + worldKey + ".MIN-RADIUS", 500) : 500;
        int max = rtpConfig != null ? rtpConfig.getInt("WORLD-SETTINGS." + worldKey + ".MAX-RADIUS", 2000) : 2000;
        int maxAttempts = rtpConfig != null ? rtpConfig.getInt("SETTINGS.MAX-ATTEMPTS", 16) : 16;
        if (maxAttempts < 1) maxAttempts = 16;

        String searchingMsg = rtpConfig != null ? rtpConfig.getString("MESSAGES.SEARCHING", "&aSearching for a safe location in {world}...") : "&aSearching for a safe location in {world}...";
        p.sendMessage(color(searchingMsg.replace("{world}", world.getName())));

        activeRtpPlayers.add(p.getUniqueId());

        Location rtpLoc = findSafeRTPLocation(world, worldKey, min, max, maxAttempts);
        if (rtpLoc == null) {
            activeRtpPlayers.remove(p.getUniqueId());
            String failMsg = rtpConfig != null ? rtpConfig.getString("MESSAGES.MAX-ATTEMPTS", "&cCould not find a safe location after %attempts% attempts.") : "&cCould not find a safe location after %attempts% attempts.";
            p.sendMessage(color(failMsg.replace("%attempts%", String.valueOf(maxAttempts))));
            return;
        }

        String foundMsg = rtpConfig != null ? rtpConfig.getString("MESSAGES.SAFE-LOCATION-FOUND", "&aSafe location found at: X:{x} Y:{y} Z:{z}") : "&aSafe location found at: X:{x} Y:{y} Z:{z}";
        p.sendMessage(color(foundMsg.replace("{x}", String.valueOf(rtpLoc.getBlockX()))
                                    .replace("{y}", String.valueOf(rtpLoc.getBlockY()))
                                    .replace("{z}", String.valueOf(rtpLoc.getBlockZ()))));

        int worldCooldown = rtpConfig != null ? rtpConfig.getInt("WORLD-SETTINGS." + worldKey + ".COOLDOWN", 30) : 30;
        if (worldCooldown > 0) {
            rtpCooldowns.put(p.getUniqueId(), System.currentTimeMillis() + (worldCooldown * 1000L));
        }

        teleportWithCooldown(p, rtpLoc, "RTP");
        activeRtpPlayers.remove(p.getUniqueId());
    }

    public Location findSafeRTPLocation(World world, String worldKey, int minRadius, int maxRadius, int maxAttempts) {
        if (world == null) return null;
        int centerX = rtpConfig != null ? rtpConfig.getInt("WORLD-SETTINGS." + worldKey + ".CENTER-X", 0) : 0;
        int centerZ = rtpConfig != null ? rtpConfig.getInt("WORLD-SETTINGS." + worldKey + ".CENTER-Z", 0) : 0;
        Random r = new Random();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int range = Math.max(1, maxRadius - minRadius);
            int xOffset = r.nextInt(range) + minRadius;
            int zOffset = r.nextInt(range) + minRadius;
            if (r.nextBoolean()) xOffset = -xOffset;
            if (r.nextBoolean()) zOffset = -zOffset;
            int x = centerX + xOffset;
            int z = centerZ + zOffset;

            if (world.getEnvironment() == World.Environment.NETHER || world.getName().toLowerCase().contains("nether")) {
                for (int y = 110; y >= 32; y--) {
                    org.bukkit.block.Block ground = world.getBlockAt(x, y - 1, z);
                    org.bukkit.block.Block feet = world.getBlockAt(x, y, z);
                    org.bukkit.block.Block head = world.getBlockAt(x, y + 1, z);
                    if (isSafeGround(ground) && isSafeAir(feet) && isSafeAir(head)) {
                        return new Location(world, x + 0.5, y, z + 0.5);
                    }
                }
            } else if (world.getEnvironment() == World.Environment.THE_END || world.getName().toLowerCase().contains("end")) {
                for (int y = 120; y >= 40; y--) {
                    org.bukkit.block.Block ground = world.getBlockAt(x, y - 1, z);
                    org.bukkit.block.Block feet = world.getBlockAt(x, y, z);
                    org.bukkit.block.Block head = world.getBlockAt(x, y + 1, z);
                    if (isSafeGround(ground) && isSafeAir(feet) && isSafeAir(head)) {
                        return new Location(world, x + 0.5, y, z + 0.5);
                    }
                }
            } else {
                int y = world.getHighestBlockYAt(x, z);
                org.bukkit.block.Block ground = world.getBlockAt(x, y, z);
                org.bukkit.block.Block feet = world.getBlockAt(x, y + 1, z);
                org.bukkit.block.Block head = world.getBlockAt(x, y + 2, z);
                if (isSafeGround(ground) && isSafeAir(feet) && isSafeAir(head)) {
                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
            }
        }
        return null;
    }

    private boolean isSafeGround(org.bukkit.block.Block block) {
        if (block == null) return false;
        Material mat = block.getType();
        if (!mat.isSolid()) return false;
        String name = mat.name();
        if (name.contains("LEAVES") || name.contains("CACTUS") || name.contains("MAGMA") || name.contains("FIRE") || name.contains("LAVA") || name.contains("WATER") || mat == Material.BEDROCK || name.contains("PORTAL")) {
            return false;
        }
        return true;
    }

    private boolean isSafeAir(org.bukkit.block.Block block) {
        if (block == null) return false;
        Material mat = block.getType();
        if (mat.isSolid()) return false;
        String name = mat.name();
        if (name.contains("LAVA") || name.contains("WATER") || name.contains("FIRE") || name.contains("PORTAL")) {
            return false;
        }
        return true;
    }

    private boolean handleShardManagerCommand(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(color("&8&m----------------------------------------"));
            sender.sendMessage(color("&b&lGerenciador de Fragmentos (Shards)"));
            sender.sendMessage(color("&e/shardmanager view <player> &f- Visualiza os shards do jogador"));
            sender.sendMessage(color("&e/shardmanager give <player> <quantia> &f- Dá shards ao jogador"));
            sender.sendMessage(color("&e/shardmanager remove <player> <quantia> &f- Remove shards do jogador"));
            sender.sendMessage(color("&e/shardmanager reset <player> &f- Reseta os shards do jogador para 0"));
            sender.sendMessage(color("&8&m----------------------------------------"));
            return true;
        }
        String sub = args[0].toLowerCase();
        if (args.length < 2) {
            sender.sendMessage(color("&cUso correto: /shardmanager " + sub + " <player> " + (sub.equals("view") || sub.equals("reset") ? "" : "<quantia>")));
            return true;
        }
        String targetName = args[1];
        Player targetOnline = Bukkit.getPlayer(targetName);
        PlayerProfile targetProf = null;
        if (targetOnline != null) {
            targetProf = getProfile(targetOnline);
        } else {
            for (PlayerProfile prof : profiles.values()) {
                if (prof.username != null && prof.username.equalsIgnoreCase(targetName)) {
                    targetProf = prof;
                    break;
                }
            }
            if (targetProf == null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
                if (op != null && (op.hasPlayedBefore() || op.isOnline())) {
                    targetProf = getProfile(op);
                }
            }
        }
        if (targetProf == null) {
            sender.sendMessage(color("&cJogador '" + targetName + "' não encontrado no banco de dados!"));
            return true;
        }

        switch (sub) {
            case "view":
            case "ver":
                sender.sendMessage(color("&#FFF000" + (targetProf.username != null ? targetProf.username : targetName) + " &7tem &#DF9FFF" + targetProf.shards));
                break;
            case "give":
            case "add":
            case "dar":
                if (args.length < 3) {
                    sender.sendMessage(color("&cUso correto: /shardmanager give " + targetName + " <quantia>"));
                    return true;
                }
                try {
                    long amt = Long.parseLong(args[2]);
                    if (amt <= 0) {
                        sender.sendMessage(color("&cA quantia deve ser maior que zero!"));
                        return true;
                    }
                    targetProf.shards += amt;
                    saveProfile(targetProf);
                    if (targetOnline != null) {
                        updateScoreboard(targetOnline, targetProf);
                    }
                    sender.sendMessage(color("&#DF9FFF+" + amt + " &7enviado para &#FFF000" + (targetProf.username != null ? targetProf.username : targetName)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(color("&cQuantia inválida: '" + args[2] + "'"));
                }
                break;
            case "remove":
            case "take":
            case "tirar":
                if (args.length < 3) {
                    sender.sendMessage(color("&cUso correto: /shardmanager remove " + targetName + " <quantia>"));
                    return true;
                }
                try {
                    long amt = Long.parseLong(args[2]);
                    if (amt <= 0) {
                        sender.sendMessage(color("&cA quantia deve ser maior que zero!"));
                        return true;
                    }
                    targetProf.shards = Math.max(0L, targetProf.shards - amt);
                    saveProfile(targetProf);
                    if (targetOnline != null) {
                        updateScoreboard(targetOnline, targetProf);
                    }
                    sender.sendMessage(color("&c-" + amt + " &7removidos de &#FFF000" + (targetProf.username != null ? targetProf.username : targetName)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(color("&cQuantia inválida: '" + args[2] + "'"));
                }
                break;
            case "reset":
            case "zerar":
                targetProf.shards = 0L;
                saveProfile(targetProf);
                if (targetOnline != null) {
                    updateScoreboard(targetOnline, targetProf);
                }
                sender.sendMessage(color("&7Fragmentos resetado de &#FFF000" + (targetProf.username != null ? targetProf.username : targetName)));
                break;
            default:
                sender.sendMessage(color("&cSubcomando desconhecido. Use /shardmanager help"));
                break;
        }
        return true;
    }

    private boolean handleMoneyManagerCommand(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(color("&8&m----------------------------------------"));
            sender.sendMessage(color("&b&lGerenciador de Dinheiro (Money)"));
            sender.sendMessage(color("&e/moneymanager view <player> &f- Visualiza o dinheiro do jogador"));
            sender.sendMessage(color("&e/moneymanager give <player> <quantia> &f- Dá dinheiro ao jogador"));
            sender.sendMessage(color("&e/moneymanager remove <player> <quantia> &f- Remove dinheiro do jogador"));
            sender.sendMessage(color("&e/moneymanager reset <player> &f- Reseta o dinheiro do jogador para 0"));
            sender.sendMessage(color("&8&m----------------------------------------"));
            return true;
        }
        String sub = args[0].toLowerCase();
        if (args.length < 2) {
            sender.sendMessage(color("&cUso correto: /moneymanager " + sub + " <player> " + (sub.equals("view") || sub.equals("reset") ? "" : "<quantia>")));
            return true;
        }
        String targetName = args[1];
        Player targetOnline = Bukkit.getPlayer(targetName);
        PlayerProfile targetProf = null;
        if (targetOnline != null) {
            targetProf = getProfile(targetOnline);
        } else {
            for (PlayerProfile prof : profiles.values()) {
                if (prof.username != null && prof.username.equalsIgnoreCase(targetName)) {
                    targetProf = prof;
                    break;
                }
            }
            if (targetProf == null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
                if (op != null && (op.hasPlayedBefore() || op.isOnline())) {
                    targetProf = getProfile(op);
                }
            }
        }
        if (targetProf == null) {
            sender.sendMessage(color("&cJogador '" + targetName + "' não encontrado no banco de dados!"));
            return true;
        }

        switch (sub) {
            case "view":
            case "ver":
                sender.sendMessage(color("&#FFF000" + (targetProf.username != null ? targetProf.username : targetName) + " &7tem &a$" + String.format(Locale.US, "%.2f", targetProf.money)));
                break;
            case "give":
            case "add":
            case "dar":
                if (args.length < 3) {
                    sender.sendMessage(color("&cUso correto: /moneymanager give " + targetName + " <quantia>"));
                    return true;
                }
                try {
                    double amt = Double.parseDouble(args[2].replace(",", "."));
                    if (amt <= 0) {
                        sender.sendMessage(color("&cA quantia deve ser maior que zero!"));
                        return true;
                    }
                    targetProf.money += amt;
                    saveProfile(targetProf);
                    if (targetOnline != null) {
                        updateScoreboard(targetOnline, targetProf);
                    }
                    sender.sendMessage(color("&a+$" + String.format(Locale.US, "%.2f", amt) + " &7enviado para &#FFF000" + (targetProf.username != null ? targetProf.username : targetName)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(color("&cQuantia inválida: '" + args[2] + "'"));
                }
                break;
            case "remove":
            case "take":
            case "tirar":
                if (args.length < 3) {
                    sender.sendMessage(color("&cUso correto: /moneymanager remove " + targetName + " <quantia>"));
                    return true;
                }
                try {
                    double amt = Double.parseDouble(args[2].replace(",", "."));
                    if (amt <= 0) {
                        sender.sendMessage(color("&cA quantia deve ser maior que zero!"));
                        return true;
                    }
                    targetProf.money = Math.max(0.0, targetProf.money - amt);
                    saveProfile(targetProf);
                    if (targetOnline != null) {
                        updateScoreboard(targetOnline, targetProf);
                    }
                    sender.sendMessage(color("&c-$" + String.format(Locale.US, "%.2f", amt) + " &7removidos de &#FFF000" + (targetProf.username != null ? targetProf.username : targetName)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(color("&cQuantia inválida: '" + args[2] + "'"));
                }
                break;
            case "reset":
            case "zerar":
                targetProf.money = 0.0;
                saveProfile(targetProf);
                if (targetOnline != null) {
                    updateScoreboard(targetOnline, targetProf);
                }
                sender.sendMessage(color("&7Money resetado de &#FFF000" + (targetProf.username != null ? targetProf.username : targetName)));
                break;
            default:
                sender.sendMessage(color("&cSubcomando desconhecido. Use /moneymanager help"));
                break;
        }
        return true;
    }

    private boolean handlePlayerManagerCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUso correto: /playermanager <view|reset|save|estatística>"));
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("save")) {
            for (PlayerProfile prof : profiles.values()) {
                if (prof != null) saveProfile(prof);
            }
            sender.sendMessage(color("&aTodos os dados dos jogadores foram salvos com sucesso!"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUso correto: /playermanager " + sub + " <player>"));
            return true;
        }
        if (sub.equals("view")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(color("&cApenas jogadores podem abrir o menu GUI!"));
                return true;
            }
            String targetName = args[1];
            Player targetOnline = Bukkit.getPlayer(targetName);
            PlayerProfile targetProf = null;
            if (targetOnline != null) {
                targetProf = getProfile(targetOnline);
            } else {
                for (PlayerProfile prof : profiles.values()) {
                    if (prof.username != null && prof.username.equalsIgnoreCase(targetName)) {
                        targetProf = prof;
                        break;
                    }
                }
                if (targetProf == null) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
                    if (op != null && (op.hasPlayedBefore() || op.isOnline())) {
                        targetProf = getProfile(op);
                    }
                }
            }
            if (targetProf == null) {
                sender.sendMessage(color("&cJogador '" + targetName + "' não encontrado no banco de dados!"));
                return true;
            }
            Player p = (Player) sender;
            viewingPlayerManagerProfile.put(p.getUniqueId(), targetProf.uuid);
            openGUI(p, "PLAYERMANAGER-MENU");
            return true;
        }

        List<String> validStats = Arrays.asList("playtime", "money", "blocksplaced", "blocksbreak", "blocksbroken", "kills", "deaths", "moneyspent", "moneymade", "killstreak", "highestkillstreak", "mobskilled", "shards", "all");
        if (sub.equals("reset") || sub.equals("zerar") || validStats.contains(sub)) {
            String targetName;
            String statType = "all";
            if (sub.equals("reset") || sub.equals("zerar")) {
                if (args.length >= 3 && validStats.contains(args[1].toLowerCase())) {
                    statType = args[1].toLowerCase();
                    targetName = args[2];
                } else {
                    targetName = args[1];
                }
            } else {
                statType = sub;
                targetName = args[1];
            }

            Player targetOnline = Bukkit.getPlayer(targetName);
            PlayerProfile targetProf = null;
            if (targetOnline != null) {
                targetProf = getProfile(targetOnline);
            } else {
                for (PlayerProfile prof : profiles.values()) {
                    if (prof.username != null && prof.username.equalsIgnoreCase(targetName)) {
                        targetProf = prof;
                        break;
                    }
                }
                if (targetProf == null) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
                    if (op != null && (op.hasPlayedBefore() || op.isOnline())) {
                        targetProf = getProfile(op);
                    }
                }
            }
            if (targetProf == null) {
                sender.sendMessage(color("&cJogador '" + targetName + "' não encontrado no banco de dados!"));
                return true;
            }
            switch (statType) {
                case "playtime": targetProf.playtimeSeconds = 0; break;
                case "money": targetProf.money = 0.0; break;
                case "blocksplaced": targetProf.blocksPlaced = 0; break;
                case "blocksbreak":
                case "blocksbroken": targetProf.blocksBroken = 0; break;
                case "kills": targetProf.kills = 0; break;
                case "deaths": targetProf.deaths = 0; break;
                case "moneyspent": targetProf.moneySpent = 0.0; break;
                case "moneymade": targetProf.moneyMade = 0.0; break;
                case "killstreak": targetProf.killStreak = 0; break;
                case "highestkillstreak": targetProf.highestKillStreak = 0; break;
                case "mobskilled": targetProf.mobsKilled = 0; break;
                case "shards": targetProf.shards = 0; break;
                case "all":
                default:
                    targetProf.playtimeSeconds = 0;
                    targetProf.money = 0.0;
                    targetProf.blocksPlaced = 0;
                    targetProf.blocksBroken = 0;
                    targetProf.kills = 0;
                    targetProf.deaths = 0;
                    targetProf.moneySpent = 0.0;
                    targetProf.moneyMade = 0.0;
                    targetProf.killStreak = 0;
                    targetProf.highestKillStreak = 0;
                    targetProf.mobsKilled = 0;
                    targetProf.shards = 0;
                    break;
            }
            saveProfile(targetProf);
            if (targetOnline != null) updateScoreboard(targetOnline, targetProf);
            sender.sendMessage(color("&aO dado &f" + statType.toUpperCase() + " &ado jogador &e" + (targetProf.username != null ? targetProf.username : targetName) + " &afoi resetado com sucesso!"));
            return true;
        } else {
            sender.sendMessage(color("&cUso correto: /playermanager <view|reset|save|estatística>"));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        String name = cmd.getName().toLowerCase();
        if ((name.equals("rtp") || name.equals("wild")) && args.length == 1) {
            return Arrays.asList("overworld", "nether", "end");
        }
        if (name.equals("sell") && args.length == 1) {
            return Collections.singletonList("all");
        }
        if (name.equals("spawn")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>(Arrays.asList("1", "2"));
                if (sender.hasPermission("donutcore.admin")) list.add("set");
                return list;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("set") && sender.hasPermission("donutcore.admin")) {
                return Arrays.asList("1", "2", "spawn1", "spawn2");
            }
            return Collections.emptyList();
        }
        if (name.equals("afk")) {
            if (args.length == 1) {
                return Arrays.asList("1", "2");
            }
            return Collections.emptyList();
        }
        if (name.equals("stash")) {
            if (args.length == 1) return Arrays.asList("summon", "1", "2");
            if (args.length == 2 && args[0].equalsIgnoreCase("summon")) return Arrays.asList("1", "2");
            return Collections.emptyList();
        }
        if (name.equals("team") && args.length == 1) {
            return Arrays.asList("create", "invite", "join", "kick", "leave", "disband", "home", "sethome", "delhome", "pvp", "chat");
        }
        if (name.equals("team") && args.length == 2 && Arrays.asList("invite", "join", "kick").contains(args[0].toLowerCase())) {
            return null;
        }
        if (name.equals("donutcore") || name.equals("dcore") || name.equals("dccore")) {
            if (args.length == 1) return Arrays.asList("reload", "wand", "cuboid", "afk", "spawn", "amethyst", "shardmanager", "moneymanager", "playermanager", "warpmanager");
            if (args.length == 2 && args[0].equalsIgnoreCase("warpmanager")) return Arrays.asList("create", "delete", "list");
            if (args.length == 3 && args[0].equalsIgnoreCase("warpmanager") && (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("remove"))) {
                ConfigurationSection sec = getWarpsConfig().getConfigurationSection("WARPS");
                if (sec != null && !sec.getKeys(false).isEmpty()) return new ArrayList<>(sec.getKeys(false));
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("shardmanager") || args[0].equalsIgnoreCase("moneymanager"))) {
                return Arrays.asList("view", "give", "remove", "reset");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("playermanager")) {
                return Arrays.asList("view", "reset", "save", "playtime", "money", "blocksplaced", "blocksbroken", "kills", "deaths", "moneyspent", "moneymade", "killstreak", "highestkillstreak", "mobskilled", "shards");
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("playermanager") && args[1].equalsIgnoreCase("reset")) {
                return Arrays.asList("all", "playtime", "money", "blocksplaced", "blocksbroken", "kills", "deaths", "moneyspent", "moneymade", "killstreak", "highestkillstreak", "mobskilled", "shards");
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("playermanager") && args[1].equalsIgnoreCase("save")) {
                return Collections.singletonList("all");
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("playermanager") && (args[1].equalsIgnoreCase("view") || Arrays.asList("playtime", "money", "blocksplaced", "blocksbroken", "kills", "deaths", "moneyspent", "moneymade", "killstreak", "highestkillstreak", "mobskilled", "shards").contains(args[1].toLowerCase()))) {
                return null;
            }
            if (args.length == 4 && args[0].equalsIgnoreCase("playermanager") && args[1].equalsIgnoreCase("reset")) {
                return null;
            }
            if (args.length == 4 && (args[0].equalsIgnoreCase("shardmanager") || args[0].equalsIgnoreCase("moneymanager")) && (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("remove"))) {
                return Arrays.asList("100", "500", "1000", "5000", "10000");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("cuboid")) return Arrays.asList("list", "create", "delete");
            if (args.length == 3 && args[0].equalsIgnoreCase("cuboid")) {
                if (args[1].equalsIgnoreCase("create")) return Arrays.asList("spawn1", "spawn2", "afk1", "afk2");
                if (args[1].equalsIgnoreCase("delete")) {
                    ConfigurationSection cubSec = getCuboidsConfig().getConfigurationSection("CUBOIDS");
                    if (cubSec != null && !cubSec.getKeys(false).isEmpty()) return new ArrayList<>(cubSec.getKeys(false));
                    return Arrays.asList("spawn1", "spawn2", "afk1", "afk2");
                }
                return Collections.emptyList();
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("afk")) return Arrays.asList("list", "set", "delete");
            if (args.length == 3 && args[0].equalsIgnoreCase("afk") && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("delete"))) {
                Set<String> suggestions = new HashSet<>();
                ConfigurationSection cubSec = getCuboidsConfig().getConfigurationSection("CUBOIDS");
                if (cubSec != null) {
                    for (String k : cubSec.getKeys(false)) {
                        if (k.toLowerCase().startsWith("afk")) suggestions.add(k);
                    }
                }
                ConfigurationSection spSec = getConfig().getConfigurationSection("LOCATIONS.AFK-SPAWNS");
                if (spSec != null) suggestions.addAll(spSec.getKeys(false));
                if (suggestions.isEmpty()) return Arrays.asList("afk1", "afk2");
                return new ArrayList<>(suggestions);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) return Arrays.asList("list", "set", "delete");
            if (args.length == 3 && args[0].equalsIgnoreCase("spawn") && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("delete"))) {
                Set<String> suggestions = new HashSet<>();
                ConfigurationSection cubSec = getCuboidsConfig().getConfigurationSection("CUBOIDS");
                if (cubSec != null) {
                    for (String k : cubSec.getKeys(false)) {
                        if (k.toLowerCase().startsWith("spawn") || k.equals("1") || k.equals("2")) suggestions.add(k);
                    }
                }
                ConfigurationSection spSec = getConfig().getConfigurationSection("LOCATIONS.SPAWN-SPAWNS");
                if (spSec != null) suggestions.addAll(spSec.getKeys(false));
                if (suggestions.isEmpty()) return Arrays.asList("spawn1", "spawn2");
                return new ArrayList<>(suggestions);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("amethyst")) return Arrays.asList("pickaxe", "axe", "sellaxe", "shovel", "bucket", "booster");
            if (args.length == 3 && args[0].equalsIgnoreCase("amethyst")) return null;
            if (args.length == 4 && args[0].equalsIgnoreCase("amethyst")) return Arrays.asList("1s", "10m", "1h", "1d", "24h");
        }
        if (name.equals("warpmanager")) {
            if (args.length == 1) return Arrays.asList("create", "delete", "list");
            if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove"))) {
                ConfigurationSection sec = getWarpsConfig().getConfigurationSection("WARPS");
                if (sec != null && !sec.getKeys(false).isEmpty()) return new ArrayList<>(sec.getKeys(false));
            }
            return Collections.emptyList();
        }
        if (name.equals("warp")) {
            if (args.length == 1) {
                ConfigurationSection sec = getWarpsConfig().getConfigurationSection("WARPS");
                if (sec != null && !sec.getKeys(false).isEmpty()) return new ArrayList<>(sec.getKeys(false));
            }
            return Collections.emptyList();
        }
        if (name.equals("playermanager")) {
            if (args.length == 1) return Arrays.asList("view", "reset", "save", "playtime", "money", "blocksplaced", "blocksbroken", "kills", "deaths", "moneyspent", "moneymade", "killstreak", "highestkillstreak", "mobskilled", "shards");
            if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
                return Arrays.asList("all", "playtime", "money", "blocksplaced", "blocksbroken", "kills", "deaths", "moneyspent", "moneymade", "killstreak", "highestkillstreak", "mobskilled", "shards");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("save")) {
                return Collections.singletonList("all");
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("view") || Arrays.asList("playtime", "money", "blocksplaced", "blocksbroken", "kills", "deaths", "moneyspent", "moneymade", "killstreak", "highestkillstreak", "mobskilled", "shards").contains(args[0].toLowerCase()))) {
                return null;
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
                return null;
            }
            return Collections.emptyList();
        }
        if (name.equals("shardmanager") || name.equals("moneymanager")) {
            if (args.length == 1) return Arrays.asList("view", "give", "remove", "reset");
            if (args.length == 2) return null;
            if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove"))) return Arrays.asList("100", "500", "1000", "5000", "10000");
            return Collections.emptyList();
        }
        if (name.equals("afk") || name.equals("spawn") || name.equals("hub")) {
            return Collections.emptyList();
        }
        if (name.equals("bounty") || name.equals("recompensas")) {
            if (args.length == 1) return Arrays.asList("add");
            if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(java.util.stream.Collectors.toList());
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("add")) return Arrays.asList("100", "500", "1000", "5000", "10000");
        }
        if (Arrays.asList("pay", "tpa", "tpahere", "tpaccept", "tpdeny", "tpno", "findplayer", "stats", "status", "perfil", "profile", "fragmentos", "fragmento", "shards", "shard", "fragmentopay", "tell", "msg", "w", "endersee", "report", "reportar", "ignore", "ignorar", "ping").contains(name) && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(java.util.stream.Collectors.toList());
        }
        if (Arrays.asList("pay", "fragmentopay").contains(name) && args.length == 2) {
            return Arrays.asList("100", "500", "1000", "5000", "10000");
        }
        if ((name.equals("home") || name.equals("delhome") || name.equals("sethome")) && args.length == 1 && sender instanceof Player) {
            PlayerProfile prof = getProfile((Player) sender);
            return new ArrayList<>(prof.homes.keySet());
        }
        if ((name.equals("tell") || name.equals("msg") || name.equals("w") || name.equals("ignore") || name.equals("ignorar") || name.equals("blocktell") || name.equals("bloqueartell")) && args.length == 1) {
            return null;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings({"all", "deprecation", "removal", "unused"})
    public static class DonutVaultEconomy implements net.milkbowl.vault.economy.Economy {
        private final DonutCore plugin;
        public DonutVaultEconomy(DonutCore plugin) { this.plugin = plugin; }
        @Override public boolean isEnabled() { return plugin.isEnabled(); }
        @Override public String getName() { return "DonutCore Economy"; }
        @Override public boolean hasBankSupport() { return false; }
        @Override public int fractionalDigits() { return 2; }
        @Override public String format(double amount) { return "$" + String.format("%.2f", amount); }
        @Override public String currencyNamePlural() { return "$"; }
        @Override public String currencyNameSingular() { return "$"; }
        @Override public boolean hasAccount(String playerName) { return true; }
        @Override public boolean hasAccount(OfflinePlayer player) { return true; }
        @Override public boolean hasAccount(String playerName, String worldName) { return true; }
        @Override public boolean hasAccount(OfflinePlayer player, String worldName) { return true; }
        @Override public double getBalance(String playerName) { return getBalance(Bukkit.getOfflinePlayer(playerName)); }
        @Override public double getBalance(OfflinePlayer player) { PlayerProfile prof = plugin.profiles.get(player.getUniqueId()); return prof != null ? prof.money : 0.0; }
        @Override public double getBalance(String playerName, String world) { return getBalance(playerName); }
        @Override public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }
        @Override public boolean has(String playerName, double amount) { return getBalance(playerName) >= amount; }
        @Override public boolean has(OfflinePlayer player, double amount) { return getBalance(player) >= amount; }
        @Override public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }
        @Override public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }
        @Override public net.milkbowl.vault.economy.EconomyResponse withdrawPlayer(String playerName, double amount) { return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount); }
        @Override public net.milkbowl.vault.economy.EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
            PlayerProfile prof = plugin.profiles.get(player.getUniqueId());
            if (prof == null) return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.FAILURE, "Account not found");
            if (prof.money < amount) return new net.milkbowl.vault.economy.EconomyResponse(0, prof.money, net.milkbowl.vault.economy.EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
            prof.money -= amount;
            return new net.milkbowl.vault.economy.EconomyResponse(amount, prof.money, net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS, null);
        }
        @Override public net.milkbowl.vault.economy.EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
        @Override public net.milkbowl.vault.economy.EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }
        @Override public net.milkbowl.vault.economy.EconomyResponse depositPlayer(String playerName, double amount) { return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount); }
        @Override public net.milkbowl.vault.economy.EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
            PlayerProfile prof = plugin.profiles.get(player.getUniqueId());
            if (prof == null) return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.FAILURE, "Account not found");
            prof.money += amount;
            return new net.milkbowl.vault.economy.EconomyResponse(amount, prof.money, net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS, null);
        }
        @Override public net.milkbowl.vault.economy.EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
        @Override public net.milkbowl.vault.economy.EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }
        @Override public net.milkbowl.vault.economy.EconomyResponse createBank(String name, String player) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse createBank(String name, OfflinePlayer player) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse deleteBank(String name) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse bankBalance(String name) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse bankHas(String name, double amount) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse bankWithdraw(String name, double amount) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse bankDeposit(String name, double amount) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse isBankOwner(String name, String playerName) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse isBankOwner(String name, OfflinePlayer player) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse isBankMember(String name, String playerName) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public net.milkbowl.vault.economy.EconomyResponse isBankMember(String name, OfflinePlayer player) { return new net.milkbowl.vault.economy.EconomyResponse(0, 0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
        @Override public List<String> getBanks() { return Collections.emptyList(); }
        @Override public boolean createPlayerAccount(String playerName) { return true; }
        @Override public boolean createPlayerAccount(OfflinePlayer player) { return true; }
        @Override public boolean createPlayerAccount(String playerName, String worldName) { return true; }
        @Override public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return true; }
    }
}

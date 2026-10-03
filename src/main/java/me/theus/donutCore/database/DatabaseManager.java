package me.theus.donutCore.database;

import me.theus.donutCore.DonutCore;
import me.theus.donutCore.DonutCore.PlayerProfile;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.*;

@SuppressWarnings({"all", "deprecation", "removal", "unused", "SpellCheckingInspection", "ConstantConditions", "DuplicatedCode", "ResultOfMethodCallIgnored", "CallToPrintStackTrace"})
public class DatabaseManager {

    private final DonutCore plugin;
    private String type = "YAML";
    private Connection connection;
    private boolean connected = false;

    // MySQL info
    private String host, database, user, password, params;
    private int port;

    // SQLite info
    private File sqliteFile;

    public DatabaseManager(DonutCore plugin) {
        this.plugin = plugin;
    }

    public void init() {
        close();
        File dbFile = new File(plugin.getDataFolder(), "database.yml");
        if (!dbFile.exists()) {
            plugin.saveResource("database.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dbFile);
        this.type = config.getString("DATABASE", "MYSQL").toUpperCase();

        if ("YAML".equals(type) || "MONGODB".equals(type)) {
            if ("MONGODB".equals(type)) {
                plugin.getLogger().warning("DonutCore: MONGODB não está ativado no momento, usando armazenamento em MySQL/YAML.");
            }
            this.connected = false;
            return;
        }

        if ("MYSQL".equals(type)) {
            this.host = config.getString("MYSQL.HOST", "127.0.0.1");
            this.port = config.getInt("MYSQL.PORT", 3306);
            this.database = config.getString("MYSQL.DATABASE", "donutcore");
            this.user = config.getString("MYSQL.USER", "root");
            this.password = config.getString("MYSQL.PASSWORD", "");
            this.params = config.getString("MYSQL.PARAMS", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        } else if ("SQLITE".equals(type)) {
            String filePath = config.getString("SQLITE.FILE", "plugins/DonutCore/donutcore.db");
            this.sqliteFile = new File(plugin.getDataFolder(), "donutcore.db");
            if (!this.sqliteFile.getParentFile().exists()) {
                this.sqliteFile.getParentFile().mkdirs();
            }
        }

        connectAndCreateTables();
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed() && connection.isValid(2)) {
            return connection;
        }
        if ("MYSQL".equals(type)) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                } catch (ClassNotFoundException ignored) {}
            }
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + params;
            connection = DriverManager.getConnection(url, user, password);
        } else if ("SQLITE".equals(type)) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException ignored) {}
            String url = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
        }
        return connection;
    }

    private void connectAndCreateTables() {
        try {
            Connection conn = getConnection();
            this.connected = (conn != null && !conn.isClosed());
            if (this.connected) {
                createTables();
                plugin.getLogger().info("DonutCore: Conectado com sucesso ao banco de dados (" + type + ")!");
            }
        } catch (Exception e) {
            this.connected = false;
            plugin.getLogger().severe("DonutCore: Erro ao conectar ao banco de dados (" + type + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        String createSql;
        if ("MYSQL".equals(type)) {
            createSql = "CREATE TABLE IF NOT EXISTS donutcore_profiles (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(64), " +
                    "money DOUBLE DEFAULT 1000.0, " +
                    "shards BIGINT DEFAULT 0, " +
                    "kills INT DEFAULT 0, " +
                    "deaths INT DEFAULT 0, " +
                    "playtime BIGINT DEFAULT 0, " +
                    "blocksPlaced BIGINT DEFAULT 0, " +
                    "blocksBroken BIGINT DEFAULT 0, " +
                    "mobsKilled BIGINT DEFAULT 0, " +
                    "killStreak INT DEFAULT 0, " +
                    "highestKillStreak INT DEFAULT 0, " +
                    "moneySpent DOUBLE DEFAULT 0.0, " +
                    "moneyMade DOUBLE DEFAULT 0.0, " +
                    "team VARCHAR(64) DEFAULT '', " +
                    "tpAuto BOOLEAN DEFAULT FALSE, " +
                    "phantomDisabled BOOLEAN DEFAULT FALSE, " +
                    "scoreboardVisible BOOLEAN DEFAULT TRUE, " +
                    "tpaEnabled BOOLEAN DEFAULT TRUE, " +
                    "permEditHome BOOLEAN DEFAULT FALSE, " +
                    "permManageTeammates BOOLEAN DEFAULT FALSE, " +
                    "permTogglePvp BOOLEAN DEFAULT FALSE, " +
                    "permVisitHome BOOLEAN DEFAULT TRUE, " +
                    "permTeamChat BOOLEAN DEFAULT TRUE, " +
                    "permHelper BOOLEAN DEFAULT FALSE, " +
                    "nightVisionEnabled BOOLEAN DEFAULT FALSE, " +
                    "blockTell BOOLEAN DEFAULT FALSE, " +
                    "ignoredPlayers_json LONGTEXT, " +
                    "homes_json LONGTEXT, " +
                    "multipliers_json LONGTEXT, " +
                    "progress_json LONGTEXT, " +
                    "soldPriceHistory_json LONGTEXT, " +
                    "soldAmountHistory_json LONGTEXT, " +
                    "lastSoldTime_json LONGTEXT, " +
                    "enderchest_data LONGTEXT, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

            createSql += " CREATE TABLE IF NOT EXISTS donutcore_teams (" +
                    "name VARCHAR(64) PRIMARY KEY, " +
                    "leader VARCHAR(36), " +
                    "pvp BOOLEAN DEFAULT FALSE, " +
                    "home_json LONGTEXT, " +
                    "members_json LONGTEXT, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            createSql += " CREATE TABLE IF NOT EXISTS donutcore_cuboids (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "world VARCHAR(64), " +
                    "min_x DOUBLE, " +
                    "min_y DOUBLE, " +
                    "min_z DOUBLE, " +
                    "max_x DOUBLE, " +
                    "max_y DOUBLE, " +
                    "max_z DOUBLE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            createSql += " CREATE TABLE IF NOT EXISTS donutcore_warps (" +
                    "name VARCHAR(64) PRIMARY KEY, " +
                    "location VARCHAR(255)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        } else {
            createSql = "CREATE TABLE IF NOT EXISTS donutcore_profiles (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "username TEXT, " +
                    "money REAL DEFAULT 1000.0, " +
                    "shards INTEGER DEFAULT 0, " +
                    "kills INTEGER DEFAULT 0, " +
                    "deaths INTEGER DEFAULT 0, " +
                    "playtime INTEGER DEFAULT 0, " +
                    "blocksPlaced INTEGER DEFAULT 0, " +
                    "blocksBroken INTEGER DEFAULT 0, " +
                    "mobsKilled INTEGER DEFAULT 0, " +
                    "killStreak INTEGER DEFAULT 0, " +
                    "highestKillStreak INTEGER DEFAULT 0, " +
                    "moneySpent REAL DEFAULT 0.0, " +
                    "moneyMade REAL DEFAULT 0.0, " +
                    "team TEXT DEFAULT '', " +
                    "tpAuto INTEGER DEFAULT 0, " +
                    "phantomDisabled INTEGER DEFAULT 0, " +
                    "scoreboardVisible INTEGER DEFAULT 1, " +
                    "tpaEnabled INTEGER DEFAULT 1, " +
                    "permEditHome INTEGER DEFAULT 0, " +
                    "permManageTeammates INTEGER DEFAULT 0, " +
                    "permTogglePvp INTEGER DEFAULT 0, " +
                    "permVisitHome INTEGER DEFAULT 1, " +
                    "permTeamChat INTEGER DEFAULT 1, " +
                    "permHelper INTEGER DEFAULT 0, " +
                    "nightVisionEnabled INTEGER DEFAULT 0, " +
                    "blockTell INTEGER DEFAULT 0, " +
                    "ignoredPlayers_json TEXT, " +
                    "homes_json TEXT, " +
                    "multipliers_json TEXT, " +
                    "progress_json TEXT, " +
                    "soldPriceHistory_json TEXT, " +
                    "soldAmountHistory_json TEXT, " +
                    "lastSoldTime_json TEXT, " +
                    "enderchest_data TEXT, " +
                    "last_updated DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

            createSql += " CREATE TABLE IF NOT EXISTS donutcore_teams (" +
                    "name TEXT PRIMARY KEY, " +
                    "leader TEXT, " +
                    "pvp INTEGER DEFAULT 0, " +
                    "home_json TEXT, " +
                    "members_json TEXT, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            createSql += " CREATE TABLE IF NOT EXISTS donutcore_cuboids (" +
                    "id TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "min_x REAL, " +
                    "min_y REAL, " +
                    "min_z REAL, " +
                    "max_x REAL, " +
                    "max_y REAL, " +
                    "max_z REAL" +
                    ");";
            createSql += " CREATE TABLE IF NOT EXISTS donutcore_warps (" +
                    "name TEXT PRIMARY KEY, " +
                    "location TEXT" +
                    ");";
        }
        Connection conn = getConnection();
        if (conn == null) return;
        try (Statement st = conn.createStatement()) {
            for (String s : createSql.split(";")) {
                if (s != null && !s.trim().isEmpty()) {
                    st.execute(s.trim() + ";");
                }
            }
        }
        if ("MYSQL".equals(type)) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE donutcore_profiles ADD COLUMN IF NOT EXISTS blockTell BOOLEAN DEFAULT FALSE;");
            } catch (SQLException ignored) {}
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE donutcore_profiles ADD COLUMN IF NOT EXISTS ignoredPlayers_json LONGTEXT;");
            } catch (SQLException ignored) {}
        } else {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE donutcore_profiles ADD COLUMN blockTell INTEGER DEFAULT 0;");
            } catch (SQLException ignored) {}
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE donutcore_profiles ADD COLUMN ignoredPlayers_json TEXT;");
            } catch (SQLException ignored) {}
        }
    }

    public boolean isSql() {
        return "MYSQL".equals(type) || "SQLITE".equals(type);
    }

    public synchronized void saveProfile(PlayerProfile prof) {
        if (!isSql() || prof == null) return;
        String sql;
        if ("MYSQL".equals(type)) {
            sql = "INSERT INTO donutcore_profiles (" +
                    "uuid, username, money, shards, kills, deaths, playtime, blocksPlaced, blocksBroken, mobsKilled, " +
                    "killStreak, highestKillStreak, moneySpent, moneyMade, team, tpAuto, phantomDisabled, scoreboardVisible, " +
                    "tpaEnabled, permEditHome, permManageTeammates, permTogglePvp, permVisitHome, permTeamChat, permHelper, " +
                    "nightVisionEnabled, blockTell, ignoredPlayers_json, homes_json, multipliers_json, progress_json, soldPriceHistory_json, soldAmountHistory_json, " +
                    "lastSoldTime_json, enderchest_data) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "username=VALUES(username), money=VALUES(money), shards=VALUES(shards), kills=VALUES(kills), deaths=VALUES(deaths), " +
                    "playtime=VALUES(playtime), blocksPlaced=VALUES(blocksPlaced), blocksBroken=VALUES(blocksBroken), mobsKilled=VALUES(mobsKilled), " +
                    "killStreak=VALUES(killStreak), highestKillStreak=VALUES(highestKillStreak), moneySpent=VALUES(moneySpent), moneyMade=VALUES(moneyMade), " +
                    "team=VALUES(team), tpAuto=VALUES(tpAuto), phantomDisabled=VALUES(phantomDisabled), scoreboardVisible=VALUES(scoreboardVisible), " +
                    "tpaEnabled=VALUES(tpaEnabled), permEditHome=VALUES(permEditHome), permManageTeammates=VALUES(permManageTeammates), " +
                    "permTogglePvp=VALUES(permTogglePvp), permVisitHome=VALUES(permVisitHome), permTeamChat=VALUES(permTeamChat), permHelper=VALUES(permHelper), " +
                    "nightVisionEnabled=VALUES(nightVisionEnabled), blockTell=VALUES(blockTell), ignoredPlayers_json=VALUES(ignoredPlayers_json), homes_json=VALUES(homes_json), multipliers_json=VALUES(multipliers_json), " +
                    "progress_json=VALUES(progress_json), soldPriceHistory_json=VALUES(soldPriceHistory_json), soldAmountHistory_json=VALUES(soldAmountHistory_json), " +
                    "lastSoldTime_json=VALUES(lastSoldTime_json), enderchest_data=VALUES(enderchest_data);";
        } else {
            sql = "INSERT OR REPLACE INTO donutcore_profiles (" +
                    "uuid, username, money, shards, kills, deaths, playtime, blocksPlaced, blocksBroken, mobsKilled, " +
                    "killStreak, highestKillStreak, moneySpent, moneyMade, team, tpAuto, phantomDisabled, scoreboardVisible, " +
                    "tpaEnabled, permEditHome, permManageTeammates, permTogglePvp, permVisitHome, permTeamChat, permHelper, " +
                    "nightVisionEnabled, blockTell, ignoredPlayers_json, homes_json, multipliers_json, progress_json, soldPriceHistory_json, soldAmountHistory_json, " +
                    "lastSoldTime_json, enderchest_data) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        }

        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, prof.uuid.toString());
                ps.setString(2, prof.username);
                ps.setDouble(3, prof.money);
                ps.setLong(4, prof.shards);
                ps.setInt(5, prof.kills);
                ps.setInt(6, prof.deaths);
                ps.setLong(7, prof.playtimeSeconds);
                ps.setLong(8, prof.blocksPlaced);
                ps.setLong(9, prof.blocksBroken);
                ps.setLong(10, prof.mobsKilled);
                ps.setInt(11, prof.killStreak);
                ps.setInt(12, prof.highestKillStreak);
                ps.setDouble(13, prof.moneySpent);
                ps.setDouble(14, prof.moneyMade);
                ps.setString(15, prof.teamName != null ? prof.teamName : "");
                ps.setBoolean(16, prof.tpAuto);
                ps.setBoolean(17, prof.phantomDisabled);
                ps.setBoolean(18, prof.scoreboardVisible);
                ps.setBoolean(19, prof.tpaEnabled);
                ps.setBoolean(20, prof.permEditHome);
                ps.setBoolean(21, prof.permManageTeammates);
                ps.setBoolean(22, prof.permTogglePvp);
                ps.setBoolean(23, prof.permVisitHome);
                ps.setBoolean(24, prof.permTeamChat);
                ps.setBoolean(25, prof.permHelper);
                ps.setBoolean(26, prof.nightVisionEnabled);
                ps.setBoolean(27, prof.blockTell);
                ps.setString(28, serializeUUIDSet(prof.ignoredPlayers));
                if (prof.lastLocation != null) prof.homes.put("__LAST_LOCATION__", prof.lastLocation);
                ps.setString(29, serializeHomes(prof.homes));
                prof.homes.remove("__LAST_LOCATION__");
                ps.setString(30, serializeIntMap(prof.sellMultipliers));
                ps.setString(31, serializeDoubleMap(prof.categoryProgress));
                ps.setString(32, serializeDoubleMap(prof.soldPriceHistory));
                ps.setString(33, serializeIntMap(prof.soldAmountHistory));
                ps.setString(34, serializeLongMap(prof.lastSoldTime));
                ps.setString(35, serializeEnderchest(prof.enderchestContents));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DonutCore: Erro SQL ao salvar perfil de " + prof.username + ": " + e.getMessage());
        }
    }

    public synchronized void loadAllProfiles(Map<UUID, PlayerProfile> profiles) {
        if (!isSql()) return;
        String sql = "SELECT * FROM donutcore_profiles;";
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String username = rs.getString("username");
                    PlayerProfile prof = new PlayerProfile(uuid, username != null ? username : "Desconhecido");
                    prof.money = rs.getDouble("money");
                    prof.shards = rs.getLong("shards");
                    prof.kills = rs.getInt("kills");
                    prof.deaths = rs.getInt("deaths");
                    prof.playtimeSeconds = rs.getLong("playtime");
                    prof.blocksPlaced = rs.getLong("blocksPlaced");
                    prof.blocksBroken = rs.getLong("blocksBroken");
                    prof.mobsKilled = rs.getLong("mobsKilled");
                    prof.killStreak = rs.getInt("killStreak");
                    prof.highestKillStreak = rs.getInt("highestKillStreak");
                    prof.moneySpent = rs.getDouble("moneySpent");
                    prof.moneyMade = rs.getDouble("moneyMade");
                    prof.teamName = rs.getString("team");
                    if (prof.teamName == null) prof.teamName = "";
                    prof.tpAuto = rs.getBoolean("tpAuto");
                    prof.phantomDisabled = rs.getBoolean("phantomDisabled");
                    prof.scoreboardVisible = rs.getBoolean("scoreboardVisible");
                    prof.tpaEnabled = rs.getBoolean("tpaEnabled");
                    prof.permEditHome = rs.getBoolean("permEditHome");
                    prof.permManageTeammates = rs.getBoolean("permManageTeammates");
                    prof.permTogglePvp = rs.getBoolean("permTogglePvp");
                    prof.permVisitHome = rs.getBoolean("permVisitHome");
                    prof.permTeamChat = rs.getBoolean("permTeamChat");
                    prof.permHelper = rs.getBoolean("permHelper");
                    prof.nightVisionEnabled = rs.getBoolean("nightVisionEnabled");
                    try { prof.blockTell = rs.getBoolean("blockTell"); } catch (SQLException ignored) {}
                    try { deserializeUUIDSet(rs.getString("ignoredPlayers_json"), prof.ignoredPlayers); } catch (SQLException ignored) {}

                    deserializeHomes(rs.getString("homes_json"), prof.homes);
                    if (prof.homes.containsKey("__LAST_LOCATION__")) prof.lastLocation = prof.homes.remove("__LAST_LOCATION__");

                    deserializeIntMap(rs.getString("multipliers_json"), prof.sellMultipliers);
                    deserializeDoubleMap(rs.getString("progress_json"), prof.categoryProgress);
                    deserializeDoubleMap(rs.getString("soldPriceHistory_json"), prof.soldPriceHistory);
                    deserializeIntMap(rs.getString("soldAmountHistory_json"), prof.soldAmountHistory);
                    deserializeLongMap(rs.getString("lastSoldTime_json"), prof.lastSoldTime);
                    deserializeEnderchest(rs.getString("enderchest_data"), prof.enderchestContents);

                    prof.recalculateAllLevels();
                    profiles.put(uuid, prof);
                }
                plugin.getLogger().info("DonutCore: Carregados " + profiles.size() + " perfis do banco SQL (" + type + ").");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DonutCore: Erro ao carregar perfis do banco SQL: " + e.getMessage());
        }
    }

    public synchronized void saveTeam(DonutCore.Team team) {
        if (!isSql() || team == null) return;
        String sql;
        if ("MYSQL".equals(type)) {
            sql = "INSERT INTO donutcore_teams (name, leader, pvp, home_json, members_json) VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE leader=VALUES(leader), pvp=VALUES(pvp), home_json=VALUES(home_json), members_json=VALUES(members_json);";
        } else {
            sql = "INSERT OR REPLACE INTO donutcore_teams (name, leader, pvp, home_json, members_json) VALUES (?, ?, ?, ?, ?);";
        }
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, team.name);
                ps.setString(2, team.leader.toString());
                ps.setBoolean(3, team.pvp);
                Map<String, Location> homeMap = new HashMap<>();
                if (team.home != null) homeMap.put("home", team.home);
                ps.setString(4, serializeHomes(homeMap));
                List<String> memList = new ArrayList<>();
                for (UUID u : team.members) memList.add(u.toString());
                YamlConfiguration conf = new YamlConfiguration();
                conf.set("members", memList);
                ps.setString(5, conf.saveToString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DonutCore: Erro ao salvar time no banco SQL: " + e.getMessage());
        }
    }

    public synchronized void deleteTeam(String teamName) {
        if (!isSql() || teamName == null) return;
        String sql = "DELETE FROM donutcore_teams WHERE name = ?;";
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, teamName);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    public synchronized void loadAllTeams(Map<String, DonutCore.Team> teams) {
        if (!isSql()) return;
        String sql = "SELECT * FROM donutcore_teams;";
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    UUID leader = UUID.fromString(rs.getString("leader"));
                    DonutCore.Team team = new DonutCore.Team(name, leader);
                    team.pvp = rs.getBoolean("pvp");
                    Map<String, Location> homeMap = new HashMap<>();
                    deserializeHomes(rs.getString("home_json"), homeMap);
                    team.home = homeMap.get("home");
                    String memData = rs.getString("members_json");
                    if (memData != null && !memData.trim().isEmpty()) {
                        try {
                            YamlConfiguration conf = new YamlConfiguration();
                            conf.loadFromString(memData);
                            List<String> memList = conf.getStringList("members");
                            for (String m : memList) team.members.add(UUID.fromString(m));
                        } catch (Exception ignored) {}
                    }
                    teams.put(name.toLowerCase(), team);
                }
                plugin.getLogger().info("DonutCore: Carregados " + teams.size() + " times do banco SQL (" + type + ").");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DonutCore: Erro ao carregar times do banco SQL: " + e.getMessage());
        }
    }

    public synchronized void saveCuboids(org.bukkit.configuration.file.FileConfiguration cuboidsConfig) {
        if (!isSql() || cuboidsConfig == null) return;
        ConfigurationSection sec = cuboidsConfig.getConfigurationSection("CUBOIDS");
        if (sec == null) return;
        String sql;
        if ("MYSQL".equals(type)) {
            sql = "INSERT INTO donutcore_cuboids (id, world, min_x, min_y, min_z, max_x, max_y, max_z) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE world=VALUES(world), min_x=VALUES(min_x), min_y=VALUES(min_y), min_z=VALUES(min_z), max_x=VALUES(max_x), max_y=VALUES(max_y), max_z=VALUES(max_z);";
        } else {
            sql = "INSERT OR REPLACE INTO donutcore_cuboids (id, world, min_x, min_y, min_z, max_x, max_y, max_z) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        }
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (String key : sec.getKeys(false)) {
                    ConfigurationSection c = sec.getConfigurationSection(key);
                    if (c != null) {
                        ps.setString(1, key);
                        ps.setString(2, c.getString("WORLD", "world"));
                        ps.setDouble(3, c.getDouble("MIN-X"));
                        ps.setDouble(4, c.getDouble("MIN-Y"));
                        ps.setDouble(5, c.getDouble("MIN-Z"));
                        ps.setDouble(6, c.getDouble("MAX-X"));
                        ps.setDouble(7, c.getDouble("MAX-Y"));
                        ps.setDouble(8, c.getDouble("MAX-Z"));
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DonutCore: Erro ao salvar cuboides no banco SQL: " + e.getMessage());
        }
    }

    public synchronized void loadCuboids(org.bukkit.configuration.file.FileConfiguration cuboidsConfig) {
        if (!isSql() || cuboidsConfig == null) return;
        String sql = "SELECT * FROM donutcore_cuboids;";
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                int count = 0;
                while (rs.next()) {
                    String id = rs.getString("id");
                    cuboidsConfig.set("CUBOIDS." + id + ".WORLD", rs.getString("world"));
                    cuboidsConfig.set("CUBOIDS." + id + ".MIN-X", rs.getDouble("min_x"));
                    cuboidsConfig.set("CUBOIDS." + id + ".MIN-Y", rs.getDouble("min_y"));
                    cuboidsConfig.set("CUBOIDS." + id + ".MIN-Z", rs.getDouble("min_z"));
                    cuboidsConfig.set("CUBOIDS." + id + ".MAX-X", rs.getDouble("max_x"));
                    cuboidsConfig.set("CUBOIDS." + id + ".MAX-Y", rs.getDouble("max_y"));
                    cuboidsConfig.set("CUBOIDS." + id + ".MAX-Z", rs.getDouble("max_z"));
                    count++;
                }
                if (count > 0) {
                    plugin.getLogger().info("DonutCore: Carregados " + count + " cuboides do banco SQL (" + type + ").");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DonutCore: Erro ao carregar cuboides do banco SQL: " + e.getMessage());
        }
    }

    public synchronized void saveWarps(org.bukkit.configuration.file.FileConfiguration warpsConfig) {
        if (!isSql() || warpsConfig == null) return;
        ConfigurationSection sec = warpsConfig.getConfigurationSection("WARPS");
        if (sec == null) return;
        String sql;
        if ("MYSQL".equals(type)) {
            sql = "INSERT INTO donutcore_warps (name, location) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE location=VALUES(location);";
        } else {
            sql = "INSERT OR REPLACE INTO donutcore_warps (name, location) VALUES (?, ?);";
        }
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (String key : sec.getKeys(false)) {
                    String locStr = sec.getString(key);
                    if (locStr != null && !locStr.isEmpty()) {
                        ps.setString(1, key.toLowerCase());
                        ps.setString(2, locStr);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DonutCore: Erro ao salvar warps no banco SQL: " + e.getMessage());
        }
    }

    public synchronized void loadWarps(org.bukkit.configuration.file.FileConfiguration warpsConfig) {
        if (!isSql() || warpsConfig == null) return;
        String sql = "SELECT * FROM donutcore_warps;";
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                int count = 0;
                while (rs.next()) {
                    String name = rs.getString("name");
                    String location = rs.getString("location");
                    warpsConfig.set("WARPS." + name.toLowerCase(), location);
                    count++;
                }
                if (count > 0) {
                    plugin.getLogger().info("DonutCore: Carregadas " + count + " warps do banco SQL (" + type + ").");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DonutCore: Erro ao carregar warps do banco SQL: " + e.getMessage());
        }
    }

    public synchronized void deleteWarp(String name) {
        if (!isSql() || name == null) return;
        String sql = "DELETE FROM donutcore_warps WHERE name = ?;";
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name.toLowerCase());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DonutCore: Erro ao deletar warp do banco SQL: " + e.getMessage());
        }
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) connection.close();
            } catch (SQLException ignored) {}
            connection = null;
            connected = false;
        }
    }

    // ==========================================
    // SERIALIZATION HELPERS
    // ==========================================
    private String serializeHomes(Map<String, Location> homes) {
        if (homes == null || homes.isEmpty()) return "";
        YamlConfiguration conf = new YamlConfiguration();
        for (Map.Entry<String, Location> e : homes.entrySet()) {
            conf.set("homes." + e.getKey(), e.getValue());
        }
        return conf.saveToString();
    }

    private void deserializeHomes(String data, Map<String, Location> homes) {
        if (homes != null) homes.clear();
        if (data == null || data.trim().isEmpty() || homes == null) return;
        try {
            YamlConfiguration conf = new YamlConfiguration();
            conf.loadFromString(data);
            ConfigurationSection sec = conf.getConfigurationSection("homes");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    Location loc = sec.getLocation(key);
                    if (loc != null) homes.put(key, loc);
                }
            }
        } catch (Exception ignored) {}
    }

    private String serializeUUIDSet(Set<UUID> set) {
        if (set == null || set.isEmpty()) return "";
        YamlConfiguration conf = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (UUID uuid : set) {
            if (uuid != null) list.add(uuid.toString());
        }
        conf.set("ignored", list);
        return conf.saveToString();
    }

    private void deserializeUUIDSet(String data, Set<UUID> set) {
        if (set != null) set.clear();
        if (data == null || data.trim().isEmpty() || set == null) return;
        try {
            YamlConfiguration conf = new YamlConfiguration();
            conf.loadFromString(data);
            List<String> list = conf.getStringList("ignored");
            if (list != null) {
                for (String s : list) {
                    try { set.add(UUID.fromString(s)); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    private String serializeEnderchest(ItemStack[] contents) {
        if (contents == null) return "";
        YamlConfiguration conf = new YamlConfiguration();
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack is : contents) {
            list.add(is);
        }
        conf.set("items", list);
        return conf.saveToString();
    }

    private void deserializeEnderchest(String data, ItemStack[] contents) {
        if (data == null || data.trim().isEmpty() || contents == null) return;
        try {
            YamlConfiguration conf = new YamlConfiguration();
            conf.loadFromString(data);
            List<?> list = conf.getList("items");
            if (list != null) {
                for (int i = 0; i < list.size() && i < contents.length; i++) {
                    Object obj = list.get(i);
                    if (obj instanceof ItemStack) {
                        contents[i] = (ItemStack) obj;
                    } else {
                        contents[i] = null;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private String serializeIntMap(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        YamlConfiguration conf = new YamlConfiguration();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            conf.set("d." + e.getKey(), e.getValue());
        }
        return conf.saveToString();
    }

    private void deserializeIntMap(String data, Map<String, Integer> map) {
        if (data == null || data.trim().isEmpty() || map == null) return;
        try {
            YamlConfiguration conf = new YamlConfiguration();
            conf.loadFromString(data);
            ConfigurationSection sec = conf.getConfigurationSection("d");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    map.put(key, sec.getInt(key, 1));
                }
            }
        } catch (Exception ignored) {}
    }

    private String serializeDoubleMap(Map<String, Double> map) {
        if (map == null || map.isEmpty()) return "";
        YamlConfiguration conf = new YamlConfiguration();
        for (Map.Entry<String, Double> e : map.entrySet()) {
            conf.set("d." + e.getKey(), e.getValue());
        }
        return conf.saveToString();
    }

    private void deserializeDoubleMap(String data, Map<String, Double> map) {
        if (data == null || data.trim().isEmpty() || map == null) return;
        try {
            YamlConfiguration conf = new YamlConfiguration();
            conf.loadFromString(data);
            ConfigurationSection sec = conf.getConfigurationSection("d");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    map.put(key, sec.getDouble(key, 0.0));
                }
            }
        } catch (Exception ignored) {}
    }

    private String serializeLongMap(Map<String, Long> map) {
        if (map == null || map.isEmpty()) return "";
        YamlConfiguration conf = new YamlConfiguration();
        for (Map.Entry<String, Long> e : map.entrySet()) {
            conf.set("d." + e.getKey(), e.getValue());
        }
        return conf.saveToString();
    }

    private void deserializeLongMap(String data, Map<String, Long> map) {
        if (data == null || data.trim().isEmpty() || map == null) return;
        try {
            YamlConfiguration conf = new YamlConfiguration();
            conf.loadFromString(data);
            ConfigurationSection sec = conf.getConfigurationSection("d");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    map.put(key, sec.getLong(key, 0L));
                }
            }
        } catch (Exception ignored) {}
    }


}

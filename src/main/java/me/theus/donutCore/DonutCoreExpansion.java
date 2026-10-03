package me.theus.donutCore;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"all", "deprecation", "removal", "unused", "SpellCheckingInspection", "ConstantConditions", "DuplicatedCode", "ResultOfMethodCallIgnored", "CallToPrintStackTrace"})
public class DonutCoreExpansion extends PlaceholderExpansion {

    private final DonutCore plugin;
    private final Map<String, List<DonutCore.PlayerProfile>> cachedSortedProfiles = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCacheTime = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10_000L; // 10 seconds cache TTL

    public DonutCoreExpansion(DonutCore plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "donutcore";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Theus";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase();

        // KeyAll Cooldown / Countdown
        if (lower.equals("keyall_cooldown") || lower.equals("keyall_countdown")) {
            return plugin.formatTime(plugin.getKeyallCountdown());
        }
        if (lower.equals("keyall_cooldown_seconds") || lower.equals("keyall_countdown_seconds")) {
            return String.valueOf(plugin.getKeyallCountdown());
        }

        // RTP Zone Cooldown
        if (lower.equals("rtp_cooldown") || lower.equals("rtpzone_cooldown")) {
            return plugin.formatTime(Math.max(0, plugin.getRtpZoneCountdown()));
        }
        if (lower.equals("rtp_cooldown_seconds") || lower.equals("rtpzone_cooldown_seconds")) {
            return String.valueOf(Math.max(0, plugin.getRtpZoneCountdown()));
        }

        // ClearLag Cooldown
        if (lower.equals("clearlag_cooldown")) {
            return plugin.formatTime(Math.max(0, plugin.getClearlagCountdown()));
        }
        if (lower.equals("clearlag_cooldown_seconds")) {
            return String.valueOf(Math.max(0, plugin.getClearlagCountdown()));
        }

        // Leaderboard placeholders: %donutcore_lb_<type>_<pos>_name% or %donutcore_lb_<type>_<pos>_value%
        if (lower.startsWith("lb_")) {
            String sub = lower.substring(3);
            boolean isName = false;
            boolean isValue = false;
            boolean isRaw = false;
            if (sub.endsWith("_name")) {
                isName = true;
                sub = sub.substring(0, sub.length() - 5);
            } else if (sub.endsWith("_value_raw")) {
                isValue = true;
                isRaw = true;
                sub = sub.substring(0, sub.length() - 10);
            } else if (sub.endsWith("_value")) {
                isValue = true;
                sub = sub.substring(0, sub.length() - 6);
            }

            if (isName || isValue) {
                int lastUnderscore = sub.lastIndexOf('_');
                if (lastUnderscore != -1) {
                    String typeStr = sub.substring(0, lastUnderscore);
                    String posStr = sub.substring(lastUnderscore + 1);
                    try {
                        int pos = Integer.parseInt(posStr);
                        String normType = normalizeType(typeStr);
                        if (!normType.isEmpty() && pos >= 1) {
                            List<DonutCore.PlayerProfile> sorted = getSortedProfiles(normType);
                            if (pos <= sorted.size()) {
                                DonutCore.PlayerProfile prof = sorted.get(pos - 1);
                                if (isName) {
                                    return prof.username != null && !prof.username.isEmpty() ? prof.username : "---";
                                } else {
                                    double val = getProfileValue(prof, normType);
                                    return formatStatValue(normType, val, isRaw);
                                }
                            } else {
                                if (isName) return "---";
                                return normType.equals("playtime") ? "0m 00s" : "0";
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Rank placeholders: %donutcore_rank_<type>%
        if (lower.startsWith("rank_")) {
            String typeStr = lower.substring(5);
            String normType = normalizeType(typeStr);
            if (!normType.isEmpty()) {
                if (player == null) return "N/A";
                List<DonutCore.PlayerProfile> sorted = getSortedProfiles(normType);
                for (int i = 0; i < sorted.size(); i++) {
                    if (sorted.get(i).uuid.equals(player.getUniqueId())) {
                        return String.valueOf(i + 1);
                    }
                }
                DonutCore.PlayerProfile prof = plugin.getProfile(player);
                if (prof != null && prof.uuid != null) {
                    sorted = getSortedProfiles(normType);
                    for (int i = 0; i < sorted.size(); i++) {
                        if (sorted.get(i).uuid.equals(prof.uuid)) {
                            return String.valueOf(i + 1);
                        }
                    }
                }
                return "N/A";
            }
        }

        // Player specific placeholders
        if (player != null) {
            DonutCore.PlayerProfile prof = plugin.getProfile(player);
            if (prof != null) {
                switch (lower) {
                    case "shards":
                    case "fragmentos":
                        return DonutCore.formatAbbreviated(prof.shards);
                    case "shards_raw":
                    case "fragmentos_raw":
                        return String.valueOf(prof.shards);
                    case "money":
                    case "dinheiro":
                    case "nicestmoney":
                    case "nicest_money":
                        return DonutCore.formatAbbreviated(prof.money);
                    case "money_raw":
                    case "dinheiro_raw":
                        return String.format(Locale.US, "%.2f", prof.money);
                    case "kills":
                    case "abates":
                        return String.valueOf(prof.kills);
                    case "deaths":
                    case "mortes":
                        return String.valueOf(prof.deaths);
                    case "playtime":
                    case "tempo_jogado":
                        return plugin.formatTime(prof.playtimeSeconds);
                    case "blocksbroken":
                    case "blocks_broken":
                    case "blocosquebrados":
                        return String.valueOf(prof.blocksBroken);
                    case "blocksplaced":
                    case "blocks_placed":
                    case "blocoscolocados":
                        return String.valueOf(prof.blocksPlaced);
                    case "moneyspent":
                    case "money_spent":
                        return DonutCore.formatAbbreviated(prof.moneySpent);
                    case "moneymade":
                    case "money_made":
                        return DonutCore.formatAbbreviated(prof.moneyMade);
                    case "ping":
                        return player.getPlayer() != null ? String.valueOf(player.getPlayer().getPing()) : "0";
                    case "team":
                    case "equipe":
                        return prof.teamName != null && !prof.teamName.isEmpty() ? prof.teamName : "";
                    case "username":
                    case "player":
                    case "jogador":
                        return prof.username != null && !prof.username.isEmpty() ? prof.username : (player.getName() != null ? player.getName() : "");
                    case "booster":
                    case "booster_countdown":
                        long end = plugin.getPlayerBoosterEnd(player.getUniqueId());
                        return end > 0 ? plugin.formatTime((end - System.currentTimeMillis()) / 1000L) : "";
                    case "formatted_name":
                    case "formattedname":
                        return player.getName() != null ? player.getName() : "";
                    case "teamsuffix":
                    case "team_suffix":
                        boolean showTeam = plugin.getConfig().getBoolean("TABLIST.SHOW-TEAM-NAME", false);
                        String teamFmt = plugin.getConfig().getString("TABLIST.TEAM-FORMAT", " &7[%team%]");
                        return (showTeam && prof.teamName != null && !prof.teamName.isEmpty()) ? teamFmt.replace("%team%", prof.teamName) : "";
                }
            }
        }

        return null;
    }

    private String normalizeType(String type) {
        if (type == null) return "";
        String clean = type.toLowerCase().replace("_", "").replace("-", "");
        switch (clean) {
            case "blocksbroken":
            case "blocosquebrados":
                return "blocksBroken";
            case "blocksplaced":
            case "blocoscolocados":
                return "blocksPlaced";
            case "playtime":
            case "tempojogado":
            case "tempo":
                return "playtime";
            case "shards":
            case "fragmentos":
                return "shards";
            case "money":
            case "dinheiro":
            case "coins":
            case "saldo":
                return "money";
            case "deaths":
            case "mortes":
                return "deaths";
            case "kills":
            case "abates":
                return "kills";
            case "moneyspent":
            case "gasto":
            case "gastou":
                return "moneySpent";
            case "moneymade":
            case "ganho":
            case "lucro":
            case "faturou":
                return "moneyMade";
            case "mobskilled":
            case "mobs":
                return "mobsKilled";
            case "killstreak":
            case "streak":
                return "killStreak";
            case "highestkillstreak":
            case "maxstreak":
                return "highestKillStreak";
            default:
                return "";
        }
    }

    private double getProfileValue(DonutCore.PlayerProfile prof, String normType) {
        if (prof == null) return 0.0;
        switch (normType) {
            case "blocksBroken": return prof.blocksBroken;
            case "blocksPlaced": return prof.blocksPlaced;
            case "playtime": return prof.playtimeSeconds;
            case "shards": return prof.shards;
            case "money": return prof.money;
            case "deaths": return prof.deaths;
            case "kills": return prof.kills;
            case "moneySpent": return prof.moneySpent;
            case "moneyMade": return prof.moneyMade;
            case "mobsKilled": return prof.mobsKilled;
            case "killStreak": return prof.killStreak;
            case "highestKillStreak": return prof.highestKillStreak;
            default: return 0.0;
        }
    }

    private String formatStatValue(String normType, double val, boolean isRaw) {
        if (normType.equals("playtime")) {
            return plugin.formatTime((long) val);
        }
        if (isRaw) {
            if (val == (long) val) {
                return String.valueOf((long) val);
            }
            return String.format(Locale.US, "%.2f", val);
        }
        return DonutCore.formatAbbreviated(val);
    }

    private List<DonutCore.PlayerProfile> getSortedProfiles(String normType) {
        long now = System.currentTimeMillis();
        Long lastTime = lastCacheTime.get(normType);
        if (lastTime == null || now - lastTime > CACHE_TTL_MS || !cachedSortedProfiles.containsKey(normType)) {
            Collection<DonutCore.PlayerProfile> all = plugin.getAllProfiles();
            List<DonutCore.PlayerProfile> list = new ArrayList<>(all);
            list.sort((p1, p2) -> {
                double v1 = getProfileValue(p1, normType);
                double v2 = getProfileValue(p2, normType);
                return Double.compare(v2, v1);
            });
            cachedSortedProfiles.put(normType, list);
            lastCacheTime.put(normType, now);
            return list;
        }
        return cachedSortedProfiles.get(normType);
    }
}

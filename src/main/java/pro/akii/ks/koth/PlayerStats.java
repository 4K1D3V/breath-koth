package pro.akii.ks.koth;

import pro.akii.ks.BreathKOTH;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {

    private final BreathKOTH plugin;
    private final Map<UUID, Map<String, Map<String, Long>>> stats; // UUID -> Zone Name -> Metric (time-spent, damage-taken) -> Value

    public PlayerStats(BreathKOTH plugin) {
        this.plugin = plugin;
        this.stats = new HashMap<>();
    }

    public void incrementTime(UUID playerId, String zoneName, long ticks) {
        stats.computeIfAbsent(playerId, k -> new HashMap<>())
                .computeIfAbsent(zoneName, k -> new HashMap<>())
                .merge("time-spent", ticks, Long::sum);
    }

    public void incrementDamage(UUID playerId, String zoneName, long damage) {
        stats.computeIfAbsent(playerId, k -> new HashMap<>())
                .computeIfAbsent(zoneName, k -> new HashMap<>())
                .merge("damage-taken", damage, Long::sum);
    }

    public Map<UUID, Map<String, Map<String, Long>>> getStats() {
        return stats;
    }

    public void saveStats() {
        plugin.getConfigManager().savePlayerStats(stats);
    }

    public void loadStats() {
        stats.clear();
        stats.putAll(plugin.getConfigManager().loadPlayerStats());
    }
}
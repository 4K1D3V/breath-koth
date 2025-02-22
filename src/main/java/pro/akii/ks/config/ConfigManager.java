package pro.akii.ks.config;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pro.akii.ks.BreathKOTH;
import pro.akii.ks.koth.KothZone;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ConfigManager {

    private final BreathKOTH plugin;
    private final FileConfiguration config;
    private final File zonesFile;
    private YamlConfiguration zonesConfig;
    private final File statsFile;
    private YamlConfiguration statsConfig;

    public ConfigManager(BreathKOTH plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.zonesFile = new File(plugin.getDataFolder(), "zones.yml");
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        loadZonesConfig();
        loadStatsConfig();
    }

    private void loadZonesConfig() {
        if (!zonesFile.exists()) {
            try {
                zonesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create zones.yml", e);
            }
        }
        zonesConfig = YamlConfiguration.loadConfiguration(zonesFile);
    }

    private void loadStatsConfig() {
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create stats.yml", e);
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public List<KothZone> loadKothZones() {
        List<KothZone> zones = new ArrayList<>();
        ConfigurationSection configSection = config.getConfigurationSection("koth-zones");
        ConfigurationSection zonesSection = zonesConfig.getConfigurationSection("koth-zones");

        if (configSection != null) {
            for (String key : configSection.getKeys(false)) {
                KothZone zone = loadZone(configSection, key);
                if (zone != null) zones.add(zone);
            }
        }

        if (zonesSection != null) {
            for (String key : zonesSection.getKeys(false)) {
                zones.removeIf(zone -> zone.getName().equals(key));
                KothZone zone = loadZone(zonesSection, key);
                if (zone != null) zones.add(zone);
            }
        }

        if (zones.isEmpty()) {
            plugin.getLogger().warning("No valid KOTH zones loaded!");
        }
        return zones;
    }

    private KothZone loadZone(ConfigurationSection section, String key) {
        String worldName = section.getString(key + ".world");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Invalid world name for zone " + key + ": " + worldName);
            return null;
        }
        double x = section.getDouble(key + ".x");
        double y = section.getDouble(key + ".y");
        double z = section.getDouble(key + ".z");
        int radius = section.getInt(key + ".radius");
        int interval = section.getInt(key + ".interval", config.getInt("flame-settings.interval", 300)) * 20;
        String permission = section.getString(key + ".permission", null);
        boolean visualize = section.getBoolean(key + ".visualize", config.getBoolean("visualize-zones", true));
        List<String> events = section.getStringList(key + ".events");
        KothZone zone = new KothZone(new Location(world, x, y, z), radius, key, interval, permission);
        zone.setVisualize(visualize);
        zone.setEvents(events);
        return zone;
    }

    public void saveZones(List<KothZone> zones) {
        zonesConfig.set("koth-zones", null);
        ConfigurationSection section = zonesConfig.createSection("koth-zones");
        for (KothZone zone : zones) {
            String key = zone.getName();
            section.set(key + ".world", zone.getCenter().getWorld().getName());
            section.set(key + ".x", zone.getCenter().getX());
            section.set(key + ".y", zone.getCenter().getY());
            section.set(key + ".z", zone.getCenter().getZ());
            section.set(key + ".radius", zone.getRadius());
            section.set(key + ".interval", zone.getInterval() / 20);
            if (zone.getPermission() != null) {
                section.set(key + ".permission", zone.getPermission());
            }
            section.set(key + ".visualize", zone.isVisualized());
            section.set(key + ".events", zone.getEvents());
        }
        try {
            zonesConfig.save(zonesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save zones.yml", e);
        }
    }

    public void saveCustomEffects(String zoneName, List<Map<String, Object>> effects) {
        ConfigurationSection section = config.getConfigurationSection("custom-effects." + zoneName);
        if (section == null) {
            section = config.createSection("custom-effects." + zoneName);
        }
        List<Map<String, Object>> configEffects = new ArrayList<>();
        for (Map<String, Object> effect : effects) {
            Map<String, Object> configEffect = new HashMap<>();
            configEffect.put("type", effect.get("type"));
            configEffect.put("duration", (int) effect.get("duration") / 20);
            configEffect.put("amplifier", effect.get("amplifier"));
            configEffects.add(configEffect);
        }
        section.set("cloud-effects", configEffects);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
            plugin.getLogger().info("Saved custom effects for zone: " + zoneName);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config.yml for zone " + zoneName, e);
        }
    }

    public void savePlayerStats(Map<UUID, Map<String, Map<String, Long>>> stats) {
        for (Map.Entry<UUID, Map<String, Map<String, Long>>> entry : stats.entrySet()) {
            String uuid = entry.getKey().toString();
            ConfigurationSection playerSection = statsConfig.createSection(uuid);
            for (Map.Entry<String, Map<String, Long>> zoneEntry : entry.getValue().entrySet()) {
                ConfigurationSection zoneSection = playerSection.createSection(zoneEntry.getKey());
                zoneSection.set("time-spent", zoneEntry.getValue().getOrDefault("time-spent", 0L));
                zoneSection.set("damage-taken", zoneEntry.getValue().getOrDefault("damage-taken", 0L));
            }
        }
        try {
            statsConfig.save(statsFile);
            plugin.getLogger().info("Saved player stats to stats.yml");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", e);
        }
    }

    public Map<UUID, Map<String, Map<String, Long>>> loadPlayerStats() {
        Map<UUID, Map<String, Map<String, Long>>> stats = new HashMap<>();
        for (String uuidKey : statsConfig.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in stats.yml: " + uuidKey);
                continue;
            }
            ConfigurationSection section = statsConfig.getConfigurationSection(uuidKey);
            if (section == null) continue;

            Map<String, Map<String, Long>> zoneStats = new HashMap<>();
            for (String zone : section.getKeys(false)) {
                ConfigurationSection zoneSection = section.getConfigurationSection(zone);
                Map<String, Long> metrics = new HashMap<>();
                metrics.put("time-spent", zoneSection.getLong("time-spent", 0));
                metrics.put("damage-taken", zoneSection.getLong("damage-taken", 0));
                zoneStats.put(zone, metrics);
            }
            stats.put(uuid, zoneStats);
        }
        return stats;
    }

    public int getDuration() {
        return config.getInt("flame-settings.duration", 10) * 20;
    }

    public int getWarningTime() {
        return config.getInt("flame-settings.warning-time", 5) * 20;
    }

    public double getDamagePerTick(String kothName) {
        double customDamage = config.getDouble("custom-effects." + kothName + ".damage-per-tick", -1);
        return customDamage >= 0 ? customDamage / 2.0 : config.getDouble("flame-settings.damage-per-tick", 4) / 2.0;
    }

    public int getCloudDuration(String kothName) {
        int customDuration = config.getInt("custom-effects." + kothName + ".cloud-duration", -1);
        return customDuration >= 0 ? customDuration * 20 : config.getInt("flame-settings.cloud-duration", 20) * 20;
    }

    public List<Map<String, Object>> getCloudEffects(String kothName) {
        List<Map<String, Object>> effects = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("custom-effects." + kothName + ".cloud-effects");
        if (section != null) {
            int index = 0;
            for (Object obj : section.getList("cloud-effects", new ArrayList<>())) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> effect = (Map<String, Object>) obj;
                    Map<String, Object> formattedEffect = new HashMap<>();
                    formattedEffect.put("type", effect.get("type"));
                    formattedEffect.put("duration", ((Number) effect.get("duration")).intValue() * 20);
                    formattedEffect.put("amplifier", effect.get("amplifier"));
                    effects.add(formattedEffect);
                    index++;
                }
            }
        }
        if (effects.isEmpty()) {
            Map<String, Object> defaultEffect = new HashMap<>();
            defaultEffect.put("type", "HARM");
            defaultEffect.put("duration", config.getInt("flame-settings.cloud-duration", 20) * 20);
            defaultEffect.put("amplifier", 0);
            effects.add(defaultEffect);
        }
        return effects;
    }

    public String getParticleType(String kothName) {
        String customType = config.getString("custom-effects." + kothName + ".particle-type", null);
        return customType != null ? customType.toUpperCase() : config.getString("effects.particle-type", "DRAGON_BREATH").toUpperCase();
    }

    public int getPlayerActivationTime() {
        return config.getInt("flame-settings.player-activation-time", 30) * 20;
    }

    public boolean isSoundEnabled() {
        return config.getBoolean("effects.sound-enabled", true);
    }

    public double getParticleDensity() {
        return config.getDouble("effects.particle-density", 0.5);
    }

    public boolean isSpawnDragonEnabled() {
        return config.getBoolean("effects.spawn-dragon", true);
    }

    public int getMaxParticles() {
        return config.getInt("performance.max-particles", 100);
    }

    public boolean isDebugMode() {
        return config.getBoolean("performance.debug-mode", false);
    }

    public boolean isVisualizeZonesEnabled() {
        return config.getBoolean("visualize-zones", true);
    }

    public void logDebug(String message) {
        if (isDebugMode()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
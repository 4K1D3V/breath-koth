package pro.akii.ks;

import org.bukkit.plugin.java.JavaPlugin;
import pro.akii.ks.config.ConfigManager;
import pro.akii.ks.gui.ZoneManagementGUI;
import pro.akii.ks.koth.DragonBreathTask;
import pro.akii.ks.koth.KothListener;
import pro.akii.ks.koth.PlayerStats;
import pro.akii.ks.koth.ZoneVisualizer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class BreathKOTH extends JavaPlugin {

    private ConfigManager configManager;
    private DragonBreathTask dragonBreathTask;
    private ZoneVisualizer zoneVisualizer;
    private PlayerStats playerStats;
    private final Map<UUID, ZoneManagementGUI> pendingZoneInteractions = new HashMap<>();
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        if (!loadTask()) {
            getLogger().severe("Failed to initialize BreathKOTH! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        BreathKothCommand command = new BreathKothCommand(this);
        getCommand("breathkoth").setExecutor(command);
        getCommand("breathkoth").setTabCompleter(command);
        KothListener listener = new KothListener(dragonBreathTask, configManager, this);
        getServer().getPluginManager().registerEvents(listener, this);

        zoneVisualizer = new ZoneVisualizer(this, dragonBreathTask);
        zoneVisualizer.runTaskTimer(this, 0L, 100L);

        playerStats = new PlayerStats(this);
        playerStats.loadStats();

        getLogger().info("BreathKOTH enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (dragonBreathTask != null) {
            configManager.saveZones(dragonBreathTask.getActiveZones());
            dragonBreathTask.cancel();
        }
        if (zoneVisualizer != null) {
            zoneVisualizer.cancel();
        }
        if (playerStats != null) {
            playerStats.saveStats();
        }
        pendingZoneInteractions.clear();
        teleportCooldowns.clear();
        getLogger().info("BreathKOTH disabled.");
    }

    public boolean reloadConfigAndTask() {
        reloadConfig();
        configManager = new ConfigManager(this);
        if (dragonBreathTask != null) {
            dragonBreathTask.cancel();
        }
        if (zoneVisualizer != null) {
            zoneVisualizer.cancel();
        }
        if (playerStats != null) {
            playerStats.saveStats();
        }
        boolean success = loadTask();
        if (success) {
            zoneVisualizer = new ZoneVisualizer(this, dragonBreathTask);
            zoneVisualizer.runTaskTimer(this, 0L, 100L);
            playerStats = new PlayerStats(this);
            playerStats.loadStats();
        }
        return success;
    }

    private boolean loadTask() {
        try {
            dragonBreathTask = new DragonBreathTask(this, configManager);
            dragonBreathTask.runTaskTimer(this, 0L, 10L);
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load KOTH zones or start task", e);
            return false;
        }
    }

    public DragonBreathTask getDragonBreathTask() {
        return dragonBreathTask;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerStats getPlayerStats() {
        return playerStats;
    }

    public void addPendingZoneInteraction(UUID playerId, ZoneManagementGUI gui) {
        pendingZoneInteractions.put(playerId, gui);
    }

    public ZoneManagementGUI removePendingZoneInteraction(UUID playerId) {
        return pendingZoneInteractions.remove(playerId);
    }

    public boolean hasPendingZoneInteraction(UUID playerId) {
        return pendingZoneInteractions.containsKey(playerId);
    }

    public boolean canTeleport(UUID playerId) {
        Long lastTeleport = teleportCooldowns.get(playerId);
        int cooldown = configManager.getConfig().getInt("teleport-cooldown", 30) * 1000;
        if (lastTeleport == null || System.currentTimeMillis() - lastTeleport >= cooldown) {
            teleportCooldowns.put(playerId, System.currentTimeMillis());
            return true;
        }
        return false;
    }
}
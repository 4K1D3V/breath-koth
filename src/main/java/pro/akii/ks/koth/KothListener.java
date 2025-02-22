package pro.akii.ks.koth;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import pro.akii.ks.BreathKOTH;
import pro.akii.ks.config.ConfigManager;
import pro.akii.ks.gui.ZoneManagementGUI;

public class KothListener implements Listener {

    private final DragonBreathTask task;
    private final ConfigManager configManager;
    private final BreathKOTH plugin;
    private final ZoneManagementGUI gui;

    public KothListener(DragonBreathTask task, ConfigManager configManager, BreathKOTH plugin) {
        this.task = task;
        this.configManager = configManager;
        this.plugin = plugin;
        this.gui = new ZoneManagementGUI(plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        for (KothZone zone : task.getActiveZones()) {
            if (zone.isInZone(player) && zone.canAffect(player)) {
                zone.incrementPresence(10);
                plugin.getPlayerStats().incrementTime(player.getUniqueId(), zone.getName(), 10);
                if (zone.getPresenceTicks() >= configManager.getPlayerActivationTime()) {
                    task.startFlameBursts();
                    zone.resetPresence();
                }
            } else {
                zone.resetPresence();
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        gui.handleClick(event);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.hasPendingZoneInteraction(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();
        ZoneManagementGUI pendingGui = plugin.removePendingZoneInteraction(player.getUniqueId());
        if (pendingGui != null) {
            Bukkit.getScheduler().runTask(plugin, () -> pendingGui.handleZoneInteraction(player, message));
        }
    }
}
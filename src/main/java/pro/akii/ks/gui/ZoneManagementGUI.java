package pro.akii.ks.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pro.akii.ks.BreathKOTH;
import pro.akii.ks.koth.DragonBreathTask;
import pro.akii.ks.koth.KothZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class ZoneManagementGUI {

    private final BreathKOTH plugin;
    private final Inventory inventory;
    private static final List<String> PARTICLE_TYPES = Arrays.asList("DRAGON_BREATH", "FLAME", "SMOKE", "LAVA");
    private static final List<String> EFFECT_TYPES = Arrays.asList("HARM", "POISON", "SLOWNESS", "WEAKNESS", "REGENERATION", "SPEED", "FIRE_RESISTANCE", "INVISIBILITY", "BLINDNESS", "CONFUSION");
    private String editingZoneName = null;
    private int editingStep = 0; // 0: Main, 1: Edit coords, 2: Edit radius, 3: Edit interval, 4: Edit permission, 5: Select effect, 6: Edit effect, 7: Confirm removal, 8: Effect duration, 9: Effect amplifier, 10: Leaderboard main, 11: Leaderboard zone submenu
    private int editingEffectIndex = -1;
    private String pendingRemovalZone = null;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 54;
    private String selectedEffectType = null;
    private int selectedEffectDuration = 20;
    private int selectedEffectAmplifier = 0;
    private int leaderboardPage = 0;
    private String leaderboardZoneFilter = null;
    private String leaderboardSortMetric = "time-spent"; // Default sort by time

    public ZoneManagementGUI(BreathKOTH plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "BreathKOTH Zone Manager");
        populateMainInventory();
    }

    private void populateMainInventory() {
        inventory.clear();
        DragonBreathTask task = plugin.getDragonBreathTask();
        List<KothZone> zones = task.getActiveZones();
        int totalPages = (int) Math.ceil((double) zones.size() / ITEMS_PER_PAGE);

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE - 6, zones.size());

        for (int i = startIndex; i < endIndex; i++) {
            KothZone zone = zones.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + zone.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Location: " + zone.getCenter().getBlockX() + ", " + zone.getCenter().getBlockY() + ", " + zone.getCenter().getBlockZ());
            lore.add(ChatColor.GRAY + "Radius: " + zone.getRadius());
            lore.add(ChatColor.GRAY + "Interval: " + (zone.getInterval() / 20) + "s");
            lore.add(ChatColor.GRAY + "Permission: " + (zone.getPermission() != null ? zone.getPermission() : "None"));
            lore.add(ChatColor.GRAY + "Visualized: " + (zone.isVisualized() ? "Yes" : "No"));
            lore.add(ChatColor.GREEN + "Left-click to edit");
            lore.add(ChatColor.RED + "Right-click to remove");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i - startIndex, item);
        }

        if (currentPage > 0) {
            inventory.setItem(48, createItem(Material.ARROW, ChatColor.GREEN + "Prev", "Previous page"));
        }
        inventory.setItem(49, createItem(Material.BOOK, ChatColor.WHITE + "Page " + (currentPage + 1) + " of " + totalPages, "Current page"));
        if (currentPage < totalPages - 1) {
            inventory.setItem(50, createItem(Material.ARROW, ChatColor.GREEN + "Next", "Next page"));
        }

        inventory.setItem(51, createItem(Material.PLAYER_HEAD, ChatColor.YELLOW + "Leaderboard", "View top players"));
        inventory.setItem(52, createItem(Material.EMERALD, ChatColor.GREEN + "Add New Zone", "Add a new zone"));
        inventory.setItem(53, createItem(Material.BLAZE_POWDER, ChatColor.RED + "Trigger Flames", "Trigger flames now"));
    }

    private void populateEditInventory(String zoneName) {
        inventory.clear();
        KothZone zone = plugin.getDragonBreathTask().getActiveZones().stream()
                .filter(z -> z.getName().equals(zoneName)).findFirst().orElse(null);
        if (zone == null) return;

        inventory.setItem(10, createItem(Material.COMPASS, ChatColor.YELLOW + "Edit Coordinates", "Click to set new coordinates"));
        inventory.setItem(11, createItem(Material.SPYGLASS, ChatColor.YELLOW + "Edit Radius", "Click to set new radius"));
        inventory.setItem(12, createItem(Material.CLOCK, ChatColor.YELLOW + "Edit Interval", "Click to set new interval (seconds)"));
        inventory.setItem(13, createItem(Material.BARRIER, ChatColor.YELLOW + "Edit Permission", "Click to set new permission (or none)"));
        inventory.setItem(14, createItem(Material.FIREWORK_STAR, ChatColor.YELLOW + "Edit Particle Type",
                "Current: " + plugin.getConfigManager().getParticleType(zoneName),
                "Left-click to cycle forward", "Right-click to cycle backward"));
        inventory.setItem(15, createItem(Material.POTION, ChatColor.YELLOW + "Edit Cloud Effects", "Click to configure effects"));
        inventory.setItem(16, createItem(Material.ENDER_PEARL, ChatColor.YELLOW + "Teleport to Zone", "Click to teleport to center"));
        inventory.setItem(17, createItem(Material.BEACON, ChatColor.YELLOW + "Toggle Visualization",
                "Current: " + (zone.isVisualized() ? "Enabled" : "Disabled"),
                "Click to toggle zone boundary particles"));
        inventory.setItem(31, createItem(Material.ARROW, ChatColor.GREEN + "Back to Main Menu", "Return to zone list"));
    }

    private void populateEffectInventory(String zoneName) {
        inventory.clear();
        List<Map<String, Object>> currentEffects = plugin.getConfigManager().getCloudEffects(zoneName);
        for (int i = 0; i < Math.min(currentEffects.size(), 18); i++) {
            Map<String, Object> effect = currentEffects.get(i);
            inventory.setItem(i, createItem(Material.POTION, ChatColor.YELLOW + (String) effect.get("type"),
                    "Duration: " + ((int) effect.get("duration") / 20) + "s", "Amplifier: " + effect.get("amplifier"),
                    ChatColor.RED + "Left-click to remove", ChatColor.GREEN + "Shift-click to edit"));
        }
        inventory.setItem(27, createItem(Material.EMERALD, ChatColor.GREEN + "Add Effect", "Click to add a new effect"));
        inventory.setItem(31, createItem(Material.ARROW, ChatColor.GREEN + "Back to Edit Menu", "Return to zone edit"));
    }

    private void populateEffectTypeSelection(String zoneName) {
        inventory.clear();
        for (int i = 0; i < Math.min(EFFECT_TYPES.size(), 18); i++) {
            String effectType = EFFECT_TYPES.get(i);
            inventory.setItem(i, createItem(Material.POTION, ChatColor.YELLOW + effectType, "Click to select this effect"));
        }
        inventory.setItem(31, createItem(Material.ARROW, ChatColor.GREEN + "Back to Effects Menu", "Return to effect configuration"));
    }

    private void populateEffectDurationSelection(String zoneName) {
        inventory.clear();
        ItemStack slider = createItem(Material.CLOCK, ChatColor.YELLOW + "Duration: " + selectedEffectDuration + "s", "Adjust duration");
        ItemMeta meta = slider.getItemMeta();
        meta.setLore(createSliderLore(selectedEffectDuration, 1, 600));
        slider.setItemMeta(meta);
        inventory.setItem(13, slider);

        inventory.setItem(11, createItem(Material.REDSTONE_TORCH, ChatColor.RED + "-10s", "Decrease by 10 seconds"));
        inventory.setItem(12, createItem(Material.REDSTONE_TORCH, ChatColor.RED + "-1s", "Decrease by 1 second"));
        inventory.setItem(14, createItem(Material.TORCH, ChatColor.GREEN + "+1s", "Increase by 1 second"));
        inventory.setItem(15, createItem(Material.TORCH, ChatColor.GREEN + "+10s", "Increase by 10 seconds"));

        inventory.setItem(22, createItem(Material.ARROW, ChatColor.GREEN + "Next: Set Amplifier", "Click to proceed"));
        inventory.setItem(31, createItem(Material.ARROW, ChatColor.GREEN + "Back to Type Selection", "Return to effect type selection"));
    }

    private void populateEffectAmplifierSelection(String zoneName) {
        inventory.clear();
        ItemStack slider = createItem(Material.REDSTONE, ChatColor.YELLOW + "Amplifier: " + selectedEffectAmplifier, "Adjust amplifier");
        ItemMeta meta = slider.getItemMeta();
        meta.setLore(createSliderLore(selectedEffectAmplifier, 0, 10));
        slider.setItemMeta(meta);
        inventory.setItem(13, slider);

        inventory.setItem(11, createItem(Material.REDSTONE_TORCH, ChatColor.RED + "-5", "Decrease by 5"));
        inventory.setItem(12, createItem(Material.REDSTONE_TORCH, ChatColor.RED + "-1", "Decrease by 1"));
        inventory.setItem(14, createItem(Material.TORCH, ChatColor.GREEN + "+1", "Increase by 1"));
        inventory.setItem(15, createItem(Material.TORCH, ChatColor.GREEN + "+5", "Increase by 5"));

        inventory.setItem(22, createItem(Material.EMERALD, ChatColor.GREEN + "Confirm Effect", "Click to save effect"));
        inventory.setItem(31, createItem(Material.ARROW, ChatColor.GREEN + "Back to Duration", "Return to duration selection"));
    }

    private void populateLeaderboardMain() {
        inventory.clear();
        List<KothZone> zones = plugin.getDragonBreathTask().getActiveZones();

        for (int i = 0; i < Math.min(zones.size(), 45); i++) {
            KothZone zone = zones.get(i);
            ItemStack item = createItem(Material.PAPER, ChatColor.YELLOW + zone.getName(),
                    ChatColor.GRAY + "Click to view leaderboard for this zone");
            inventory.setItem(i, item);
        }

        inventory.setItem(47, createItem(Material.COMPASS, ChatColor.YELLOW + "Sort: " + (leaderboardSortMetric.equals("time-spent") ? "Time" : "Damage"),
                "Click to toggle sort metric (Time/Damage)"));
        inventory.setItem(49, createItem(Material.BOOK, ChatColor.WHITE + "All Zones", "View combined leaderboard"));
        inventory.setItem(31, createItem(Material.ARROW, ChatColor.GREEN + "Back to Main Menu", "Return to zone list"));
    }

    private void populateLeaderboardZone(String zoneFilter) {
        inventory.clear();
        Map<UUID, Map<String, Map<String, Long>>> stats = plugin.getPlayerStats().getStats();
        List<Map.Entry<UUID, Long>> leaderboard = new ArrayList<>();

        // Aggregate stats based on filter and sort metric
        for (Map.Entry<UUID, Map<String, Map<String, Long>>> entry : stats.entrySet()) {
            long value = 0;
            if (zoneFilter == null) {
                value = entry.getValue().values().stream()
                        .mapToLong(zone -> zone.getOrDefault(leaderboardSortMetric, 0L))
                        .sum();
            } else if (entry.getValue().containsKey(zoneFilter)) {
                value = entry.getValue().get(zoneFilter).getOrDefault(leaderboardSortMetric, 0L);
            }
            if (value > 0) {
                leaderboard.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), value));
            }
        }

        // Sort based on metric
        leaderboard.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());

        // Pagination
        int itemsPerPage = 36; // Reduced to fit breakdown
        int totalPages = (int) Math.ceil((double) leaderboard.size() / itemsPerPage);
        int startIndex = leaderboardPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, leaderboard.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Long> entry = leaderboard.get(i);
            UUID uuid = entry.getKey();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            if (playerName == null) playerName = uuid.toString();
            Map<String, Map<String, Long>> playerStats = stats.get(uuid);

            List<String> lore = new ArrayList<>();
            if (zoneFilter == null) {
                long totalTime = playerStats.values().stream()
                        .mapToLong(zone -> zone.getOrDefault("time-spent", 0L))
                        .sum();
                long totalDamage = playerStats.values().stream()
                        .mapToLong(zone -> zone.getOrDefault("damage-taken", 0L))
                        .sum();
                lore.add(ChatColor.GRAY + "Total Time: " + formatTime(totalTime));
                lore.add(ChatColor.GRAY + "Total Damage Taken: " + totalDamage + " HP");
                lore.addAll(createStatBreakdown(totalTime, totalDamage));
                for (Map.Entry<String, Map<String, Long>> zoneEntry : playerStats.entrySet()) {
                    String zoneName = zoneEntry.getKey();
                    long timeSpent = zoneEntry.getValue().getOrDefault("time-spent", 0L);
                    long damageTaken = zoneEntry.getValue().getOrDefault("damage-taken", 0L);
                    lore.add(ChatColor.GRAY + "- " + zoneName + ": " + formatTime(timeSpent) + ", " + damageTaken + " HP");
                }
            } else {
                long timeSpent = playerStats.getOrDefault(zoneFilter, new HashMap<>()).getOrDefault("time-spent", 0L);
                long damageTaken = playerStats.getOrDefault(zoneFilter, new HashMap<>()).getOrDefault("damage-taken", 0L);
                lore.add(ChatColor.GRAY + "Time in " + zoneFilter + ": " + formatTime(timeSpent));
                lore.add(ChatColor.GRAY + "Damage Taken: " + damageTaken + " HP");
                lore.addAll(createStatBreakdown(timeSpent, damageTaken));
            }

            ItemStack item = createItem(Material.PLAYER_HEAD,
                    ChatColor.YELLOW + "#" + (i + 1) + ": " + playerName,
                    lore.toArray(new String[0]));
            inventory.setItem(i - startIndex, item);
        }

        // Pagination and controls
        if (leaderboardPage > 0) {
            inventory.setItem(45, createItem(Material.ARROW, ChatColor.GREEN + "Prev Page", "Previous leaderboard page"));
        }
        inventory.setItem(49, createItem(Material.BOOK, ChatColor.WHITE + "Page " + (leaderboardPage + 1) + " of " + totalPages,
                "Leaderboard " + (zoneFilter == null ? "All Zones" : zoneFilter)));
        if (leaderboardPage < totalPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, ChatColor.GREEN + "Next Page", "Next leaderboard page"));
        }

        inventory.setItem(47, createItem(Material.COMPASS, ChatColor.YELLOW + "Sort: " + (leaderboardSortMetric.equals("time-spent") ? "Time" : "Damage"),
                "Click to toggle sort metric (Time/Damage)"));
        inventory.setItem(31, createItem(Material.ARROW, ChatColor.GREEN + "Back to Leaderboard Menu", "Return to leaderboard selection"));
    }

    private List<String> createStatBreakdown(long timeSpent, long damageTaken) { // Added: Text-based "pie chart"
        List<String> breakdown = new ArrayList<>();
        long total = timeSpent + damageTaken; // Simplified metric sum for visualization
        if (total == 0) {
            breakdown.add(ChatColor.GRAY + "Breakdown: No stats");
            return breakdown;
        }

        int timePercent = (int) ((timeSpent * 100) / total);
        int damagePercent = 100 - timePercent;
        int barLength = 20;
        int timeBars = (int) ((timePercent * barLength) / 100.0);
        StringBuilder bar = new StringBuilder(ChatColor.GRAY + "[");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < timeBars ? ChatColor.GREEN + "█" : ChatColor.RED + "█");
        }
        bar.append(ChatColor.GRAY + "]");
        breakdown.add(ChatColor.GRAY + "Time: " + timePercent + "% | Damage: " + damagePercent + "%");
        breakdown.add(bar.toString());
        return breakdown;
    }

    private String formatTime(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    private List<String> createSliderLore(int value, int min, int max) {
        int barLength = 20;
        int filled = (int) ((double) (value - min) / (max - min) * barLength);
        StringBuilder bar = new StringBuilder(ChatColor.GRAY + "[");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? ChatColor.GREEN + "█" : ChatColor.RED + "█");
        }
        bar.append(ChatColor.GRAY + "]");
        return Arrays.asList(bar.toString(), ChatColor.GRAY + "Min: " + min + " | Max: " + max);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        DragonBreathTask task = plugin.getDragonBreathTask();
        String displayName = clicked.getItemMeta().getDisplayName();

        if (editingZoneName == null) {
            // Main menu handling
            if (displayName.equals(ChatColor.GREEN + "Add New Zone")) {
                player.closeInventory();
                player.sendMessage("§eEnter zone details in chat: <name> <x> <y> <z> <radius> <interval> [permission]");
                plugin.addPendingZoneInteraction(player.getUniqueId(), this);
                editingStep = 0;
            } else if (displayName.equals(ChatColor.RED + "Trigger Flames")) {
                task.startFlameBursts();
                player.sendMessage("§aDragon flames triggered!");
            } else if (displayName.equals(ChatColor.GREEN + "Prev")) {
                if (currentPage > 0) {
                    currentPage--;
                    populateMainInventory();
                }
            } else if (displayName.equals(ChatColor.GREEN + "Next")) {
                int totalPages = (int) Math.ceil((double) task.getActiveZones().size() / ITEMS_PER_PAGE);
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    populateMainInventory();
                }
            } else if (displayName.equals(ChatColor.YELLOW + "Leaderboard")) {
                editingStep = 10;
                leaderboardPage = 0;
                leaderboardZoneFilter = null;
                leaderboardSortMetric = "time-spent";
                populateLeaderboardMain();
            } else {
                String zoneName = ChatColor.stripColor(displayName);
                if (task.getActiveZones().stream().anyMatch(z -> z.getName().equals(zoneName))) {
                    if (event.getClick() == ClickType.LEFT) {
                        editingZoneName = zoneName;
                        editingStep = 0;
                        populateEditInventory(zoneName);
                    } else if (event.getClick() == ClickType.RIGHT) {
                        pendingRemovalZone = zoneName;
                        player.closeInventory();
                        player.sendMessage("§eType 'confirm' to remove zone " + zoneName + " or anything else to cancel.");
                        plugin.addPendingZoneInteraction(player.getUniqueId(), this);
                        editingStep = 7;
                    }
                }
            }
        } else {
            // Edit menu or submenus handling
            if (displayName.equals(ChatColor.GREEN + "Back to Main Menu")) {
                editingZoneName = null;
                editingStep = 0;
                leaderboardPage = 0;
                leaderboardZoneFilter = null;
                leaderboardSortMetric = "time-spent";
                populateMainInventory();
            } else if (editingStep == 10) { // Leaderboard main menu
                if (displayName.equals(ChatColor.YELLOW + "Sort: Time") || displayName.equals(ChatColor.YELLOW + "Sort: Damage")) {
                    leaderboardSortMetric = leaderboardSortMetric.equals("time-spent") ? "damage-taken" : "time-spent";
                    populateLeaderboardMain();
                } else if (displayName.equals(ChatColor.WHITE + "All Zones")) {
                    editingStep = 11;
                    leaderboardPage = 0;
                    leaderboardZoneFilter = null;
                    populateLeaderboardZone(null);
                } else {
                    String zoneName = ChatColor.stripColor(displayName);
                    if (task.getActiveZones().stream().anyMatch(z -> z.getName().equals(zoneName))) {
                        editingStep = 11;
                        leaderboardPage = 0;
                        leaderboardZoneFilter = zoneName;
                        populateLeaderboardZone(zoneName);
                    }
                }
            } else if (editingStep == 11) { // Leaderboard zone submenu
                if (displayName.equals(ChatColor.GREEN + "Prev Page")) {
                    if (leaderboardPage > 0) {
                        leaderboardPage--;
                        populateLeaderboardZone(leaderboardZoneFilter);
                    }
                } else if (displayName.equals(ChatColor.GREEN + "Next Page")) {
                    Map<UUID, Map<String, Map<String, Long>>> stats = plugin.getPlayerStats().getStats();
                    int itemsPerPage = 36;
                    int totalEntries = leaderboardZoneFilter == null ? stats.size() :
                            (int) stats.values().stream().filter(zoneStats -> zoneStats.containsKey(leaderboardZoneFilter)).count();
                    int totalPages = (int) Math.ceil((double) totalEntries / itemsPerPage);
                    if (leaderboardPage < totalPages - 1) {
                        leaderboardPage++;
                        populateLeaderboardZone(leaderboardZoneFilter);
                    }
                } else if (displayName.equals(ChatColor.YELLOW + "Sort: Time") || displayName.equals(ChatColor.YELLOW + "Sort: Damage")) {
                    leaderboardSortMetric = leaderboardSortMetric.equals("time-spent") ? "damage-taken" : "time-spent";
                    leaderboardPage = 0; // Reset page on sort change
                    populateLeaderboardZone(leaderboardZoneFilter);
                } else if (displayName.equals(ChatColor.GREEN + "Back to Leaderboard Menu")) {
                    editingStep = 10;
                    leaderboardPage = 0;
                    populateLeaderboardMain();
                }
            } else if (displayName.startsWith(ChatColor.YELLOW + "Edit Particle Type")) {
                String currentType = plugin.getConfigManager().getParticleType(editingZoneName);
                int index = PARTICLE_TYPES.indexOf(currentType);
                if (event.getClick() == ClickType.LEFT) {
                    index = (index + 1) % PARTICLE_TYPES.size();
                } else if (event.getClick() == ClickType.RIGHT) {
                    index = (index - 1 + PARTICLE_TYPES.size()) % PARTICLE_TYPES.size();
                }
                String newType = PARTICLE_TYPES.get(index);
                ConfigurationSection section = config.getConfigurationSection("custom-effects." + editingZoneName);
                if (section == null) {
                    section = config.createSection("custom-effects." + editingZoneName);
                }
                section.set("particle-type", newType);
                try {
                    config.save(new File(plugin.getDataFolder(), "config.yml"));
                    player.sendMessage("§aUpdated particle type for " + editingZoneName + " to " + newType);
                } catch (IOException e) {
                    player.sendMessage("§cFailed to save particle type.");
                    plugin.getLogger().log(Level.SEVERE, "Failed to save config.yml", e);
                }
                populateEditInventory(editingZoneName);
            } else if (displayName.equals(ChatColor.YELLOW + "Edit Coordinates")) {
                player.closeInventory();
                player.sendMessage("§eEnter new coordinates in chat: <x> <y> <z>");
                plugin.addPendingZoneInteraction(player.getUniqueId(), this);
                editingStep = 1;
            } else if (displayName.equals(ChatColor.YELLOW + "Edit Radius")) {
                player.closeInventory();
                player.sendMessage("§eEnter new radius in chat: <radius>");
                plugin.addPendingZoneInteraction(player.getUniqueId(), this);
                editingStep = 2;
            } else if (displayName.equals(ChatColor.YELLOW + "Edit Interval")) {
                player.closeInventory();
                player.sendMessage("§eEnter new interval in chat: <interval> (seconds)");
                plugin.addPendingZoneInteraction(player.getUniqueId(), this);
                editingStep = 3;
            } else if (displayName.equals(ChatColor.YELLOW + "Edit Permission")) {
                player.closeInventory();
                player.sendMessage("§eEnter new permission in chat: <permission> (or 'none' to clear)");
                plugin.addPendingZoneInteraction(player.getUniqueId(), this);
                editingStep = 4;
            } else if (displayName.equals(ChatColor.YELLOW + "Edit Cloud Effects")) {
                populateEffectInventory(editingZoneName);
            } else if (displayName.equals(ChatColor.YELLOW + "Teleport to Zone")) {
                if (plugin.canTeleport(player.getUniqueId())) {
                    KothZone zone = task.getActiveZones().stream()
                            .filter(z -> z.getName().equals(editingZoneName)).findFirst().orElse(null);
                    if (zone != null) {
                        Location loc = findSafeTeleportLocation(zone.getCenter());
                        player.getWorld().loadChunk(loc.getChunk());
                        player.teleport(loc);
                        player.sendMessage("§aTeleported to zone: " + editingZoneName);
                    } else {
                        player.sendMessage("§cZone not found: " + editingZoneName);
                    }
                } else {
                    int cooldown = config.getInt("teleport-cooldown", 30);
                    player.sendMessage("§cTeleport is on cooldown. Please wait " + cooldown + " seconds.");
                }
            } else if (displayName.equals(ChatColor.YELLOW + "Toggle Visualization")) {
                KothZone zone = task.getActiveZones().stream()
                        .filter(z -> z.getName().equals(editingZoneName)).findFirst().orElse(null);
                if (zone != null) {
                    zone.setVisualize(!zone.isVisualized());
                    plugin.getConfigManager().saveZones(task.getActiveZones());
                    player.sendMessage("§aZone visualization " + (zone.isVisualized() ? "enabled" : "disabled") + " for " + editingZoneName);
                    populateEditInventory(editingZoneName);
                }
            } else if (displayName.equals(ChatColor.GREEN + "Add Effect")) {
                populateEffectTypeSelection(editingZoneName);
            } else if (displayName.equals(ChatColor.GREEN + "Back to Edit Menu")) {
                populateEditInventory(editingZoneName);
            } else if (displayName.equals(ChatColor.GREEN + "Back to Effects Menu")) {
                populateEffectInventory(editingZoneName);
            } else if (displayName.equals(ChatColor.GREEN + "Back to Type Selection")) {
                populateEffectTypeSelection(editingZoneName);
            } else if (displayName.equals(ChatColor.GREEN + "Next: Set Amplifier")) {
                populateEffectAmplifierSelection(editingZoneName);
            } else if (displayName.equals(ChatColor.GREEN + "Back to Duration")) {
                populateEffectDurationSelection(editingZoneName);
            } else if (displayName.equals(ChatColor.GREEN + "Confirm Effect")) {
                Map<String, Object> effect = new HashMap<>();
                effect.put("type", selectedEffectType);
                effect.put("duration", selectedEffectDuration * 20);
                effect.put("amplifier", selectedEffectAmplifier);
                List<Map<String, Object>> effects = plugin.getConfigManager().getCloudEffects(editingZoneName);
                if (editingEffectIndex >= 0) {
                    effects.set(editingEffectIndex, effect);
                    player.sendMessage("§aUpdated effect " + selectedEffectType + " for " + editingZoneName);
                    editingEffectIndex = -1;
                } else {
                    effects.add(effect);
                    player.sendMessage("§aAdded effect " + selectedEffectType + " to " + editingZoneName);
                }
                plugin.getConfigManager().saveCustomEffects(editingZoneName, effects);
                selectedEffectType = null;
                selectedEffectDuration = 20;
                selectedEffectAmplifier = 0;
                populateEffectInventory(editingZoneName);
            } else if (displayName.startsWith(ChatColor.YELLOW + "Duration")) {
                if (event.getClick() == ClickType.LEFT) {
                    selectedEffectDuration += 1;
                } else if (event.getClick() == ClickType.RIGHT) {
                    selectedEffectDuration = Math.max(1, selectedEffectDuration - 1);
                } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                    selectedEffectDuration += 10;
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    selectedEffectDuration = Math.max(1, selectedEffectDuration - 10);
                }
                selectedEffectDuration = Math.min(selectedEffectDuration, 600);
                populateEffectDurationSelection(editingZoneName);
            } else if (displayName.startsWith(ChatColor.YELLOW + "Amplifier")) {
                if (event.getClick() == ClickType.LEFT) {
                    selectedEffectAmplifier += 1;
                } else if (event.getClick() == ClickType.RIGHT) {
                    selectedEffectAmplifier = Math.max(0, selectedEffectAmplifier - 1);
                } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                    selectedEffectAmplifier += 5;
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    selectedEffectAmplifier = Math.max(0, selectedEffectAmplifier - 5);
                }
                selectedEffectAmplifier = Math.min(selectedEffectAmplifier, 10);
                populateEffectAmplifierSelection(editingZoneName);
            } else if (displayName.startsWith(ChatColor.YELLOW)) { // Effect or Effect Type handling
                if (inventory.getItem(27) != null && inventory.getItem(27).getType() == Material.EMERALD) {
                    List<Map<String, Object>> effects = plugin.getConfigManager().getCloudEffects(editingZoneName);
                    int slot = event.getSlot();
                    if (slot < effects.size()) {
                        if (event.isShiftClick()) {
                            editingEffectIndex = slot;
                            selectedEffectType = (String) effects.get(slot).get("type");
                            selectedEffectDuration = (int) effects.get(slot).get("duration") / 20;
                            selectedEffectAmplifier = (int) effects.get(slot).get("amplifier");
                            populateEffectDurationSelection(editingZoneName);
                        } else {
                            pendingRemovalZone = editingZoneName;
                            editingEffectIndex = slot;
                            player.closeInventory();
                            player.sendMessage("§eType 'confirm' to remove effect " + effects.get(slot).get("type") + " from " + editingZoneName + " or anything else to cancel.");
                            plugin.addPendingZoneInteraction(player.getUniqueId(), this);
                            editingStep = 7;
                        }
                    }
                } else {
                    String effectType = ChatColor.stripColor(displayName);
                    if (EFFECT_TYPES.contains(effectType)) {
                        selectedEffectType = effectType;
                        selectedEffectDuration = 20;
                        selectedEffectAmplifier = 0;
                        populateEffectDurationSelection(editingZoneName);
                    }
                }
            }
        }
    }
    
    public void handleZoneInteraction(Player player, String input) {
        if (editingZoneName == null && editingStep == 0) {
            // Adding a new zone
            String[] parts = input.trim().split("\\s+");
            if (parts.length < 6 || parts.length > 7) {
                player.sendMessage("§cInvalid format. Use: <name> <x> <y> <z> <radius> <interval> [permission]");
                return;
            }

            try {
                String name = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                int radius = Integer.parseInt(parts[4]);
                int interval = Integer.parseInt(parts[5]);
                String permission = parts.length == 7 ? parts[6] : null;

                KothZone zone = new KothZone(new Location(player.getWorld(), x, y, z), radius, name, interval * 20, permission);
                plugin.getDragonBreathTask().addZone(zone);
                player.sendMessage("§aAdded zone: " + name);
                populateMainInventory();
                player.openInventory(inventory);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number format in input.");
            }
        } else {
            // Editing an existing zone
            KothZone zone = plugin.getDragonBreathTask().getActiveZones().stream()
                    .filter(z -> z.getName().equals(editingZoneName)).findFirst().orElse(null);
            if (zone == null) {
                player.sendMessage("§cZone not found: " + editingZoneName);
                editingZoneName = null;
                populateMainInventory();
                return;
            }

            switch (editingStep) {
                case 1: // Edit Coordinates
                    String[] coords = input.trim().split("\\s+");
                    if (coords.length != 3) {
                        player.sendMessage("§cInvalid format. Use: <x> <y> <z>");
                        return;
                    }
                    try {
                        double x = Double.parseDouble(coords[0]);
                        double y = Double.parseDouble(coords[1]);
                        double z = Double.parseDouble(coords[2]);
                        zone.updateZone(new Location(zone.getCenter().getWorld(), x, y, z), zone.getRadius());
                        player.sendMessage("§aUpdated coordinates for " + editingZoneName);
                        plugin.getConfigManager().saveZones(plugin.getDragonBreathTask().getActiveZones());
                        populateEditInventory(editingZoneName);
                        player.openInventory(inventory);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid number format.");
                    }
                    break;
                case 2: // Edit Radius
                    try {
                        int radius = Integer.parseInt(input.trim());
                        zone.updateZone(zone.getCenter(), radius);
                        player.sendMessage("§aUpdated radius for " + editingZoneName);
                        plugin.getConfigManager().saveZones(plugin.getDragonBreathTask().getActiveZones());
                        populateEditInventory(editingZoneName);
                        player.openInventory(inventory);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid radius.");
                    }
                    break;
                case 3: // Edit Interval
                    try {
                        int interval = Integer.parseInt(input.trim()) * 20;
                        zone.setInterval(interval);
                        player.sendMessage("§aUpdated interval for " + editingZoneName);
                        plugin.getConfigManager().saveZones(plugin.getDragonBreathTask().getActiveZones());
                        populateEditInventory(editingZoneName);
                        player.openInventory(inventory);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid interval.");
                    }
                    break;
                case 4: // Edit Permission
                    String permission = input.trim().equalsIgnoreCase("none") ? null : input.trim();
                    zone.setPermission(permission);
                    player.sendMessage("§aUpdated permission for " + editingZoneName);
                    plugin.getConfigManager().saveZones(plugin.getDragonBreathTask().getActiveZones());
                    populateEditInventory(editingZoneName);
                    player.openInventory(inventory);
                    break;
                case 7: // Confirm Removal
                    if (input.trim().equalsIgnoreCase("confirm")) {
                        if (pendingRemovalZone != null && editingEffectIndex == -1) {
                            // Zone removal
                            if (task.removeZone(pendingRemovalZone)) {
                                player.sendMessage("§aRemoved zone: " + pendingRemovalZone);
                                editingZoneName = null;
                            } else {
                                player.sendMessage("§cFailed to remove zone: " + pendingRemovalZone);
                            }
                            pendingRemovalZone = null;
                            populateMainInventory();
                        } else if (pendingRemovalZone != null && editingEffectIndex >= 0) {
                            // Effect removal
                            List<Map<String, Object>> effects = plugin.getConfigManager().getCloudEffects(editingZoneName);
                            if (editingEffectIndex < effects.size()) {
                                String effectType = (String) effects.get(editingEffectIndex).get("type");
                                effects.remove(editingEffectIndex);
                                plugin.getConfigManager().saveCustomEffects(editingZoneName, effects);
                                player.sendMessage("§aRemoved effect " + effectType + " from " + editingZoneName);
                            }
                            pendingRemovalZone = null;
                            editingEffectIndex = -1;
                            populateEffectInventory(editingZoneName);
                        }
                    } else {
                        player.sendMessage("§cRemoval cancelled.");
                        pendingRemovalZone = null;
                        if (editingEffectIndex >= 0) {
                            populateEffectInventory(editingZoneName);
                        } else {
                            populateMainInventory();
                            editingZoneName = null;
                        }
                    }
                    player.openInventory(inventory);
                    break;
            }
        }
    }

    private Location findSafeTeleportLocation(Location center) {
        World world = center.getWorld();
        int x = center.getBlockX();
        int z = center.getBlockZ();
        int y = center.getBlockY();

        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -5; dy <= 5; dy++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    Block above = world.getBlockAt(x + dx, y + dy + 1, z + dz);
                    Block below = world.getBlockAt(x + dx, y + dy - 1, z + dz);

                    if (!block.getType().isSolid() && !above.getType().isSolid() && below.getType().isSolid() &&
                            block.getType() != Material.LAVA && block.getType() != Material.WATER) {
                        return new Location(world, x + dx + 0.5, y + dy, z + dz + 0.5, center.getYaw(), center.getPitch());
                    }
                }
            }
        }

        return center;
    }
}
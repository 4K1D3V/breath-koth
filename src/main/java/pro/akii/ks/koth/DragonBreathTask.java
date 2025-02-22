package pro.akii.ks.koth;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import pro.akii.ks.BreathKOTH;
import pro.akii.ks.config.ConfigManager;
import pro.akii.ks.utils.NMSUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DragonBreathTask extends BukkitRunnable {

    private final BreathKOTH plugin;
    private final ConfigManager configManager;
    private final List<KothZone> activeZones;

    private boolean isFlaming = false;
    private int flameTicksRemaining = 0;
    private boolean cloudsSpawned = false;
    private final Map<String, Integer> globalItemCaps = new HashMap<>(); // Added: Global item caps per flame cycle

    public DragonBreathTask(BreathKOTH plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.activeZones = new ArrayList<>(configManager.loadKothZones());
        if (activeZones.isEmpty()) {
            throw new IllegalStateException("No valid KOTH zones loaded from config.");
        }
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();
        int duration = configManager.getDuration();
        int warningTime = configManager.getWarningTime();

        for (KothZone zone : activeZones) {
            zone.incrementElapsed(10);

            if (!isFlaming && zone.getTicksElapsed() >= zone.getInterval() - warningTime) {
                Bukkit.broadcastMessage("§cThe dragon awakens in " + (warningTime / 20) + " seconds at " + zone.getName() + "!");
                if (configManager.isSoundEnabled()) {
                    getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                            zone.getCenter().getWorld().playSound(zone.getCenter(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f));
                }
            }

            if (zone.getTicksElapsed() >= zone.getInterval()) {
                if (!isFlaming) {
                    isFlaming = true;
                    flameTicksRemaining = duration;
                    cloudsSpawned = false;
                    globalItemCaps.clear(); // Reset global caps at start of each flame cycle
                    Bukkit.broadcastMessage("§4The dragon unleashes its flames at " + zone.getName() + "!");
                    executeEvents(zone, "start");
                }

                AtomicInteger particleCount = new AtomicInteger();
                NMSUtils.spawnDragonBreath(zone.getCenter(), zone.getRadius(), configManager.getParticleDensity(),
                        configManager.getParticleType(zone.getName()), configManager.getMaxParticles(), particleCount);
                if (configManager.isSoundEnabled()) {
                    zone.getCenter().getWorld().playSound(zone.getCenter(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
                }
                if (configManager.isSpawnDragonEnabled()) {
                    NMSUtils.spawnVisualDragon(zone.getCenter());
                }

                List<Map<String, Object>> cloudEffects = configManager.getCloudEffects(zone.getName());
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (zone.isInZone(player) && zone.canAffect(player)) {
                        NMSUtils.applyDragonBreathDamage(player, configManager.getDamagePerTick(zone.getName()));
                        long damage = (long) (configManager.getDamagePerTick(zone.getName()) * 20);
                        plugin.getPlayerStats().incrementDamage(player.getUniqueId(), zone.getName(), damage);
                    }
                }

                if (!cloudsSpawned) {
                    NMSUtils.spawnLingeringClouds(zone.getCenter(), zone.getRadius(), configManager.getCloudDuration(zone.getName()), cloudEffects);
                    cloudsSpawned = true;
                }

                flameTicksRemaining -= 10;
                if (flameTicksRemaining <= 0) {
                    isFlaming = false;
                    zone.resetElapsed();
                    cloudsSpawned = false;
                    Bukkit.broadcastMessage("§aThe dragon’s fury subsides at " + zone.getName() + "... beware the lingering breath!");
                    executeEvents(zone, "end");
                }
            }
        }

        if (configManager.isDebugMode()) {
            long durationNs = System.nanoTime() - startTime;
            configManager.logDebug("Task execution took " + (durationNs / 1_000_000) + "ms");
        }
    }

    private void executeEvents(KothZone zone, String phase) {
        for (String event : zone.getEvents()) {
            String[] parts = event.split(":", 5); // Increased to 5 for random flag/global cap
            if (parts.length < 2) continue;

            String typeWithPhase = parts[0].trim().toLowerCase();
            if (!typeWithPhase.startsWith(phase)) continue;

            String type = typeWithPhase.replace(phase, "").trim();
            String value = parts.length > 1 ? parts[1].trim() : "";
            String args = parts.length > 2 ? parts[2].trim() : "";
            String limitStr = parts.length > 3 ? parts[3].trim() : "all";
            String extra = parts.length > 4 ? parts[4].trim() : ""; // For random flag or global cap

            int playerLimit;
            try {
                playerLimit = limitStr.equals("all") ? Integer.MAX_VALUE : Integer.parseInt(limitStr);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid player limit for event in zone " + zone.getName() + ": " + limitStr);
                continue;
            }

            boolean isRandom = extra.equalsIgnoreCase("random");
            int globalCap = extra.matches("\\d+") ? Integer.parseInt(extra) : Integer.MAX_VALUE;

            List<Player> eligiblePlayers = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (zone.isInZone(player) && zone.canAffect(player)) {
                    eligiblePlayers.add(player);
                }
            }

            List<Player> affectedPlayers;
            if (isRandom && eligiblePlayers.size() > playerLimit) {
                Collections.shuffle(eligiblePlayers);
                affectedPlayers = eligiblePlayers.subList(0, Math.min(playerLimit, eligiblePlayers.size()));
            } else {
                affectedPlayers = eligiblePlayers.subList(0, Math.min(playerLimit, eligiblePlayers.size()));
            }

            switch (type) {
                case "broadcast":
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', value));
                    break;
                case "spawn":
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "summon " + value + " " +
                                    zone.getCenter().getX() + " " + zone.getCenter().getY() + " " + zone.getCenter().getZ());
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to spawn entity for zone " + zone.getName() + ": " + e.getMessage());
                        }
                    });
                    break;
                case "give":
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String[] giveParts = (value + " " + args).split("\\s+", 3);
                        if (giveParts.length < 2) {
                            plugin.getLogger().warning("Invalid give event format for zone " + zone.getName() + ": " + event);
                            return;
                        }
                        String itemName = giveParts[0].toUpperCase();
                        int amount;
                        try {
                            amount = Math.min(Integer.parseInt(giveParts[1]), 64);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid amount for give event in zone " + zone.getName() + ": " + giveParts[1]);
                            return;
                        }
                        Material material = Material.getMaterial(itemName);
                        if (material == null) {
                            plugin.getLogger().warning("Invalid item type for give event in zone " + zone.getName() + ": " + itemName);
                            return;
                        }

                        int totalGiven = globalItemCaps.getOrDefault(itemName, 0);
                        int remainingCap = Math.max(0, globalCap - totalGiven);
                        int itemsToGive = Math.min(amount * affectedPlayers.size(), remainingCap);
                        if (itemsToGive <= 0) {
                            plugin.getLogger().info("Global cap reached for " + itemName + " in zone " + zone.getName());
                            return;
                        }

                        int itemsPerPlayer = itemsToGive / affectedPlayers.size();
                        int extraItems = itemsToGive % affectedPlayers.size();
                        ItemStack baseItem = new ItemStack(material, itemsPerPlayer);

                        for (Player player : affectedPlayers) {
                            ItemStack item = baseItem.clone();
                            if (extraItems > 0) {
                                item.setAmount(itemsPerPlayer + 1);
                                extraItems--;
                            }
                            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                            if (!leftover.isEmpty()) {
                                player.sendMessage("§cInventory full! Some items dropped at " + zone.getName() + ".");
                                Bukkit.getScheduler().runTask(plugin, () ->
                                        player.getWorld().dropItemNaturally(player.getLocation(), leftover.values().iterator().next()));
                            } else {
                                player.sendMessage("§aReceived " + item.getAmount() + " " + itemName + " from " + zone.getName() + "!");
                            }
                            totalGiven += item.getAmount();
                        }
                        globalItemCaps.put(itemName, totalGiven);
                    });
                    break;
                case "effect":
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String[] effectParts = (value + " " + args).split("\\s+", 3);
                        if (effectParts.length < 3) {
                            plugin.getLogger().warning("Invalid effect event format for zone " + zone.getName() + ": " + event);
                            return;
                        }
                        String effectName = effectParts[0].toUpperCase();
                        int duration;
                        try {
                            duration = Math.min(Integer.parseInt(effectParts[1]) * 20, 600 * 20);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid duration for effect event in zone " + zone.getName() + ": " + effectParts[1]);
                            return;
                        }
                        int amplifier;
                        try {
                            amplifier = Math.min(Integer.parseInt(effectParts[2]), 255);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid amplifier for effect event in zone " + zone.getName() + ": " + effectParts[2]);
                            return;
                        }
                        PotionEffectType effectType = PotionEffectType.getByName(effectName);
                        if (effectType == null) {
                            plugin.getLogger().warning("Invalid effect type for zone " + zone.getName() + ": " + effectName);
                            return;
                        }
                        PotionEffect effect = new PotionEffect(effectType, duration, amplifier, true, true);
                        for (Player player : affectedPlayers) {
                            PotionEffect existing = player.getActivePotionEffects().stream()
                                    .filter(e -> e.getType().equals(effectType))
                                    .findFirst().orElse(null);
                            if (existing != null && existing.getAmplifier() > amplifier) {
                                continue;
                            }
                            if (existing != null) {
                                player.removePotionEffect(effectType);
                            }
                            player.addPotionEffect(effect);
                            player.sendMessage("§aReceived " + effectName + " " + (amplifier + 1) + " for " + (duration / 20) + "s from " + zone.getName() + "!");
                        }
                    });
                    break;
                default:
                    plugin.getLogger().warning("Unknown event type for zone " + zone.getName() + ": " + type);
            }
        }
    }

    public void startFlameBursts() {
        for (KothZone zone : activeZones) {
            zone.resetElapsed();
            zone.incrementElapsed(configManager.getInterval() - configManager.getWarningTime());
        }
    }

    public void stopFlameBursts() {
        isFlaming = false;
        cloudsSpawned = false;
        for (KothZone zone : activeZones) {
            zone.resetElapsed();
        }
    }

    public void addZone(KothZone zone) {
        activeZones.add(zone);
        configManager.saveZones(activeZones);
        plugin.getLogger().info("Added zone: " + zone.getName());
    }

    public boolean removeZone(String name) {
        boolean removed = activeZones.removeIf(zone -> zone.getName().equals(name));
        if (removed) {
            configManager.saveZones(activeZones);
            plugin.getLogger().info("Removed zone: " + name);
        }
        return removed;
    }

    public List<KothZone> getActiveZones() {
        return new ArrayList<>(activeZones);
    }

    public BreathKOTH getPlugin() {
        return plugin;
    }
}
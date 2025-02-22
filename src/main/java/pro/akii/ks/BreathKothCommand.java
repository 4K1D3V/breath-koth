package pro.akii.ks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pro.akii.ks.gui.ZoneManagementGUI;
import pro.akii.ks.koth.DragonBreathTask;
import pro.akii.ks.koth.KothZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BreathKothCommand implements CommandExecutor, TabCompleter {

    private final BreathKOTH plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList("start", "stop", "addzone", "removezone", "reload", "gui");

    public BreathKothCommand(BreathKOTH plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("breathkoth.admin")) {
            sender.sendMessage("§cYou don’t have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        DragonBreathTask task = plugin.getDragonBreathTask();
        switch (args[0].toLowerCase()) {
            case "start":
                task.startFlameBursts();
                sender.sendMessage("§aDragon flames triggered!");
                break;
            case "stop":
                task.stopFlameBursts();
                sender.sendMessage("§aDragon flames stopped.");
                break;
            case "addzone":
                if (args.length != 7) {
                    sender.sendMessage("§cUsage: /breathkoth addzone <name> <x> <y> <z> <radius> <interval>");
                    return true;
                }
                try {
                    String name = args[1];
                    double x = Double.parseDouble(args[2]);
                    double y = Double.parseDouble(args[3]);
                    double z = Double.parseDouble(args[4]);
                    int radius = Integer.parseInt(args[5]);
                    int interval = Integer.parseInt(args[6]);
                    World world = sender instanceof Player ? ((Player) sender).getWorld() : plugin.getServer().getWorlds().get(0);
                    KothZone zone = new KothZone(new Location(world, x, y, z), radius, name, interval * 20, null);
                    task.addZone(zone);
                    sender.sendMessage("§aAdded zone: " + name + " with interval " + interval + "s");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid coordinates, radius, or interval.");
                }
                break;
            case "removezone":
                if (args.length != 2) {
                    sender.sendMessage("§cUsage: /breathkoth removezone <name>");
                    return true;
                }
                if (task.removeZone(args[1])) {
                    sender.sendMessage("§aRemoved zone: " + args[1]);
                } else {
                    sender.sendMessage("§cZone not found: " + args[1]);
                }
                break;
            case "reload":
                if (plugin.reloadConfigAndTask()) {
                    sender.sendMessage("§aConfiguration reloaded successfully!");
                } else {
                    sender.sendMessage("§cFailed to reload configuration.");
                }
                break;
            case "gui":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }
                new ZoneManagementGUI(plugin).open((Player) sender);
                break;
            default:
                sendUsage(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("breathkoth.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("removezone")) {
            return plugin.getDragonBreathTask().getActiveZones().stream()
                    .map(KothZone::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args[0].equalsIgnoreCase("addzone")) {
            if (args.length >= 3 && args.length <= 5) {
                if (sender instanceof Player) {
                    Location loc = ((Player) sender).getLocation();
                    if (args.length == 3) return List.of(String.valueOf(loc.getBlockX()));
                    if (args.length == 4) return List.of(String.valueOf(loc.getBlockY()));
                    if (args.length == 5) return List.of(String.valueOf(loc.getBlockZ()));
                }
            } else if (args.length == 6) {
                return List.of("5"); // Suggest default radius
            } else if (args.length == 7) {
                return List.of("300"); // Suggest default interval
            }
        }
        return new ArrayList<>();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§eBreathKOTH Commands:");
        sender.sendMessage("§e/breathkoth start - Trigger flames now.");
        sender.sendMessage("§e/breathkoth stop - Stop flames.");
        sender.sendMessage("§e/breathkoth addzone <name> <x> <y> <z> <radius> <interval> - Add a new zone.");
        sender.sendMessage("§e/breathkoth removezone <name> - Remove a zone.");
        sender.sendMessage("§e/breathkoth reload - Reload configuration.");
        sender.sendMessage("§e/breathkoth gui - Open zone management GUI.");
    }
}
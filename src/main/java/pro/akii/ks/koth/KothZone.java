package pro.akii.ks.koth;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class KothZone {

    private Location center;
    private int radius;
    private final int radiusSquared;
    private final String name;
    private int interval;
    private String permission;
    private boolean visualize = true;
    private List<String> events = new ArrayList<>();
    private int playerPresenceTicks = 0;
    private int ticksElapsed = 0;

    public KothZone(Location center, int radius, String name, int interval, String permission) {
        this.center = center;
        this.radius = radius;
        this.radiusSquared = radius * radius;
        this.name = name;
        this.interval = interval;
        this.permission = permission;
    }

    public boolean isInZone(Player player) {
        Location loc = player.getLocation();
        if (!loc.getWorld().equals(center.getWorld())) return false;

        double dx = loc.getX() - center.getX();
        double dy = loc.getY() - center.getY();
        double dz = loc.getZ() - center.getZ();
        return (dx * dx + dy * dy + dz * dz) <= radiusSquared;
    }

    public boolean canAffect(Player player) {
        return permission == null || player.hasPermission(permission) || player.hasPermission("breathkoth.zone.*");
    }

    public Location getCenter() {
        return center.clone();
    }

    public int getRadius() {
        return radius;
    }

    public String getName() {
        return name;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isVisualized() {
        return visualize;
    }

    public void setVisualize(boolean visualize) {
        this.visualize = visualize;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    public void updateZone(Location newCenter, int newRadius) {
        this.center = newCenter;
        this.radius = newRadius;
    }

    public void incrementPresence(int ticks) {
        playerPresenceTicks += ticks;
    }

    public void resetPresence() {
        playerPresenceTicks = 0;
    }

    public int getPresenceTicks() {
        return playerPresenceTicks;
    }

    public void incrementElapsed(int ticks) {
        ticksElapsed += ticks;
    }

    public int getTicksElapsed() {
        return ticksElapsed;
    }

    public void resetElapsed() {
        ticksElapsed = 0;
    }
}
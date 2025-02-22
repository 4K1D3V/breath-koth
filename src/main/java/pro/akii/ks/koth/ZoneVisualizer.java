package pro.akii.ks.koth;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import pro.akii.ks.BreathKOTH;

import java.util.List;

public class ZoneVisualizer extends BukkitRunnable {

    private final BreathKOTH plugin;
    private final DragonBreathTask task;

    public ZoneVisualizer(BreathKOTH plugin, DragonBreathTask task) {
        this.plugin = plugin;
        this.task = task;
    }

    @Override
    public void run() {
        List<KothZone> zones = task.getActiveZones();
        for (KothZone zone : zones) {
            if (!zone.isVisualized() || !plugin.getConfigManager().isVisualizeZonesEnabled()) continue;

            Location center = zone.getCenter();
            int radius = zone.getRadius();
            World world = center.getWorld();

            // Adjust particle density based on radius (more points for larger zones)
            int points = Math.max(32, radius * 4); // Minimum 32 points, scales with radius
            double angleStep = 2 * Math.PI / points;

            // Draw circle outline with height variation
            for (double angle = 0; angle < 2 * Math.PI; angle += angleStep) {
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                double baseY = center.getY() + 1;

                // Add height variation (e.g., ±1 block based on radius)
                double heightVariation = Math.sin(angle * 2) * Math.min(radius / 5.0, 1.0); // Up to ±1 block
                for (double yOffset = -heightVariation; yOffset <= heightVariation; yOffset += 0.5) {
                    world.spawnParticle(Particle.FLAME, x, baseY + yOffset, z, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}
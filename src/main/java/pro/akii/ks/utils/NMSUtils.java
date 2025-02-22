package pro.akii.ks.utils;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public class NMSUtils {

    public static void spawnDragonBreath(Location center, int radius, double density, String particleType, int maxParticles, AtomicInteger particleCount) {
        World world = center.getWorld();
        Particle particle = Particle.valueOf(particleType);
        int totalParticles = (int) (radius * radius * Math.PI * density);
        totalParticles = Math.min(totalParticles, maxParticles);

        for (int i = 0; i < totalParticles && particleCount.get() < maxParticles; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * radius;
            double x = center.getX() + distance * Math.cos(angle);
            double y = center.getY() + (Math.random() - 0.5) * 2;
            double z = center.getZ() + distance * Math.sin(angle);
            world.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
            particleCount.incrementAndGet();
        }
    }

    public static void spawnLingeringClouds(Location center, int radius, int duration, List<Map<String, Object>> effects) {
        World world = center.getWorld();
        int clouds = (int) (radius / 2.0);
        for (int i = 0; i < clouds; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * radius;
            double x = center.getX() + distance * Math.cos(angle);
            double y = center.getY();
            double z = center.getZ() + distance * Math.sin(angle);
            AreaEffectCloud cloud = (AreaEffectCloud) world.spawnEntity(new Location(world, x, y, z), org.bukkit.entity.EntityType.AREA_EFFECT_CLOUD);
            cloud.setDuration(duration);
            cloud.setRadius(2.0f);
            cloud.setRadiusPerTick(-0.005f);
            for (Map<String, Object> effect : effects) {
                PotionEffectType type = PotionEffectType.getByName((String) effect.get("type"));
                if (type != null) {
                    cloud.addCustomEffect(new PotionEffect(type, (int) effect.get("duration"), (int) effect.get("amplifier")), true);
                }
            }
        }
    }

    public static void applyDragonBreathDamage(Player player, double damagePerTick) {
        ((CraftPlayer) player).getHandle().hurt(player.getWorld().getHandle().damageSources().dragonBreath(), (float) damagePerTick);
    }

    public static void spawnVisualDragon(Location location) {
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
        EnderDragon dragon = new EnderDragon(EntityType.ENDER_DRAGON, world);
        dragon.setPos(location.getX(), location.getY() + 10, location.getZ());
        dragon.setNoAi(true);
        dragon.setInvisible(true);
        world.addFreshEntity(dragon);
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("BreathKOTH"), dragon::discard, 20L * 5); // Remove after 5 seconds
    }
}
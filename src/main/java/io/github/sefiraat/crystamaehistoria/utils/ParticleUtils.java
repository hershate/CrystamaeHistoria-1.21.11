package io.github.sefiraat.crystamaehistoria.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class ParticleUtils {

    @ParametersAreNonnullByDefault
    public static void displayParticleEffect(Entity entity, Particle particle, double rangeRadius) {
        displayParticleEffect(entity.getLocation(), particle, rangeRadius, 5);
    }

    @ParametersAreNonnullByDefault
    public static void displayParticleEffect(Location location, Particle particle, double rangeRadius, int numberOfParticles) {
        // 客户端原生散布：count+offset 重载由客户端按盒偏移生成随机云——
        // 原 N 次单发调用（N 个粒子包 + 服务端 N×3 随机数与坐标 set）
        // 归一为单次调用（单包）；散布形态同为逐轴均匀云
        location.getWorld().spawnParticle(particle, location, numberOfParticles, rangeRadius, rangeRadius, rangeRadius);
    }

    @ParametersAreNonnullByDefault
    public static void displayParticleEffect(Entity entity, Particle particle, double rangeRadius, int numberOfParticles) {
        // getLocation() 已返回新实例，无需再克隆
        displayParticleEffect(entity.getLocation().add(0, 1, 0), particle, rangeRadius, numberOfParticles);
    }

    @ParametersAreNonnullByDefault
    public static void displayParticleEffect(Location location, Particle particle, double rangeRadius) {
        displayParticleEffect(location, particle, rangeRadius, 5);
    }

    @ParametersAreNonnullByDefault
    public static void displayParticleEffect(Entity entity, double rangeRadius, int numberOfParticles, Particle.DustOptions dustOptions) {
        displayParticleEffect(entity.getLocation(), rangeRadius, numberOfParticles, dustOptions);
    }

    @ParametersAreNonnullByDefault
    public static void displayParticleEffect(Location location, double rangeRadius, int numberOfParticles, Particle.DustOptions dustOptions) {
        // 客户端原生散布（同上，DUST 数据变体）：单次调用单包
        location.getWorld().spawnParticle(Particle.DUST, location, numberOfParticles, rangeRadius, rangeRadius, rangeRadius, dustOptions);
    }

    @ParametersAreNonnullByDefault
    public static void displayParticleEffect(Entity entity, double rangeRadius, Particle.DustOptions dustOptions) {
        displayParticleEffect(entity.getLocation(), rangeRadius, 5, dustOptions);
    }

    @ParametersAreNonnullByDefault
    public static void drawLine(Particle particle, Location start, Location end, double space) {
        drawLine(particle, start, end, space, null);
    }

    @ParametersAreNonnullByDefault
    public static void drawLine(Particle particle, Location start, Location end, double space, @Nullable Particle.DustOptions dustOptions) {
        final double distance = start.distance(end);
        double currentPoint = 0;
        final Vector startVector = start.toVector();
        final Vector endVector = end.toVector();
        final Vector vector = endVector.clone().subtract(startVector).normalize().multiply(space);

        while (currentPoint < distance) {
            if (dustOptions != null) {
                start.getWorld().spawnParticle(
                    particle,
                    startVector.getX(),
                    startVector.getY(),
                    startVector.getZ(),
                    1,
                    dustOptions
                );
            } else {
                start.getWorld().spawnParticle(
                    particle,
                    startVector.getX(),
                    startVector.getY(),
                    startVector.getZ(),
                    1
                );
            }
            currentPoint += space;
            startVector.add(vector);
        }
    }

    @ParametersAreNonnullByDefault
    public static void drawLine(@Nullable Particle.DustOptions dustOptions, Location start, Location end, double space) {
        drawLine(Particle.DUST, start, end, space, dustOptions);
    }

    @ParametersAreNonnullByDefault
    public static List<Location> getLine(Location start, Location end, double space) {
        final double distance = start.distance(end);
        double currentPoint = 0;
        final Vector startVector = start.toVector();
        final Vector endVector = end.toVector();
        final Vector vector = endVector.clone().subtract(startVector).normalize().multiply(space);

        List<Location> locations = new ArrayList<>();

        while (currentPoint < distance) {
            locations.add(new Location(
                start.getWorld(),
                startVector.getX(),
                startVector.getY(),
                startVector.getZ()
            ));

            currentPoint += space;
            startVector.add(vector);
        }
        return locations;
    }

    @ParametersAreNonnullByDefault
    public static void drawCube(Particle particle, Location corner1, Location corner2, double space) {
        drawCube(particle, corner1, corner2, space, null);
    }

    /**
     * https://www.spigotmc.org/threads/create-particles-in-cube-outline-shape.65991/
     */
    @ParametersAreNonnullByDefault
    public static void drawCube(Particle particle, Location corner1, Location corner2, double particleDistance, @Nullable Particle.DustOptions dustOptions) {
        World world = corner1.getWorld();
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        for (double x = minX; x <= maxX; x += particleDistance) {
            for (double y = minY; y <= maxY; y += particleDistance) {
                for (double z = minZ; z <= maxZ; z += particleDistance) {
                    int components = 0;
                    if (x == minX || x == maxX) components++;
                    if (y == minY || y == maxY) components++;
                    if (z == minZ || z == maxZ) components++;
                    if (components >= 2) {
                        if (dustOptions != null) {
                            world.spawnParticle(particle, x, y, z, 1, dustOptions);
                        } else {
                            world.spawnParticle(particle, x, y, z, 1);
                        }
                    }
                }
            }
        }
    }

    @ParametersAreNonnullByDefault
    public static void drawCube(@Nullable Particle.DustOptions dustOptions, Location corner1, Location corner2, double space) {
        drawCube(Particle.DUST, corner1, corner2, space, dustOptions);
    }
}

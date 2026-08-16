package io.github.sefiraat.crystamaehistoria.runnables.spells;

import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

/**
 * 隧道钻探法术的周期钻进任务：每 tick 以钻探实体为中心清理半径内可破坏方块。
 * （原类注释"Removed due to issues"已过时——法术在 SpellType 中注册且可达。）
 */
public class TunnelBoreRunnable extends BukkitRunnable {

    private final LivingEntity bore;
    private final int radius;
    private final UUID owner;
    private int iterations;

    @ParametersAreNonnullByDefault
    public TunnelBoreRunnable(LivingEntity bore, int radius, UUID owner, int iterations) {
        this.bore = bore;
        this.radius = radius;
        this.owner = owner;
        this.iterations = iterations;
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        bore.remove();
        super.cancel();
    }

    @Override
    public void run() {
        if (iterations <= 0) {
            this.cancel();
        } else {
            // 直接按坐标遍历方块：三重循环的 (x,y,z) 组合构造上互异，原实现的
            // BlockPosition 列表 + contains 去重（O(n²)，半径 5 时约 88 万次比较/迭代）
            // 与至多 1331 个中间对象分配均为无效工作；访问顺序与原集合构建序一致
            final Location location = bore.getLocation();
            final org.bukkit.World world = location.getWorld();
            final int baseX = location.getBlockX();
            final int baseY = location.getBlockY();
            final int baseZ = location.getBlockZ();

            for (int x = baseX - radius; x <= baseX + radius; x++) {
                for (int y = baseY - radius; y <= baseY + radius; y++) {
                    for (int z = baseZ - radius; z <= baseZ + radius; z++) {
                        final Block block = world.getBlockAt(x, y, z);
                        if (GeneralUtils.blockCanBeBroken(this.owner, block)) {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }

            ParticleUtils.displayParticleEffect(bore, Particle.FALLING_LAVA, radius, 10);
        }
        iterations--;
    }
}

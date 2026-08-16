package io.github.sefiraat.crystamaehistoria.utils.mobgoals;

import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Goat;

import java.util.UUID;

public class BatteringRamGoal extends AbstractGoal<Goat> {

    public BatteringRamGoal(UUID owningPlayer) {
        super(owningPlayer);
    }

    @Override
    public void tick() {
        final org.bukkit.util.Vector velocity = self.getVelocity();
        final double velX = Math.abs(velocity.getX());
        final double velZ = Math.abs(velocity.getZ());

        if (self.isOnGround() || (velX < 0.1 && velZ < 0.1)) {
            ParticleUtils.displayParticleEffect(self, Particle.ANGRY_VILLAGER, 1, 5);
            self.remove();
            return;
        }

        // 直接按坐标遍历方块：三重循环的 (x,y,z) 组合构造上互异，原实现的
        // BlockPosition 列表 + contains 去重（O(n²) ≈ 2800 次比较/tick）与
        // 75 个中间对象分配均为无效工作；访问顺序与原集合构建序一致
        final Location location = self.getLocation();
        final World world = location.getWorld();
        final int radius = 2;
        final int baseX = location.getBlockX();
        final int baseY = location.getBlockY();
        final int baseZ = location.getBlockZ();

        for (int x = baseX - radius; x <= baseX + radius; x++) {
            for (int y = baseY; y <= baseY + radius; y++) {
                for (int z = baseZ - radius; z <= baseZ + radius; z++) {
                    final Block block = world.getBlockAt(x, y, z);
                    final BlockData blockData = block.getBlockData();
                    if (GeneralUtils.blockCanBeBroken(this.owner, block)) {
                        block.setType(Material.AIR);
                        final FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(block.getLocation(), blockData);
                        GeneralUtils.pushEntity(this.owner, self.getLocation(), fallingBlock, 0.5);
                    }
                }
            }
        }
    }

    @Override
    public boolean getTickCondition() {
        return false;
    }

    @Override
    public boolean getTargetsEnemies() {
        return false;
    }

    @Override
    public boolean getFollowsPlayer() {
        return false;
    }
}

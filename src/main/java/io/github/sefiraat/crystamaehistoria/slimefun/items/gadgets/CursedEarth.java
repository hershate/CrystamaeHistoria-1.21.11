package io.github.sefiraat.crystamaehistoria.slimefun.items.gadgets;

import io.github.sefiraat.crystamaehistoria.utils.ParticleUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CursedEarth extends SlimefunItem {

    @Getter
    private final double ticksToSpawn;
    @Getter
    private final int lightLevel;
    @Getter
    private final List<EntityType> spawns;
    @Getter
    private final Particle.DustOptions dustOptions;

    /**
     * 每方块独立的刷怪计时。原实现用单个实例字段：SlimefunItem 为单例，
     * N 块诅咒之土共享同一计数器，计数以 N 倍速推进——刷怪频率随方块数
     * 失控（多方块状态污染）。破坏时清理条目。
     */
    private final Map<Location, Integer> tickCounters = new HashMap<>();

    @ParametersAreNonnullByDefault
    public CursedEarth(ItemGroup category,
                       SlimefunItemStack item,
                       RecipeType recipeType,
                       ItemStack[] recipe,
                       double ticksToSpawn,
                       int lightLevel,
                       List<EntityType> spawns,
                       Color color
    ) {
        super(category, item, recipeType, recipe);
        this.ticksToSpawn = ticksToSpawn;
        this.lightLevel = lightLevel;
        this.spawns = spawns;
        this.dustOptions = new Particle.DustOptions(color, 1);
    }

    @Override
    public void preRegister() {
        addItemHandler(onTick(), onBlockBreak());
    }

    private BlockBreakHandler onBlockBreak() {
        return new BlockBreakHandler(false, false) {
            @Override
            @ParametersAreNonnullByDefault
            public void onPlayerBreak(BlockBreakEvent event, ItemStack item, List<ItemStack> drops) {
                tickCounters.remove(event.getBlock().getLocation());
            }
        };
    }

    private BlockTicker onTick() {
        return new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return true;
            }

            @Override
            public void tick(Block block, SlimefunItem slimefunItem, Config config) {
                final Location location = block.getLocation().add(0.5, 1.5, 0.5);
                if (block.isEmpty()) {
                    // 方块已不存在：清理计数与 BlockStorage 信息，防止残留
                    tickCounters.remove(block.getLocation());
                    BlockStorage.clearBlockInfo(block.getLocation());
                    return;
                }
                int currentTick = tickCounters.getOrDefault(block.getLocation(), 0);
                if (currentTick >= ticksToSpawn) {
                    final Block blockA = block.getRelative(BlockFace.UP);
                    final Block blockB = blockA.getRelative(BlockFace.UP);
                    if (blockA.getLightLevel() <= lightLevel
                        && blockA.isEmpty()
                        && blockB.isEmpty()
                        && location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5).isEmpty()
                        && location.getWorld().getNearbyEntities(location, 4, 4, 4, LivingEntity.class::isInstance).size() < 10
                    ) {
                        location.getWorld().spawnEntity(
                            location,
                            spawns.get(ThreadLocalRandom.current().nextInt(spawns.size())),
                            true
                        );
                    }
                    currentTick = 0;
                } else {
                    currentTick++;
                }
                tickCounters.put(block.getLocation(), currentTick);
                ParticleUtils.displayParticleEffect(
                    location,
                    1,
                    3,
                    dustOptions
                );
            }
        };
    }
}

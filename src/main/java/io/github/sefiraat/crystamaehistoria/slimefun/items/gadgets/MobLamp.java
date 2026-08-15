package io.github.sefiraat.crystamaehistoria.slimefun.items.gadgets;

import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MobLamp extends SlimefunItem {

    /**
     * 所有者缓存（懒加载）：CH_UUID 放置后不变，首个 tick 从 BlockStorage 读入
     * 并解析一次，之后每 tick 直接查表（原实现每 tick 字符串读取 + UUID.fromString）。
     * null 值表示已解析过但缺失/损坏（失败关闭跳过）。破坏时清理条目。
     */
    @Getter
    private final Map<Location, UUID> ownerMap = new HashMap<>();

    @Getter
    private final double radius;
    @Getter
    private final double force;

    @ParametersAreNonnullByDefault
    public MobLamp(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, double radius, double force) {
        super(category, item, recipeType, recipe);
        this.radius = radius;
        this.force = force;
        this.addItemHandler(
            new BlockPlaceHandler(false) {
                @Override
                public void onPlayerPlace(@Nonnull BlockPlaceEvent event) {
                    BlockStorage.addBlockInfo(event.getBlock(), "CH_UUID", event.getPlayer().getUniqueId().toString());
                }
            },
            new BlockBreakHandler(false, false) {
                @Override
                public void onPlayerBreak(BlockBreakEvent blockBreakEvent, ItemStack itemStack, List<ItemStack> list) {
                    BlockStorage.clearBlockInfo(blockBreakEvent.getBlock());
                    // 破坏后清除缓存条目，否则随放置/破坏循环无界增长
                    ownerMap.remove(blockBreakEvent.getBlock().getLocation());
                }
            },
            new BlockTicker() {
                @Override
                public boolean isSynchronized() {
                    return true;
                }

                @Override
                public void tick(Block block, SlimefunItem slimefunItem, Config config) {
                    // 每 tick 单次 getLocation 复用；所有者懒缓存（键缺失/损坏时失败关闭）
                    final Location blockLocation = block.getLocation();
                    if (!ownerMap.containsKey(blockLocation)) {
                        final String ownerString = BlockStorage.getLocationInfo(blockLocation, "CH_UUID");
                        UUID owner = null;
                        if (ownerString != null) {
                            try {
                                owner = UUID.fromString(ownerString);
                            } catch (IllegalArgumentException e) {
                                // UUID 损坏：登记 null（失败关闭）
                            }
                        }
                        ownerMap.put(blockLocation, owner);
                    }
                    final UUID uuid = ownerMap.get(blockLocation);
                    if (uuid == null) {
                        return;
                    }
                    // 推挤中心点移出怪物循环（原实现每怪物分配一次）
                    final Location center = blockLocation.clone().add(0.5, 0.5, 0.5);
                    for (Monster monster : block.getWorld().getNearbyEntitiesByType(Monster.class, blockLocation, radius)) {
                        GeneralUtils.pushEntity(
                            uuid,
                            center,
                            monster,
                            force
                        );
                    }
                }
            }
        );
    }
}

package io.github.sefiraat.crystamaehistoria.slimefun.types;

import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.TickingBlockNoGui;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Stand extends TickingBlockNoGui {

    protected static final String PDC_ITEM = "itemUUID";

    @Getter
    protected final Map<Location, UUID> itemMap = new HashMap<>();
    @Getter
    protected final Map<Location, Integer> currentTickMap = new HashMap<>();

    @ParametersAreNonnullByDefault
    protected Stand(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        super.preRegister();
        addItemHandler((BlockUseHandler) this::onRightClick);
    }

    public abstract void onRightClick(PlayerRightClickEvent e);

    @Override
    @ParametersAreNonnullByDefault
    protected void onFirstTick(Block block, SlimefunItem slimefunItem, Config config) {
        final Location blockLocation = block.getLocation();
        String itemUuidString = BlockStorage.getLocationInfo(block.getLocation(), PDC_ITEM);
        if (itemUuidString != null) {
            try {
                itemMap.put(block.getLocation(), UUID.fromString(itemUuidString));
            } catch (IllegalArgumentException e) {
                // BlockStorage 为持久化数据，不可信：损坏 UUID 不登记（视为无展示物品）
            }
        }
        // Set a random current tick
        Stand.this.currentTickMap.put(
            blockLocation,
            ThreadLocalRandom.current().nextInt(3, 7)
        );
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void onTick(Block block, SlimefunItem slimefunItem, Config config) {
        final Location blockLocation = block.getLocation();
        final UUID currentItemUuid = itemMap.get(blockLocation);

        // If an item isn't in place, then don't do anything
        if (currentItemUuid != null) {
            int currentTick = currentTickMap.get(blockLocation);
            final Item currentItem = (Item) Bukkit.getEntity(currentItemUuid);

            if (currentItem == null) {
                return;
            }

            // Only tick periodically
            if (currentTick <= 0 && Stand.this.itemMap.containsKey(blockLocation)) {
                final Location itemLocation = currentItem.getLocation();
                final Location desiredLocation = blockLocation.clone().add(0.5, 1.5, 0.5);

                // Check if item has moved off the platform
                // （跨世界 distance 会抛 IllegalArgumentException——按已离开平台处理）
                if (itemLocation.getWorld() != desiredLocation.getWorld()
                    || itemLocation.distance(desiredLocation) > 0.3) {
                    final ItemStack itemStack = currentItem.getItemStack();
                    blockLocation.getWorld().dropItemNaturally(blockLocation, itemStack);
                    BlockStorage.addBlockInfo(block, PDC_ITEM, null);
                    itemMap.remove(blockLocation);
                    currentItem.remove();
                }

                Stand.this.currentTickMap.put(blockLocation, ThreadLocalRandom.current().nextInt(4, 8));
            } else {
                currentTick--;
                Stand.this.currentTickMap.put(blockLocation, currentTick);
            }
            afterTick(currentItem, block, slimefunItem, config);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void onPlace(BlockPlaceEvent event) {
        Stand.this.currentTickMap.put(
            event.getBlock().getLocation(),
            ThreadLocalRandom.current().nextInt(3, 7)
        );
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void onBreak(BlockBreakEvent blockBreakEvent, ItemStack itemStack, List<ItemStack> list) {
        Location location = blockBreakEvent.getBlock().getLocation();
        // 破坏后清理两个映射，否则随放置/破坏循环无界增长
        currentTickMap.remove(location);
        final UUID currentItemUuid = itemMap.remove(location);
        if (currentItemUuid != null) {
            final Item currentItem = (Item) Bukkit.getEntity(currentItemUuid);
            if (currentItem != null) {
                final ItemStack displayStack = currentItem.getItemStack();
                location.getWorld().dropItemNaturally(location, displayStack);
                currentItem.remove();
            }
            BlockStorage.clearBlockInfo(location);
        }
    }

    @ParametersAreNonnullByDefault
    public abstract void afterTick(Item item, Block block, SlimefunItem slimefunItem, Config config);

    /**
     * 展示物品实体 → 已解析 {@link SlimefunItem}（afterTick 每 tick 调用，
     * 委托共享弱缓存 {@link SlimefunItemResolver}——生命周期闭合论证见其
     * javadoc；round-39 引入，round-40 统一至共享实现）。
     */
    @ParametersAreNonnullByDefault
    protected SlimefunItem resolveDisplayItem(Item item) {
        return io.github.sefiraat.crystamaehistoria.utils.SlimefunItemResolver.resolve(item);
    }
}

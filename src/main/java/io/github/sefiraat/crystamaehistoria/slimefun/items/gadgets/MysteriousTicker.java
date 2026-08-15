package io.github.sefiraat.crystamaehistoria.slimefun.items.gadgets;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class MysteriousTicker extends SlimefunItem {

    private final Set<Material> materials;
    /**
     * 抽取用数组缓存：原实现每次随机变换都 materials.toArray(new Material[]{})
     * 复制整个数组——材质集合构造后固定，预生成一次。
     */
    private final Material[] materialArray;
    private final Map<Location, Integer> tickMap = new HashMap<>();
    private final int ticks;
    private final Consumer<Block> consumer;

    @ParametersAreNonnullByDefault
    public MysteriousTicker(ItemGroup category,
                            SlimefunItemStack item,
                            RecipeType recipeType,
                            ItemStack[] recipe,
                            Set<Material> tickingMaterials,
                            int tickFrequency
    ) {
        this(category, item, recipeType, recipe, tickingMaterials, tickFrequency, null);
    }

    @ParametersAreNonnullByDefault
    public MysteriousTicker(ItemGroup category,
                            SlimefunItemStack item,
                            RecipeType recipeType,
                            ItemStack[] recipe,
                            Set<Material> tickingMaterials,
                            int tickFrequency,
                            @Nullable Consumer<Block> consumer
    ) {
        super(category, item, recipeType, recipe);
        this.materials = tickingMaterials;
        this.materialArray = tickingMaterials.toArray(new Material[0]);
        this.ticks = tickFrequency;
        this.consumer = consumer;
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
                tickMap.remove(event.getBlock().getLocation());
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
                // 每 tick 单次 getLocation 复用（查表/落盘/清理共用同一键）
                final Location blockLocation = block.getLocation();
                if (block.isEmpty()) {
                    // 方块已不存在：同步清理 BlockStorage 与计数条目（原实现泄漏 tickMap 条目）
                    BlockStorage.clearBlockInfo(blockLocation);
                    tickMap.remove(blockLocation);
                    return;
                }
                Integer currentTick = tickMap.get(blockLocation);
                if (currentTick == null) {
                    currentTick = ThreadLocalRandom.current().nextInt(ticks);
                }
                if (currentTick >= ticks) {
                    currentTick = 0;
                    block.setType(
                        materialArray[ThreadLocalRandom.current().nextInt(materialArray.length)]
                    );
                    if (MysteriousTicker.this.consumer != null) {
                        MysteriousTicker.this.consumer.accept(block);
                    }
                } else {
                    currentTick++;
                }
                tickMap.put(blockLocation, currentTick);
            }
        };
    }
}

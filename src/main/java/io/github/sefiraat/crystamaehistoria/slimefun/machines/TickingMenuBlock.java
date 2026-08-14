package io.github.sefiraat.crystamaehistoria.slimefun.machines;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 可 tick 的 {@link MenuBlock}。
 * <p>
 * 原实现来自 InfinityLib（io.github.mooy1.infinitylib.machines.TickingMenuBlock），
 * 为移除 InfinityLib 依赖而在本项目内等价移植：
 * 构造时注册旧版 {@link BlockTicker}，每个 Slimefun tick 回调 {@link #tick(Block, BlockMenu)}。
 */
@ParametersAreNonnullByDefault
public abstract class TickingMenuBlock extends MenuBlock {

    public TickingMenuBlock(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        addItemHandler(new BlockTicker() {

            @Override
            public boolean isSynchronized() {
                return synchronous();
            }

            @Override
            public void tick(Block b, SlimefunItem item, Config data) {
                BlockMenu menu = BlockStorage.getInventory(b);
                if (menu != null) {
                    TickingMenuBlock.this.tick(b, menu);
                }
            }

        });
    }

    protected abstract void tick(Block b, BlockMenu menu);

    protected boolean synchronous() {
        return false;
    }

}

package io.github.sefiraat.crystamaehistoria.slimefun.items.materials;

import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.LimitedUseItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

public class PowderedEssence extends LimitedUseItem {

    private static final NamespacedKey key = Keys.newKey("uses");

    @ParametersAreNonnullByDefault
    public PowderedEssence(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int amount) {
        super(group, item, recipeType, recipe);
        setMaxUseCount(amount);
    }

    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            Optional<Block> optionalBlock = e.getClickedBlock();

            if (!optionalBlock.isPresent()) {
                return;
            }

            Block block = optionalBlock.get();

            // 领地校验：原实现可在他人领地对作物施加骨粉（催熟他人作物），
            // 与同类作用方块的物品校验先例对齐
            if (!GeneralUtils.hasPermission(e.getPlayer(), block, Interaction.INTERACT_BLOCK)) {
                return;
            }

            if (block.applyBoneMeal(e.getClickedFace())) {
                damageItem(e.getPlayer(), e.getItem());
            }

        };
    }

    @Override
    protected @Nonnull
    NamespacedKey getStorageKey() {
        return key;
    }
}

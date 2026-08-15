package io.github.sefiraat.crystamaehistoria.slimefun.items.artistic;

import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ImbuedStand extends SlimefunItem {

    public static final NamespacedKey KEY = Keys.newKey("imbued_stand");

    public ImbuedStand(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        addItemHandler(onItemUse());
    }

    private ItemUseHandler onItemUse() {
        return e -> {
            final Optional<Block> optionalBlock = e.getClickedBlock();

            if (optionalBlock.isPresent()) {
                final Block block = optionalBlock.get();
                final Block targetBlock = block.getRelative(e.getClickedFace());
                // 领地校验：原实现可在他人领地内生成盔甲架（绕过保护插件的放置检查），
                // 与 Displacer/荧光勺等同类物品的校验先例对齐
                if (!GeneralUtils.hasPermission(e.getPlayer(), targetBlock, Interaction.PLACE_BLOCK)) {
                    return;
                }
                final Location location = targetBlock.getLocation().add(0.5, 0.5, 0.5);
                final Entity entity = location.getWorld().spawnEntity(
                    location,
                    EntityType.ARMOR_STAND,
                    CreatureSpawnEvent.SpawnReason.CUSTOM
                );

                PersistentDataAPI.setBoolean(entity, KEY, true);
                if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    e.getItem().setAmount(e.getItem().getAmount() - 1);
                }
            }
        };
    }
}

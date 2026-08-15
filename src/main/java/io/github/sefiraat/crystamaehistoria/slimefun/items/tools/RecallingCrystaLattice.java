package io.github.sefiraat.crystamaehistoria.slimefun.items.tools;

import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataType;
import io.github.sefiraat.crystamaehistoria.slimefun.items.gadgets.Waystone;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.Optional;

public class RecallingCrystaLattice extends SlimefunItem {

    @ParametersAreNonnullByDefault
    public RecallingCrystaLattice(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        addItemHandler(onItemUse());
    }

    private ItemUseHandler onItemUse() {
        return e -> {
            if (e.getPlayer().isSneaking()) {
                setLocation(e);
            } else {
                teleport(e);
            }
        };
    }

    @ParametersAreNonnullByDefault
    private void setLocation(PlayerRightClickEvent event) {
        final Optional<Block> blockOptional = event.getClickedBlock();

        if (blockOptional.isPresent()) {
            final Block block = blockOptional.get();
            final SlimefunItem slimefunItem = BlockStorage.check(block);
            final Location location = block.getLocation();
            final Player player = event.getPlayer();

            if (slimefunItem instanceof Waystone
                && GeneralUtils.hasPermission(player, location, Interaction.PLACE_BLOCK)
            ) {
                final ItemStack itemStack = event.getItem();
                final ItemMeta itemMeta = itemStack.getItemMeta();
                final PersistentDataContainer container = itemMeta.getPersistentDataContainer();

                container.set(Keys.newKey("location"), DataType.LOCATION, location);
                itemStack.setItemMeta(itemMeta);
                player.sendMessage(
                    MessageFormat.format("{0}请在聊天栏内输入该路标的名称.", ChatColor.LIGHT_PURPLE)
                );
                ChatUtils.awaitInput(player, s -> renameItem(s, itemStack));
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void teleport(PlayerRightClickEvent event) {
        final ItemStack itemStack = event.getItem();
        final ItemMeta itemMeta = itemStack.getItemMeta();
        final PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        // 物品 PDC 不可信（改造客户端可注入任意字节）：读取/反序列化异常与
        // 世界无法解析（序列化的世界名不存在）一律按"路标不可用"失败关闭
        final Location location;
        try {
            location = container.has(Keys.newKey("location"), DataType.LOCATION)
                ? container.get(Keys.newKey("location"), DataType.LOCATION)
                : null;
        } catch (RuntimeException e) {
            event.getPlayer().sendActionBar(
                Component.text("路标不可用")
                    .color(TextColor.color(200, 30, 40))
            );
            return;
        }

        if (location != null && location.getWorld() != null) {
            final Block block = location.getBlock();
            final SlimefunItem slimefunItem = BlockStorage.check(block);

            if (slimefunItem instanceof Waystone
                && GeneralUtils.hasPermission(event.getPlayer(), location, Interaction.PLACE_BLOCK)
            ) {
                event.getPlayer().teleportAsync(location.add(1, 1, 1), PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                event.getPlayer().sendActionBar(
                    Component.text("路标不可用")
                        .color(TextColor.color(200, 30, 40))
                );
            }
        } else if (location != null) {
            event.getPlayer().sendActionBar(
                Component.text("路标不可用")
                    .color(TextColor.color(200, 30, 40))
            );
        } else {
            event.getPlayer().sendMessage(
                MessageFormat.format("{0}请先使用Shift+右键点击来绑定一个路标.", ChatColor.RED)
            );
        }
    }

    @ParametersAreNonnullByDefault
    private void renameItem(String s, @Nullable ItemStack itemStack) {
        if (itemStack != null) {
            final ItemMeta itemMeta = itemStack.getItemMeta();

            itemMeta.setDisplayName(ThemeType.TOOL.getColor() + s);
            itemStack.setItemMeta(itemMeta);
        }
    }
}

package io.github.sefiraat.crystamaehistoria.utils.theme;

import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot;
import io.github.sefiraat.crystamaehistoria.utils.Skulls;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.experimental.UtilityClass;
import io.github.sefiraat.crystamaehistoria.utils.NameUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;

@UtilityClass
public class GuiElements {

    public static final ItemStack MENU_BACKGROUND = new ItemStack(ChestMenuUtils.getBackground());

    public static final ItemStack MENU_BACKGROUND_INPUT = CustomItemStack.create(
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        ChatColor.BLUE + "输入"
    );

    public static final ItemStack MENU_STAVE_INPUT = CustomItemStack.create(
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        ChatColor.BLUE + "在此处放入法杖"
    );

    public static final ItemStack MENU_REMOVE_PLATES = CustomItemStack.create(
        Material.ORANGE_STAINED_GLASS_PANE,
        ChatColor.BLUE + "移除法术板"
    );

    public static final ItemStack MENU_SAVE_STAVE = CustomItemStack.create(
        Material.GREEN_STAINED_GLASS_PANE,
        ChatColor.BLUE + "保存法杖设置"
    );

    public static final ItemStack MENU_BACKGROUND_OUTPUT = CustomItemStack.create(
        Material.ORANGE_STAINED_GLASS_PANE,
        ChatColor.RED + "输出"
    );

    public static final ItemStack MENU_DIVIDER = CustomItemStack.create(
        Material.LIME_STAINED_GLASS_PANE,
        " "
    );

    public static final ItemStack TIER_INDICATOR_1 = CustomItemStack.create(
        Skulls.GUI_TIER_NUMBER_1.getPlayerHead(),
        ThemeType.CLICK_INFO.getColor() + "T1"
    );

    public static final ItemStack TIER_INDICATOR_2 = CustomItemStack.create(
        Skulls.GUI_TIER_NUMBER_2.getPlayerHead(),
        ThemeType.CLICK_INFO.getColor() + "T2"
    );

    public static final ItemStack TIER_INDICATOR_3 = CustomItemStack.create(
        Skulls.GUI_TIER_NUMBER_3.getPlayerHead(),
        ThemeType.CLICK_INFO.getColor() + "T3"
    );

    public static final ItemStack TIER_INDICATOR_4 = CustomItemStack.create(
        Skulls.GUI_TIER_NUMBER_4.getPlayerHead(),
        ThemeType.CLICK_INFO.getColor() + "T4"
    );

    public static final ItemStack TIER_INDICATOR_5 = CustomItemStack.create(
        Skulls.GUI_TIER_NUMBER_5.getPlayerHead(),
        ThemeType.CLICK_INFO.getColor() + "T5"
    );

    @ParametersAreNonnullByDefault
    public static ItemStack getUniqueStoryIcon(Material material) {
        return ThemeType.themedItemStack(
            material,
            ThemeType.RARITY_UNIQUE,
            NameUtils.getMaterialName(material),
            "该故事已被发掘"
        );
    }

    @ParametersAreNonnullByDefault
    public static ItemStack getStoryNotUnlockedIcon(Material material) {
        return ThemeType.themedItemStack(
            Material.BARRIER,
            ThemeType.RESEARCH,
            NameUtils.getMaterialName(material),
            MessageFormat.format("{0}{1}已锁定", ThemeType.RESEARCH.getColor(), ChatColor.BOLD),
            "该故事还没有解锁",
            "当你使用记录者首次发掘",
            "指定方块的故事时才能解锁"
        );
    }

    @ParametersAreNonnullByDefault
    public static ItemStack getSpellNotUnlockedIcon(String name) {
        return ThemeType.themedItemStack(
            Material.BARRIER,
            ThemeType.RESEARCH,
            name,
            MessageFormat.format("{0}{1}已锁定", ThemeType.RESEARCH.getColor(), ChatColor.BOLD),
            "该法术还没有被解锁",
            "当你在液化池中首次",
            "充能魔法板时才能解锁法术"
        );
    }

    @ParametersAreNonnullByDefault
    public static ItemStack getBlockGildedIcon(Material material) {
        return ThemeType.themedItemStack(
            material,
            ThemeType.RARITY_UNIQUE,
            NameUtils.getMaterialName(material),
            "该方块已被镀金过"
        );
    }

    @ParametersAreNonnullByDefault
    public static ItemStack getBlockNotGildedIcon(Material material) {
        return ThemeType.themedItemStack(
            Material.BARRIER,
            ThemeType.RESEARCH,
            NameUtils.getMaterialName(material),
            MessageFormat.format("{0}{1}已锁定", ThemeType.RESEARCH.getColor(), ChatColor.BOLD),
            "该方块还未被镀金过"
        );
    }

    @ParametersAreNonnullByDefault
    public static ItemStack getSpellSlotPane(SpellSlot spellSlot) {
        return CustomItemStack.create(
            Material.RED_STAINED_GLASS_PANE,
            ChatColor.GRAY + "法术: " + spellSlot.getDescription()
        );
    }

    @ParametersAreNonnullByDefault
    public static ItemStack getDirectionalSlotPane(BlockFace blockFace, boolean active) {
        Material material = active ? Material.RED_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE;
        return CustomItemStack.create(
            material,
            ChatColor.GRAY + "设置方向: " + blockFace.name()
        );
    }
}

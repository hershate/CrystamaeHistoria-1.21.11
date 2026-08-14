package io.github.sefiraat.crystamaehistoria.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;

/**
 * 名称显示工具。
 * <p>
 * 原先依赖 GuizhanLibPlugin 的 MaterialHelper / ItemStackHelper /
 * DyeColorHelper / PotionEffectTypeHelper 提供中文名称。
 * 移除 GuizhanLibPlugin 依赖后，此处回退为格式化枚举名（Title Case），
 * 与 GuizhanLib 在缺失翻译条目时的回退行为一致，仅影响展示文本，不影响玩法。
 */
@UtilityClass
public class NameUtils {

    @Nonnull
    public static String getMaterialName(@Nonnull Material material) {
        return TextUtils.toTitleCase(material.name());
    }

    @Nonnull
    public static String getItemStackName(@Nonnull ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            return itemMeta.getDisplayName();
        }
        return getMaterialName(itemStack.getType());
    }

    @Nonnull
    public static String getDyeColorName(@Nonnull DyeColor dyeColor) {
        return TextUtils.toTitleCase(dyeColor.name());
    }

    @Nonnull
    public static String getPotionEffectTypeName(@Nonnull PotionEffectType potionEffectType) {
        return TextUtils.toTitleCase(potionEffectType.getName());
    }
}

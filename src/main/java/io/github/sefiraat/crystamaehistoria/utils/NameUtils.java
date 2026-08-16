package io.github.sefiraat.crystamaehistoria.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        return getItemStackName(itemStack.getItemMeta(), itemStack.getType());
    }

    /**
     * 同 {@link #getItemStackName(ItemStack)}，但接受调用方已持有的元数据快照
     * （单次往返提交路径使用，省一次克隆；各调用路径下快照的显示名与已应用状态一致）。
     */
    @Nonnull
    public static String getItemStackName(@Nullable ItemMeta itemMeta, @Nonnull Material material) {
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            return itemMeta.getDisplayName();
        }
        return getMaterialName(material);
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

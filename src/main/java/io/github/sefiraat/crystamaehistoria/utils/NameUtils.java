package io.github.sefiraat.crystamaehistoria.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 名称显示工具。
 * <p>
 * 原先依赖 GuizhanLibPlugin 的 MaterialHelper / ItemStackHelper /
 * DyeColorHelper / PotionEffectTypeHelper 提供中文名称。
 * 移除 GuizhanLibPlugin 依赖后，此处回退为格式化枚举名（Title Case），
 * 与 GuizhanLib 在缺失翻译条目时的回退行为一致，仅影响展示文本，不影响玩法。
 * <p>
 * Title Case 为枚举实例的纯函数（枚举名不变），以实例级缓存查表消除
 * 每次调用的 StringBuilder 重建 + 分隔符替换（图鉴图标构建每页 36 次）。
 */
@UtilityClass
public class NameUtils {

    private static final Map<Material, String> MATERIAL_NAME_CACHE = new EnumMap<>(Material.class);
    private static final Map<DyeColor, String> DYE_COLOR_NAME_CACHE = new EnumMap<>(DyeColor.class);
    /** PotionEffectType 非 枚举（注册表单例），普通 HashMap 即可 */
    private static final Map<PotionEffectType, String> POTION_EFFECT_NAME_CACHE = new HashMap<>();

    @Nonnull
    public static String getMaterialName(@Nonnull Material material) {
        return MATERIAL_NAME_CACHE.computeIfAbsent(material, m -> TextUtils.toTitleCase(m.name()));
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
        return DYE_COLOR_NAME_CACHE.computeIfAbsent(dyeColor, d -> TextUtils.toTitleCase(d.name()));
    }

    @Nonnull
    public static String getPotionEffectTypeName(@Nonnull PotionEffectType potionEffectType) {
        return POTION_EFFECT_NAME_CACHE.computeIfAbsent(potionEffectType, p -> TextUtils.toTitleCase(p.getName()));
    }
}

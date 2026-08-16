package io.github.sefiraat.crystamaehistoria.utils;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Item;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 掉落物实体 → 已解析 {@link SlimefunItem} 的共享弱缓存。
 * <p>
 * 供每 tick 对同一实体重复解析的路径使用（液化池/镀金器 consumeItems
 * 的区域内物品、展示架 afterTick）——{@code getByItem} 对 Slimefun 物品
 * 为完整 ItemMeta + PDC 读取（~1.3µs）。生命周期闭合：实体存续期内
 * 物品堆不发生影响 SF 身份的变异（数量增减不影响），架位/池位换物
 * 即新实体新键，条目随实体回收自动清理；主线程单线程访问。
 */
@UtilityClass
public class SlimefunItemResolver {

    private final Map<Item, SlimefunItem> CACHE = new WeakHashMap<>();

    @ParametersAreNonnullByDefault
    public static SlimefunItem resolve(Item item) {
        SlimefunItem resolved = CACHE.get(item);
        if (resolved == null && !CACHE.containsKey(item)) {
            resolved = SlimefunItem.getByItem(item.getItemStack());
            CACHE.put(item, resolved);
        }
        return resolved;
    }
}

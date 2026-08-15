package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot;
import io.github.sefiraat.crystamaehistoria.stories.Story;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;

/**
 * A {@link PersistentDataType} for {@link Story}s which uses an
 * {@link Integer} array for storage purposes.
 * Creatively thieved from {@see <a href="https://github.com/baked-libs/dough/blob/main/dough-data/src/main/java/io/github/bakedlibs/dough/data/persistent/PersistentUUIDDataType.java">PersistentUUIDDataType}
 *
 * @author Sfiguz7
 * @author Walshy
 */

public class PersistentStaveDataType implements PersistentDataType<PersistentDataContainer[], Map<SpellSlot, InstancePlate>> {

    public static final PersistentDataType<PersistentDataContainer[], Map<SpellSlot, InstancePlate>> TYPE = new PersistentStaveDataType();

    @Override
    @Nonnull
    public Class<PersistentDataContainer[]> getPrimitiveType() {
        return PersistentDataContainer[].class;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nonnull
    public Class<Map<SpellSlot, InstancePlate>> getComplexType() {
        return (Class<Map<SpellSlot, InstancePlate>>) (Class<?>) Map.class;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public PersistentDataContainer[] toPrimitive(Map<SpellSlot, InstancePlate> complex, PersistentDataAdapterContext context) {
        PersistentDataContainer[] containers = new PersistentDataContainer[complex.size()];
        int i = 0;

        for (Map.Entry<SpellSlot, InstancePlate> spellTypeEntry : complex.entrySet()) {
            PersistentDataContainer container = context.newPersistentDataContainer();
            container.set(Keys.STAVE_SLOT, PersistentDataType.STRING, spellTypeEntry.getKey().toString());
            container.set(Keys.STAVE_PLATE, PersistentPlateDataType.TYPE, spellTypeEntry.getValue());
            containers[i] = container;
            i++;
        }

        return containers;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public Map<SpellSlot, InstancePlate> fromPrimitive(PersistentDataContainer[] primitive, PersistentDataAdapterContext context) {
        Map<SpellSlot, InstancePlate> plateStorageMap = new EnumMap<>(SpellSlot.class);
        for (PersistentDataContainer container : primitive) {
            // PDC 内容不可信：槽位名缺失/非法、法术板数据缺失都会在原实现中抛
            // NPE/IllegalArgumentException（EnumMap.put(null) 亦为 NPE），统一失败关闭
            final String slotName = container.get(Keys.STAVE_SLOT, PersistentDataType.STRING);
            if (slotName == null) {
                throw new IllegalStateException("法杖数据损坏：槽位键缺失");
            }
            final SpellSlot spellSlot;
            try {
                spellSlot = SpellSlot.valueOf(slotName);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("法杖数据损坏：未知槽位 " + slotName, e);
            }
            final InstancePlate instancePlate = container.get(Keys.STAVE_PLATE, PersistentPlateDataType.TYPE);
            if (instancePlate == null) {
                throw new IllegalStateException("法杖数据损坏：槽位 " + slotName + " 缺少法术板数据");
            }
            plateStorageMap.put(spellSlot, instancePlate);
        }
        return plateStorageMap;
    }

    /**
     * 单槽局部读取：只反序列化指定槽位的法术板（施法交互的失败路径——冷却/
     * 缺晶能/空槽——不需要其余槽位数据，免去 3/4 的法术板反序列化）。
     * 损坏守卫与 {@link #fromPrimitive} 同构：目标槽位数据损坏时失败关闭抛出；
     * 其余槽位的槽位名缺失同样视为整杖损坏（与全量读取语义一致）。
     *
     * @return 该槽位的法术板；槽位未绑定时 null
     */
    @ParametersAreNonnullByDefault
    @Nullable
    public static InstancePlate getSlotPlate(org.bukkit.inventory.meta.ItemMeta itemMeta, SpellSlot slot) {
        final PersistentDataContainer[] containers = itemMeta.getPersistentDataContainer()
                                                             .get(Keys.PDC_STAVE_STORAGE, PersistentDataType.TAG_CONTAINER_ARRAY);
        if (containers == null) {
            return null;
        }
        for (PersistentDataContainer container : containers) {
            final String slotName = container.get(Keys.STAVE_SLOT, PersistentDataType.STRING);
            if (slotName == null) {
                throw new IllegalStateException("法杖数据损坏：槽位键缺失");
            }
            if (!slot.name().equals(slotName)) {
                continue;
            }
            final InstancePlate instancePlate = container.get(Keys.STAVE_PLATE, PersistentPlateDataType.TYPE);
            if (instancePlate == null) {
                throw new IllegalStateException("法杖数据损坏：槽位 " + slotName + " 缺少法术板数据");
            }
            return instancePlate;
        }
        return null;
    }
}

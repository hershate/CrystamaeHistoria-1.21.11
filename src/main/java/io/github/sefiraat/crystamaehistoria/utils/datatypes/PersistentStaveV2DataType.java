package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;

/**
 * 法杖存储的 v2 扁平编码：v1（{@link PersistentStaveDataType}）为每板一个
 * 子容器（槽位名 + 板容器内 4 键）——4 板满配一次序列化 24 次 PDC 键操作；
 * v2 归并为单容器 5 键（槽位名/法术 id 各自 NUL 连接 + tier int[] +
 * crysta int[] + cooldown long[]），键操作 24 → 5。
 * <p>
 * **同键双读**：与 v1 共用 {@code Keys.PDC_STAVE_STORAGE}（v2 原语为
 * TAG_CONTAINER，v1 为 TAG_CONTAINER_ARRAY）。读取 v2 类型在 v1 值上抛
 * IAE，经 {@link DataTypeMethods} 断路器返回 null 后回退 v1 读取——
 * 无需第二键、无残留迁移。写入 v2 即覆盖 v1 值。
 * <p>
 * 损坏守卫：长度不匹配/未知槽位/未知法术抛 {@link IllegalStateException}
 * 失败关闭（与 v1 语义一致，调用方 InstanceStave 各工厂已有 ISE 捕获降级）。
 */
public class PersistentStaveV2DataType implements PersistentDataType<PersistentDataContainer, Map<SpellSlot, InstancePlate>> {

    public static final PersistentDataType<PersistentDataContainer, Map<SpellSlot, InstancePlate>> TYPE =
        new PersistentStaveV2DataType();

    private static final char SEPARATOR = '\u0000';

    @Override
    @Nonnull
    public Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
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
    public PersistentDataContainer toPrimitive(Map<SpellSlot, InstancePlate> complex, PersistentDataAdapterContext context) {
        final int n = complex.size();
        final StringBuilder slots = new StringBuilder();
        final StringBuilder spells = new StringBuilder();
        final int[] tiers = new int[n];
        final int[] crystas = new int[n];
        final long[] cooldowns = new long[n];
        int i = 0;
        for (Map.Entry<SpellSlot, InstancePlate> entry : complex.entrySet()) {
            if (i > 0) {
                slots.append(SEPARATOR);
                spells.append(SEPARATOR);
            }
            slots.append(entry.getKey().name());
            spells.append(entry.getValue().getStoredSpell().name());
            tiers[i] = entry.getValue().getTier();
            crystas[i] = entry.getValue().getCrysta();
            cooldowns[i] = entry.getValue().getCooldown();
            i++;
        }
        final PersistentDataContainer container = context.newPersistentDataContainer();
        container.set(Keys.STAVE_SLOTS_JOINED, PersistentDataType.STRING, slots.toString());
        container.set(Keys.STAVE_SPELLS_JOINED, PersistentDataType.STRING, spells.toString());
        container.set(Keys.STAVE_TIERS, PersistentDataType.INTEGER_ARRAY, tiers);
        container.set(Keys.STAVE_CRYSTAS, PersistentDataType.INTEGER_ARRAY, crystas);
        container.set(Keys.STAVE_COOLDOWNS, PersistentDataType.LONG_ARRAY, cooldowns);
        return container;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public Map<SpellSlot, InstancePlate> fromPrimitive(PersistentDataContainer primitive, PersistentDataAdapterContext context) {
        final String slotsJoined = primitive.get(Keys.STAVE_SLOTS_JOINED, PersistentDataType.STRING);
        final String spellsJoined = primitive.get(Keys.STAVE_SPELLS_JOINED, PersistentDataType.STRING);
        final int[] tiers = primitive.get(Keys.STAVE_TIERS, PersistentDataType.INTEGER_ARRAY);
        final int[] crystas = primitive.get(Keys.STAVE_CRYSTAS, PersistentDataType.INTEGER_ARRAY);
        final long[] cooldowns = primitive.get(Keys.STAVE_COOLDOWNS, PersistentDataType.LONG_ARRAY);
        if (slotsJoined == null || spellsJoined == null || tiers == null || crystas == null || cooldowns == null) {
            throw new IllegalStateException("法杖 v2 数据损坏：键缺失");
        }
        final Map<SpellSlot, InstancePlate> map = new EnumMap<>(SpellSlot.class);
        if (slotsJoined.isEmpty()) {
            if (tiers.length != 0 || crystas.length != 0 || cooldowns.length != 0
                || !spellsJoined.isEmpty()) {
                throw new IllegalStateException("法杖 v2 数据损坏：空槽位但数组非空");
            }
            return map;
        }
        final String[] slotNames = split(slotsJoined);
        final String[] spellNames = split(spellsJoined);
        if (slotNames.length != spellNames.length || slotNames.length != tiers.length
            || tiers.length != crystas.length || crystas.length != cooldowns.length) {
            throw new IllegalStateException("法杖 v2 数据损坏：数组长度不匹配");
        }
        for (int i = 0; i < slotNames.length; i++) {
            final SpellSlot slot;
            final SpellType spell;
            try {
                slot = SpellSlot.valueOf(slotNames[i]);
                spell = SpellType.valueOf(spellNames[i]);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("法杖 v2 数据损坏：未知槽位/法术", e);
            }
            final InstancePlate plate = new InstancePlate(tiers[i], spell, crystas[i]);
            plate.setCooldown(cooldowns[i]);
            map.put(slot, plate);
        }
        return map;
    }

    /**
     * 同键双读的统一读取入口：v2（TAG_CONTAINER）优先——v1 值上 v2 类型
     * 读取抛 IAE 被 DataTypeMethods 断路为 null 后回退 v1（数组）。
     * v2 结构损坏抛 ISE 由调用方既有捕获降级（不吞掉，防覆盖性丢失）。
     */
    @Nullable
    @ParametersAreNonnullByDefault
    public static Map<SpellSlot, InstancePlate> readStaveMap(@Nullable ItemMeta itemMeta) {
        if (itemMeta == null) {
            return null;
        }
        final Map<SpellSlot, InstancePlate> v2 = DataTypeMethods.getCustom(itemMeta, Keys.PDC_STAVE_STORAGE, TYPE);
        if (v2 != null) {
            return v2;
        }
        return DataTypeMethods.getCustom(itemMeta, Keys.PDC_STAVE_STORAGE, PersistentStaveDataType.TYPE);
    }

    /** 统一写入入口（v2 覆盖同键 v1 值，自动迁移） */
    @ParametersAreNonnullByDefault
    public static void writeStaveMap(ItemMeta itemMeta, Map<SpellSlot, InstancePlate> map) {
        DataTypeMethods.setCustom(itemMeta, Keys.PDC_STAVE_STORAGE, TYPE, map);
    }

    /**
     * 单槽局部读取（施法失败前置路径）：v2 扁平编码下解析槽位串定位索引，
     * 仅构建目标槽位板。v1 值回退 {@link PersistentStaveDataType#getSlotPlate}。
     */
    @Nullable
    @ParametersAreNonnullByDefault
    public static InstancePlate readSlotPlate(@Nullable ItemMeta itemMeta, SpellSlot slot) {
        if (itemMeta == null) {
            return null;
        }
        PersistentDataContainer container = null;
        try {
            container = itemMeta.getPersistentDataContainer()
                .get(Keys.PDC_STAVE_STORAGE, PersistentDataType.TAG_CONTAINER);
        } catch (IllegalArgumentException e) {
            // 值非容器类型（v1 的 ListTag 或 crafted 错误类型）：回退 v1 单槽读取
            //（Paper 对类型不匹配抛裸 IAE，必须显式防御——全量读取路径经
            // DataTypeMethods 断路器天然安全，本路径为裸 PDC 调用）
        }
        if (container == null) {
            // v1（数组）或无数据：回退 v1 单槽读取（含其损坏守卫语义）
            return PersistentStaveDataType.getSlotPlate(itemMeta, slot);
        }
        final String slotsJoined = container.get(Keys.STAVE_SLOTS_JOINED, PersistentDataType.STRING);
        final String spellsJoined = container.get(Keys.STAVE_SPELLS_JOINED, PersistentDataType.STRING);
        final int[] tiers = container.get(Keys.STAVE_TIERS, PersistentDataType.INTEGER_ARRAY);
        final int[] crystas = container.get(Keys.STAVE_CRYSTAS, PersistentDataType.INTEGER_ARRAY);
        final long[] cooldowns = container.get(Keys.STAVE_COOLDOWNS, PersistentDataType.LONG_ARRAY);
        if (slotsJoined == null || spellsJoined == null || tiers == null || crystas == null || cooldowns == null) {
            throw new IllegalStateException("法杖 v2 数据损坏：键缺失");
        }
        if (slotsJoined.isEmpty()) {
            return null;
        }
        final String[] slotNames = split(slotsJoined);
        final String[] spellNames = split(spellsJoined);
        int index = -1;
        for (int i = 0; i < slotNames.length; i++) {
            if (slot.name().equals(slotNames[i])) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return null;
        }
        if (slotNames.length != spellNames.length || index >= tiers.length
            || tiers.length != crystas.length || crystas.length != cooldowns.length) {
            throw new IllegalStateException("法杖 v2 数据损坏：数组长度不匹配");
        }
        final SpellType spell;
        try {
            spell = SpellType.valueOf(spellNames[index]);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("法杖 v2 数据损坏：未知法术 " + spellNames[index], e);
        }
        final InstancePlate plate = new InstancePlate(tiers[index], spell, crystas[index]);
        plate.setCooldown(cooldowns[index]);
        return plate;
    }

    private static String[] split(String joined) {
        final java.util.List<String> parts = new java.util.ArrayList<>();
        final int len = joined.length();
        int start = 0;
        for (int i = 0; i < len; i++) {
            if (joined.charAt(i) == SEPARATOR) {
                parts.add(joined.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(joined.substring(start));
        return parts.toArray(new String[0]);
    }
}

package io.github.sefiraat.crystamaehistoria.magic.spells.core;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.CastResult;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.TextUtils;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class InstanceStave {

    /**
     * 法杖 lore 的静态片段（主题色与文案均不可变）：每次成功施法写回都会重建 lore，
     * 头部/尾部/标签前缀预构建为常量，免每次施法的重复字符串拼接与主题解析。
     */
    private static final String LORE_HEADER =
        ThemeType.PASSIVE.getColor() + "可以进行法术绑定的法杖";
    private static final String LORE_FOOTER =
        ThemeType.applyThemeToString(ThemeType.CLICK_INFO, ThemeType.STAVE.getLoreLine());
    private static final String LORE_SPELL_LABEL =
        ThemeType.PASSIVE.getColor() + "法术: " + ThemeType.NOTICE.getColor();
    private static final String LORE_CRYSTA_LABEL =
        ThemeType.PASSIVE.getColor() + "充能: " + ThemeType.NOTICE.getColor();
    /** 槽位标签前缀（稀有度色 + 槽位描述），按 SpellSlot.ordinal 索引 */
    private static final String[] SLOT_LABELS = buildSlotLabels();

    private static String[] buildSlotLabels() {
        final String[] labels = new String[SpellSlot.getCashedValues().length];
        for (int i = 0; i < labels.length; i++) {
            final SpellSlot slot = SpellSlot.getCashedValues()[i];
            labels[i] = ThemeType.RARITY_MYTHICAL.getColor() + slot.getDescription();
        }
        return labels;
    }

    @Getter
    public final ItemStack itemStack;

    @Getter
    private final Map<SpellSlot, InstancePlate> spellInstanceMap = new EnumMap<>(SpellSlot.class);

    public InstanceStave(@Nonnull ItemStack itemStack) {
        this(itemStack, itemStack.getItemMeta());
    }

    /**
     * 以调用方持有的 meta 快照做全量反序列化（整个交互只克隆一次元数据时使用）。
     */
    public InstanceStave(@Nonnull ItemStack itemStack, @Nullable org.bukkit.inventory.meta.ItemMeta itemMeta) {
        this.itemStack = itemStack;
        if (itemMeta == null) {
            return;
        }
        final Map<SpellSlot, InstancePlate> map;
        try {
            map = DataTypeMethods.getCustom(
                itemMeta,
                Keys.PDC_STAVE_STORAGE,
                PersistentStaveDataType.TYPE
            );
        } catch (IllegalStateException e) {
            // 物品 PDC 不可信（作弊/损坏数据）：反序列化失败时按空法杖失败关闭，
            // 避免异常穿透到施法事件链
            CrystamaeHistoria.getInstance().getLogger().warning("法杖 PDC 数据损坏，按空法杖处理: " + e.getMessage());
            return;
        }
        if (map != null) {
            spellInstanceMap.putAll(map);
        }
    }

    private InstanceStave(@Nonnull ItemStack itemStack, boolean skipRead) {
        this.itemStack = itemStack;
    }

    /**
     * 单槽工厂：只反序列化给定槽位的法术板（其余槽位不进入映射）。
     * 用于施法交互的失败前置路径（冷却/缺晶能/空槽），免去 3/4 的法术板反序列化；
     * 不可用于需要写回全部栏位的场合（写回用 {@link #forWriteBack}）。
     * 接受调用方持有的 meta 快照，整个交互（前置读取 + 成功写回）只克隆一次。
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static InstanceStave forSlot(ItemStack itemStack, SpellSlot slot, org.bukkit.inventory.meta.ItemMeta itemMeta) {
        final InstanceStave stave = new InstanceStave(itemStack, false);
        final InstancePlate plate;
        try {
            plate = PersistentStaveDataType.getSlotPlate(itemMeta, slot);
        } catch (IllegalStateException e) {
            CrystamaeHistoria.getInstance().getLogger().warning("法杖 PDC 数据损坏，按空法杖处理: " + e.getMessage());
            return stave;
        }
        if (plate != null) {
            stave.spellInstanceMap.put(slot, plate);
        }
        return stave;
    }

    /**
     * 成功施法后的全量写回对象：全量重读并合并指定槽位已扣减的法术板。
     * 复用调用方在施法前获取的 meta 快照（主线程单线程；法术回调不触碰施法者
     * 手持物品的元数据——各法术仅作用于世界/实体）。
     * 全量重读失败（PDC 损坏）时返回 null——调用方跳过写回保持原数据，
     * 绝不能以空映射覆写法杖（会清掉其余槽位）。
     */
    @Nullable
    @ParametersAreNonnullByDefault
    public static InstanceStave forWriteBack(ItemStack itemStack, SpellSlot slot, InstancePlate mutatedPlate,
                                              org.bukkit.inventory.meta.ItemMeta itemMeta) {
        final Map<SpellSlot, InstancePlate> map;
        try {
            map = DataTypeMethods.getCustom(
                itemMeta,
                Keys.PDC_STAVE_STORAGE,
                PersistentStaveDataType.TYPE
            );
        } catch (IllegalStateException e) {
            CrystamaeHistoria.getInstance().getLogger().warning("法杖 PDC 数据损坏，跳过写回（保持原数据）: " + e.getMessage());
            return null;
        }
        final InstanceStave stave = new InstanceStave(itemStack, false);
        if (map != null) {
            stave.spellInstanceMap.putAll(map);
        }
        stave.spellInstanceMap.put(slot, mutatedPlate);
        return stave;
    }

    public void buildLore() {
        final ItemMeta itemMeta = this.itemStack.getItemMeta();
        buildLore(itemMeta);
        this.itemStack.setItemMeta(itemMeta);
    }

    /**
     * 将法术栏位 lore 写入给定 meta（不触发 getItemMeta/setItemMeta 往返）。
     * 与其他 meta 修改（如 PDC 写回）共用同一次往返，省一次 ItemMeta 克隆与应用。
     * 静态片段（头部/尾部/标签前缀/槽位标签）取类常量，仅晶能数字逐次拼接。
     */
    @ParametersAreNonnullByDefault
    public void buildLore(ItemMeta itemMeta) {
        final List<String> finalLore = new ArrayList<>();
        finalLore.add(LORE_HEADER);

        final SpellSlot[] slots = SpellSlot.getCashedValues();
        for (int i = 0; i < slots.length; i++) {
            final InstancePlate instancePlate = this.spellInstanceMap.get(slots[i]);
            if (instancePlate != null) {
                final String magic = instancePlate.getStoredSpell().getSpell().getName();
                final String crysta = String.valueOf(instancePlate.getCrysta());
                finalLore.add("");
                finalLore.add(SLOT_LABELS[i]);
                finalLore.add(LORE_SPELL_LABEL + magic);
                finalLore.add(LORE_CRYSTA_LABEL + crysta);
            }
        }
        finalLore.add("");
        finalLore.add(LORE_FOOTER);
        itemMeta.setLore(finalLore);
    }

    @ParametersAreNonnullByDefault
    public void setSlot(SpellSlot spellSlot, InstancePlate instancePlate) {
        spellInstanceMap.put(spellSlot, instancePlate);
    }

    @ParametersAreNonnullByDefault
    public CastResult tryCastSpell(SpellSlot slot, CastInformation castInformation) {
        InstancePlate instancePlate = spellInstanceMap.get(slot);
        if (instancePlate != null) {
            return instancePlate.tryCastSpell(castInformation);
        } else {
            return CastResult.CAST_FAIL_SLOT_EMPTY;
        }
    }
}

package bench;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * 基准（第 24 轮）：ItemMeta 展示写入的 String→Component 转换成本
 *
 * Paper 的 setLore(List<String>) / setDisplayName(String) 在应用时立即做
 * legacy→Component 转换（ItemMeta 为服务器运行时对象，无法脱离服务器构造；
 * 此处测其内部使用的同一 LegacyComponentSerializer.legacySection() 纯库路径，
 * 转换成本即两路径的差值主体）。
 *
 * - 旧：每次应用逐行反序列化（法杖 lore ~20 行/次施法；故事提交 N×M 行/次）
 * - 新：组件常量/缓存引用 + 列表组装（仅晶能行 1 次转换）
 */
public final class BenchLoreComponents {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /** 法杖 lore 形态（4 板满配）：头部 + 4×(空行/槽位标签/法术行/晶能行) + 空行 + 尾部 */
    private static final String[] STAVE_LINES = {
        "§7可以进行法术绑定的法杖",
        "",
        "§d左键点击",
        "§7法术: §b火焰球",
        "§7充能: §b8",
        "",
        "§d右键点击",
        "§7法术: §b治疗之雾",
        "§7充能: §b12",
        "",
        "§dShift+左键点击",
        "§7法术: §b陨石",
        "§7充能: §b5",
        "",
        "§dShift+右键点击",
        "§7法术: §b真空",
        "§7充能: §b3",
        "",
        "§b法杖"
    };

    /** 故事物品 lore 形态（3 故事 × ~5 行） */
    private static final String[] STORY_LINES = {
        "",
        "§6[史诗] 被遗忘的王国",
        "§7曾经繁荣的王国如今只剩下",
        "§7断壁残垣与风中的低语。",
        "",
        "§3[稀有] 矿工的遗物",
        "§7这把镐子的主人再也没有",
        "§7从深处的矿道回来。",
        "",
        "§f[普通] 平凡的一天",
        "§7无事发生的一天，也是",
        "§7值得记录的一天。"
    };

    public static void run(Harness h) {
        final Component[] staveComponents = new Component[STAVE_LINES.length];
        for (int i = 0; i < STAVE_LINES.length; i++) {
            staveComponents[i] = LEGACY.deserialize(STAVE_LINES[i]);
        }
        final Component[] storyComponents = new Component[STORY_LINES.length];
        for (int i = 0; i < STORY_LINES.length; i++) {
            storyComponents[i] = LEGACY.deserialize(STORY_LINES[i]);
        }

        // —— 法杖 lore（20 行）：逐行转换 vs 缓存引用组装（仅晶能行 1 次转换） ——
        h.bench("loreConvert.stave20", "old_deserialize_per_line", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final List<Component> lore = new ArrayList<>(STAVE_LINES.length);
                for (String line : STAVE_LINES) {
                    lore.add(LEGACY.deserialize(line));
                }
                bh += lore.size();
            }
            return bh;
        }, 3000, 20, 20_000);

        h.bench("loreConvert.stave20", "new_cached_refs_one_convert", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final List<Component> lore = new ArrayList<>(staveComponents.length);
                for (Component c : staveComponents) {
                    lore.add(c);
                }
                // 晶能行逐次转换（数值每次施法变化）
                lore.set(4, LEGACY.deserialize("§7充能: §b" + (i & 15)));
                bh += lore.size();
            }
            return bh;
        }, 3000, 20, 200_000);

        // —— 故事 lore（12 行）：逐行转换 vs 缓存引用 ——
        h.bench("loreConvert.story12", "old_deserialize_per_line", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final List<Component> lore = new ArrayList<>(STORY_LINES.length);
                for (String line : STORY_LINES) {
                    lore.add(LEGACY.deserialize(line));
                }
                bh += lore.size();
            }
            return bh;
        }, 3000, 20, 20_000);

        h.bench("loreConvert.story12", "new_cached_refs", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final List<Component> lore = new ArrayList<>(storyComponents.length);
                for (Component c : storyComponents) {
                    lore.add(c);
                }
                bh += lore.size();
            }
            return bh;
        }, 3000, 20, 500_000);

        // —— 等价性断言：同输入两路径产物逐值等价 ——
        boolean equiv = true;
        for (int i = 0; i < STAVE_LINES.length; i++) {
            equiv &= LEGACY.deserialize(STAVE_LINES[i]).equals(staveComponents[i]);
        }
        // 空行共享常量与逐次反序列化等价
        equiv &= LEGACY.deserialize("").equals(staveComponents[1]);
        System.err.println("round24.loreComponentEquivalence=" + equiv);
    }
}

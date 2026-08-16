package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.stories.Story;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 故事列表的 v2 瘦编码：v1（{@link PersistentStoriesDataType}）为 N 个子容器
 * × 2 键（每故事 2 次 PDC 写/读 + 1 次容器分配）；v2 归并为单容器 2 键
 * （id 以 NUL 连接的单一字符串 + 稀有度 int[]），每次提交/提取的
 * PDC 操作数从 2N 降为 2。
 * <p>
 * 读取 v2 优先（{@code Keys.PDC_STORIES_V2}），结构损坏（缺键/长度不匹配）
 * 时由 {@link #fromPrimitive} 抛 {@link IllegalStateException}，调用方
 * （StoryUtils.readStories）降级回退 v1——与 v1 对 crafted 物品逐条目
 * 跳过的优雅降级同级，不产生 tick 异常。
 * <p>
 * 信息量与 v1 完全一致（id + 稀有度 → 全局故事池实例解析），不引入
 * 新语义；旧物品的 v1 键保持可读（旧存档兼容），新写入仅落 v2 并移除
 * 残留 v1 键。
 */
public class PersistentStoriesV2DataType implements PersistentDataType<PersistentDataContainer, List<Story>> {

    public static final PersistentDataType<PersistentDataContainer, List<Story>> TYPE = new PersistentStoriesV2DataType();

    /** id 连接分隔符：配置故事名（YAML 字符串）不含 NUL；写入侧守卫见 toPrimitive */
    private static final char SEPARATOR = '\u0000';

    /** 结构损坏告警限次（crafted 物品逐 tick 读取路径防日志风暴） */
    private static final AtomicBoolean CORRUPT_WARNED = new AtomicBoolean();

    @Override
    @Nonnull
    public Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nonnull
    public Class<List<Story>> getComplexType() {
        return (Class<List<Story>>) (Class<?>) List.class;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public PersistentDataContainer toPrimitive(List<Story> complex, PersistentDataAdapterContext context) {
        // 守卫：id 含 NUL 将破坏连接编码——正常配置不可能，crafted 调用方直接拒绝
        final StringBuilder ids = new StringBuilder();
        final int[] rarities = new int[complex.size()];
        int i = 0;
        for (Story story : complex) {
            final String id = story.getId();
            if (id.indexOf(SEPARATOR) >= 0) {
                throw new IllegalStateException("故事 id 含非法字符，拒绝编码: " + id);
            }
            if (i > 0) {
                ids.append(SEPARATOR);
            }
            ids.append(id);
            rarities[i++] = story.getRarity().getId();
        }
        final PersistentDataContainer container = context.newPersistentDataContainer();
        container.set(Keys.STORY_IDS_JOINED, PersistentDataType.STRING, ids.toString());
        container.set(Keys.STORY_RARITIES, PersistentDataType.INTEGER_ARRAY, rarities);
        return container;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<Story> fromPrimitive(PersistentDataContainer primitive, PersistentDataAdapterContext context) {
        final String joined = primitive.get(Keys.STORY_IDS_JOINED, PersistentDataType.STRING);
        final int[] rarities = primitive.get(Keys.STORY_RARITIES, PersistentDataType.INTEGER_ARRAY);
        // 结构损坏（crafted/篡改）：交由调用方降级 v1，不在此静默丢弃（防覆盖性丢失）
        if (joined == null || rarities == null) {
            warnCorrupt("v2 故事键缺失");
            throw new IllegalStateException("v2 故事数据结构损坏：键缺失");
        }
        final List<Story> list;
        if (joined.isEmpty()) {
            if (rarities.length != 0) {
                warnCorrupt("v2 空故事串长度不匹配");
                throw new IllegalStateException("v2 故事数据结构损坏：长度不匹配");
            }
            list = new ArrayList<>(0);
        } else {
            final String[] ids = split(joined);
            if (ids.length != rarities.length) {
                warnCorrupt("v2 故事串长度不匹配");
                throw new IllegalStateException("v2 故事数据结构损坏：长度不匹配");
            }
            list = new ArrayList<>(ids.length);
            for (int i = 0; i < ids.length; i++) {
                // 逐条目失败关闭（缺池/非法稀有度跳过）——与 v1 语义一致
                final StoryRarity rarity = StoryRarity.getById(rarities[i]);
                if (rarity == null) {
                    continue;
                }
                final Story story = CrystamaeHistoria.getStoriesManager().getStory(ids[i], rarity);
                if (story != null) {
                    list.add(story);
                }
            }
        }
        return list;
    }

    private static String[] split(String joined) {
        final List<String> parts = new ArrayList<>();
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

    private static void warnCorrupt(String detail) {
        if (CORRUPT_WARNED.compareAndSet(false, true)) {
            CrystamaeHistoria.getInstance().getLogger().warning(
                "检测到损坏的 v2 故事数据（" + detail + "），本次会话仅告警一次；将按 v1/无数据处理");
        }
    }
}

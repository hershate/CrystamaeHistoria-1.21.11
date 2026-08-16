package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.stories.Story;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 区块晶簇故事状态的 v2 扁平编码：v1（{@link PersistentStoryChunkDataType}）
 * 为每故事一个子容器 × 5 键（id/稀有度/位置/世界/镀金）——N 晶簇一次
 * saveMap（**每次成功提取步骤触发**）5N 次 PDC 写；v2 归并为单容器 5 键
 * （ids 连接串 + 稀有度 int[] + 位置 long[] + 镀金打包串 + 共享世界 UUID
 * ——同一区块键下位置必然同世界）。
 * <p>
 * 同键双读：与 v1 共用调用方传入的键（v2 原语 TAG_CONTAINER，v1 为数组），
 * v2 类型读 v1 值抛 IAE 经 {@link DataTypeMethods} 断路转 null 后回退 v1；
 * 写 v2 即覆盖。结构损坏（缺键/长度不匹配/世界异常）限次告警并按空列表
 * 降级（读取方为区块加载期无捕获路径，不容异常；条目级字段损坏仍逐条目
 * 跳过，与 v1 语义一致）。
 */
public class PersistentStoryChunkV2DataType implements PersistentDataType<PersistentDataContainer, List<Story>> {

    public static final PersistentDataType<PersistentDataContainer, List<Story>> TYPE =
        new PersistentStoryChunkV2DataType();

    private static final char SEPARATOR = '\u0000';

    /** 结构损坏告警限次（防 crafted 数据逐 tick 读取的日志风暴） */
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
        final int n = complex.size();
        final StringBuilder ids = new StringBuilder();
        final int[] rarities = new int[n];
        final long[] positions = new long[n];
        final StringBuilder gilded = new StringBuilder(n);
        World world = null;
        int i = 0;
        for (Story story : complex) {
            if (story.getBlockPosition() == null) {
                throw new IllegalStateException("区块故事缺少方块位置，拒绝编码");
            }
            if (i > 0) {
                ids.append(SEPARATOR);
            }
            ids.append(story.getId());
            rarities[i] = story.getRarity().getId();
            positions[i] = story.getBlockPosition().getPosition();
            gilded.append(story.isGilded() ? '1' : '0');
            if (world == null) {
                world = story.getBlockPosition().getWorld();
            }
            i++;
        }
        final PersistentDataContainer container = context.newPersistentDataContainer();
        container.set(Keys.CHUNK_STORY_IDS, PersistentDataType.STRING, ids.toString());
        container.set(Keys.CHUNK_STORY_RARITIES, PersistentDataType.INTEGER_ARRAY, rarities);
        container.set(Keys.CHUNK_STORY_POSITIONS, PersistentDataType.LONG_ARRAY, positions);
        container.set(Keys.CHUNK_STORY_GILDED, PersistentDataType.STRING, gilded.toString());
        if (world != null) {
            container.set(Keys.CHUNK_STORY_WORLD, PersistentUUIDDataType.TYPE, world.getUID());
        }
        return container;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<Story> fromPrimitive(PersistentDataContainer primitive, PersistentDataAdapterContext context) {
        final String idsJoined = primitive.get(Keys.CHUNK_STORY_IDS, PersistentDataType.STRING);
        final int[] rarities = primitive.get(Keys.CHUNK_STORY_RARITIES, PersistentDataType.INTEGER_ARRAY);
        final long[] positions = primitive.get(Keys.CHUNK_STORY_POSITIONS, PersistentDataType.LONG_ARRAY);
        final String gilded = primitive.get(Keys.CHUNK_STORY_GILDED, PersistentDataType.STRING);
        if (idsJoined == null && rarities == null && positions == null && gilded == null) {
            // 空状态（无世界键、全空）合法
            if (primitive.get(Keys.CHUNK_STORY_WORLD, PersistentUUIDDataType.TYPE) == null) {
                return new ArrayList<>(0);
            }
        }
        // 结构损坏（crafted/编辑）：限次告警并按空列表降级——读取方（loadMap，
        // 区块加载期无捕获）不容异常；磁盘值不被读取破坏，合法数据不受影响
        if (idsJoined == null || rarities == null || positions == null || gilded == null) {
            warnCorrupt("键缺失");
            return new ArrayList<>(0);
        }
        final int n = rarities.length;
        if (positions.length != n || gilded.length() != n
            || (idsJoined.isEmpty() ? 0 : split(idsJoined).length) != n) {
            warnCorrupt("长度不匹配");
            return new ArrayList<>(0);
        }
        final String[] ids = idsJoined.isEmpty() ? new String[0] : split(idsJoined);
        final UUID worldUuid = primitive.get(Keys.CHUNK_STORY_WORLD, PersistentUUIDDataType.TYPE);
        final List<Story> list = new ArrayList<>(n);
        if (n == 0) {
            return list;
        }
        if (worldUuid == null) {
            warnCorrupt("世界键缺失");
            return new ArrayList<>(0);
        }
        final World world = Bukkit.getWorld(worldUuid);
        if (world == null) {
            // 世界未加载（跨世界 crafted 数据；祭坛自身区块加载则其世界必已加载）：
            // 按空降级，磁盘值保留待下次区块加载
            warnCorrupt("世界未加载");
            return new ArrayList<>(0);
        }
        for (int i = 0; i < n; i++) {
            // 条目级失败关闭（非法稀有度/缺池跳过）——与 v1 语义一致
            final StoryRarity rarity = StoryRarity.getById(rarities[i]);
            if (rarity == null) {
                continue;
            }
            final Story source = CrystamaeHistoria.getStoriesManager().getStory(ids[i], rarity);
            if (source == null) {
                continue;
            }
            final Story story = source.copy();
            story.setBlockPosition(new BlockPosition(world, positions[i]));
            story.setGilded(gilded.charAt(i) == '1');
            list.add(story);
        }
        return list;
    }

    /**
     * 同键双读统一入口：v2（TAG_CONTAINER）优先，v1 值回退
     * {@link PersistentStoryChunkDataType}。
     */
    @Nullable
    @ParametersAreNonnullByDefault
    public static List<Story> readChunkStories(PersistentDataHolder holder, NamespacedKey key) {
        final List<Story> v2 = DataTypeMethods.getCustom(holder, key, TYPE);
        if (v2 != null) {
            return v2;
        }
        return DataTypeMethods.getCustom(holder, key, PersistentStoryChunkDataType.TYPE);
    }

    /** 统一写入入口（v2 覆盖同键 v1 值，自动迁移） */
    @ParametersAreNonnullByDefault
    public static void writeChunkStories(PersistentDataHolder holder, NamespacedKey key, List<Story> stories) {
        DataTypeMethods.setCustom(holder, key, TYPE, stories);
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
                "检测到损坏的区块故事 v2 数据（" + detail + "），本次会话仅告警一次");
        }
    }
}

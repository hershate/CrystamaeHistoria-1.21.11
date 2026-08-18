package io.github.sefiraat.crystamaehistoria.managers;

import com.google.common.base.Preconditions;
import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.stories.BlockDefinition;
import io.github.sefiraat.crystamaehistoria.stories.BlockTier;
import io.github.sefiraat.crystamaehistoria.stories.Story;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryChances;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.utils.StoryUtils;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import lombok.Getter;
import io.github.sefiraat.crystamaehistoria.utils.NameUtils;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class StoriesManager {

    @Getter
    private final Map<Material, BlockDefinition> blockDefinitionMap = new EnumMap<>(Material.class);
    @Getter
    private final Map<Integer, BlockTier> blockTierMap = new HashMap<>();
    @Getter
    private final Map<String, Story> storyMapCommon = new HashMap<>();
    @Getter
    private final Map<String, Story> storyMapUncommon = new HashMap<>();
    @Getter
    private final Map<String, Story> storyMapRare = new HashMap<>();
    @Getter
    private final Map<String, Story> storyMapEpic = new HashMap<>();
    @Getter
    private final Map<String, Story> storyMapMythical = new HashMap<>();
    @Getter
    private final Map<String, Story> storyMapUnique = new HashMap<>();
    /**
     * 稀有度×类型故事索引：原 addStory 每次记录故事时对整稀有度故事表做
     * stream 过滤 + 收集（每次分配中间列表）。启动期一次构建，运行期 O(1) 查表。
     * UNIQUE 不参与（独特故事走 BlockDefinition.getUnique()）。
     */
    @Getter
    private final Map<StoryRarity, Map<StoryType, List<Story>>> storiesByRarityAndType = new EnumMap<>(StoryRarity.class);
    /**
     * 按材质名排序的方块定义快照：图鉴（故事集/镀金集）每次翻页原为
     * 复制全表 + 排序（O(n log n) 字符串比较器）；blockDefinitionMap 启动后
     * 不可变，故启动期一次排序，翻页直接 subList。
     */
    @Getter
    private final List<BlockDefinition> blockDefinitionsSortedByMaterial = new ArrayList<>();

    public StoriesManager() {
        fillBlockTierMap();
        fillStories();
        buildStoryTypeIndex();
        fillBlockDefinitions();
        buildSortedBlockDefinitions();
    }

    private void buildSortedBlockDefinitions() {
        blockDefinitionsSortedByMaterial.addAll(blockDefinitionMap.values());
        blockDefinitionsSortedByMaterial.sort(Comparator.comparing(definition -> definition.getMaterial().name()));
    }

    private void buildStoryTypeIndex() {
        storiesByRarityAndType.put(StoryRarity.COMMON, groupStoriesByType(storyMapCommon));
        storiesByRarityAndType.put(StoryRarity.UNCOMMON, groupStoriesByType(storyMapUncommon));
        storiesByRarityAndType.put(StoryRarity.RARE, groupStoriesByType(storyMapRare));
        storiesByRarityAndType.put(StoryRarity.EPIC, groupStoriesByType(storyMapEpic));
        storiesByRarityAndType.put(StoryRarity.MYTHICAL, groupStoriesByType(storyMapMythical));
    }

    @ParametersAreNonnullByDefault
    private Map<StoryType, List<Story>> groupStoriesByType(Map<String, Story> storyMap) {
        final Map<StoryType, List<Story>> index = new EnumMap<>(StoryType.class);
        for (Story story : storyMap.values()) {
            index.computeIfAbsent(story.getType(), k -> new ArrayList<>()).add(story);
        }
        return index;
    }

    /**
     * 按稀有度与类型取可选故事列表（addStory 用）。
     *
     * @return 对应列表；该类型无故事时 null（调用方按空池跳过）
     */
    @ParametersAreNonnullByDefault
    public List<Story> getStories(StoryRarity rarity, StoryType type) {
        final Map<StoryType, List<Story>> byType = storiesByRarityAndType.get(rarity);
        return byType != null ? byType.get(type) : null;
    }

    private void fillBlockTierMap() {
        blockTierMap.put(
            1,
            new BlockTier(
                1,
                700,
                3,
                1,
                new StoryChances(
                    85,
                    15,
                    0,
                    0,
                    0
                )
            )
        );
        blockTierMap.put(
            2,
            new BlockTier(
                2,
                600,
                3,
                2,
                new StoryChances(
                    70,
                    25,
                    5,
                    0,
                    0
                )
            )
        );
        blockTierMap.put(
            3,
            new BlockTier(
                3,
                500,
                4,
                2,
                new StoryChances(
                    50,
                    35,
                    10,
                    5,
                    0
                )
            )
        );
        blockTierMap.put(
            4,
            new BlockTier(
                4,
                400,
                5,
                3,
                new StoryChances(
                    25,
                    40,
                    20,
                    10,
                    5
                )
            )
        );
        blockTierMap.put(
            5,
            new BlockTier(
                5,
                300,
                5,
                4,
                new StoryChances(
                    5,
                    30,
                    30,
                    20,
                    15
                )
            )
        );
    }

    private void fillStories() {
        FileConfiguration stories = CrystamaeHistoria.getConfigManager().getStories();

        ConfigurationSection common = stories.getConfigurationSection("COMMON");
        Preconditions.checkNotNull(common, "Common story configuration is not found, changed or deleted.");
        fillMap(storyMapCommon, common, StoryRarity.COMMON);

        ConfigurationSection uncommon = stories.getConfigurationSection("UNCOMMON");
        Preconditions.checkNotNull(uncommon, "Uncommon story configuration is not found, changed or deleted.");
        fillMap(storyMapUncommon, uncommon, StoryRarity.UNCOMMON);

        ConfigurationSection rare = stories.getConfigurationSection("RARE");
        Preconditions.checkNotNull(rare, "Rare story configuration is not found, changed or deleted.");
        fillMap(storyMapRare, rare, StoryRarity.RARE);

        ConfigurationSection epic = stories.getConfigurationSection("EPIC");
        Preconditions.checkNotNull(epic, "Epic story configuration is not found, changed or deleted.");
        fillMap(storyMapEpic, epic, StoryRarity.EPIC);

        ConfigurationSection mythical = stories.getConfigurationSection("MYTHICAL");
        Preconditions.checkNotNull(mythical, "Mythical story configuration is not found, changed or deleted.");
        fillMap(storyMapMythical, mythical, StoryRarity.MYTHICAL);
    }

    private void fillBlockDefinitions() {
        final FileConfiguration blocks = CrystamaeHistoria.getConfigManager().getBlocks();
        for (String key : blocks.getKeys(false)) {
            final ConfigurationSection wholeSection = blocks.getConfigurationSection(key);

            if (wholeSection == null) {
                CrystamaeHistoria.getInstance().getLogger().info(
                    MessageFormat.format("Whole section missing for story -> {0}", key)
                );
                continue;
            }

            final ConfigurationSection storySection = wholeSection.getConfigurationSection("story");

            if (storySection == null) {
                CrystamaeHistoria.getInstance().getLogger().info(
                    MessageFormat.format("Ignoring a block with a missing story section -> {0}", key)
                );
                continue;
            }

            final String name = storySection.getString("name");
            final Material material = Material.getMaterial(key);

            if (name == null) {
                CrystamaeHistoria.getInstance().getLogger().info(
                    MessageFormat.format("Ignoring a story without a name -> {0}", key)
                );
                continue;
            }

            if (material == null) {
                CrystamaeHistoria.getInstance().getLogger().info(
                    MessageFormat.format("Ignoring a story with an invalid material -> {0}", key)
                );
                continue;
            }

            final Story story = new Story(storySection, StoryRarity.UNIQUE);
            final int tier = wholeSection.getInt("tier");
            // elements 列表只读一次复用（原实现读两次，995 键 × 每次新建列表）
            final List<String> elements = wholeSection.getStringList("elements");
            final List<StoryType> types = elements.stream()
                                                  .map(StoryType::getByName)
                                                  .filter(Objects::nonNull)
                                                  .collect(Collectors.toList());

            // 非法元素名已在过滤时剔除（原实现只打日志，null 混入池后发掘时 NPE）
            if (elements.size() != types.size()) {
                CrystamaeHistoria.getInstance().getLogger().info(
                    MessageFormat.format("A block has a badly typed element -> {0}", key)
                );
            }

            // tier 非法（缺失/越界）时跳过该方块：null 的 BlockTier 会在运行期
            // canBeStoried 等处延迟 NPE
            final BlockTier blockTier = blockTierMap.get(tier);
            if (blockTier == null) {
                CrystamaeHistoria.getInstance().getLogger().info(
                    MessageFormat.format("Ignoring a story with an invalid tier -> {0} (tier {1})", key, tier)
                );
                continue;
            }

            final BlockDefinition blockDefinition = new BlockDefinition(
                material,
                blockTier,
                types,
                story
            );
            blockDefinitionMap.put(material, blockDefinition);
            storyMapUnique.put(story.getId(), story);
        }
        CrystamaeHistoria.getInstance().getLogger().info(
            MessageFormat.format("已加载: {0} 个独特的方块故事.", blockDefinitionMap.size())
        );
    }

    @ParametersAreNonnullByDefault
    private void fillMap(Map<String, Story> map, ConfigurationSection section, StoryRarity rarity) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection storySection = section.getConfigurationSection(key);
            Preconditions.checkNotNull(storySection, "Section is null, this doesn't make sense so don't worry.");
            Story story = new Story(storySection, rarity);
            map.put(story.getId(), story);
        }
    }

    @ParametersAreNonnullByDefault
    public static void rebuildStoriedStack(ItemStack itemStack) {
        final ItemMeta im = itemStack.getItemMeta();
        rebuildStoriedStack(itemStack, im, StoryUtils.getAllStories(itemStack));
        itemStack.setItemMeta(im);
    }

    /**
     * 同 {@link #rebuildStoriedStack(ItemStack)}，但接受调用方已持有的元数据快照与
     * 已反序列化的故事列表（单次往返提交路径使用，不触发 getItemMeta/setItemMeta）。
     * 名称读取自该快照（各调用路径下快照的显示名与已应用状态一致）。
     */
    @ParametersAreNonnullByDefault
    public static void rebuildStoriedStack(ItemStack itemStack, ItemMeta itemMeta, @Nullable List<Story> storyList) {
        setName(itemStack, itemMeta);
        List<String> lore = new ArrayList<>();
        // 伪造/损坏物品可能带故事标记但无故事列表（getAllStories 为 @Nullable）
        if (storyList != null) {
            for (Story story : storyList) {
                lore.add("");
                lore.add(story.getDisplayName());
                lore.addAll(story.getStoryLore());
            }
        }
        itemMeta.setLore(lore);
    }

    @ParametersAreNonnullByDefault
    private static void setName(ItemStack itemStack, ItemMeta im) {
        // 幂等化：基础名取自当前显示名，若不剥离已有前缀，每次重建（每个故事提交/
        // 祭坛提取）都会再前置一次"有故事的"造成叠加（上游遗留缺陷，也会自愈已污染物品）
        String base = org.bukkit.ChatColor.stripColor(NameUtils.getItemStackName(im, itemStack.getType()));
        while (base.startsWith(STORIED_PREFIX)) {
            base = base.substring(STORIED_PREFIX.length());
        }
        TextComponent name = new TextComponent(STORIED_PREFIX + base);
        name.setColor(ThemeType.MAIN.getColor());
        name.setBold(true);
        im.setDisplayName(name.toLegacyText());
    }

    private static final String STORIED_PREFIX = "有故事的";

    @ParametersAreNonnullByDefault
    public Story getStory(String id, StoryRarity storyRarity) {
        switch (storyRarity) {
            case COMMON:
                return storyMapCommon.get(id);
            case UNCOMMON:
                return storyMapUncommon.get(id);
            case RARE:
                return storyMapRare.get(id);
            case EPIC:
                return storyMapEpic.get(id);
            case MYTHICAL:
                return storyMapMythical.get(id);
            case UNIQUE:
                return storyMapUnique.get(id);
            default:
                throw new IllegalStateException("Unexpected value: " + storyRarity);
        }
    }
}

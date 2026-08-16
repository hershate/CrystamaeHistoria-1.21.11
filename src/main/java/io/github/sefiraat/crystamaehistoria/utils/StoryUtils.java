package io.github.sefiraat.crystamaehistoria.utils;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.managers.StoriesManager;
import io.github.sefiraat.crystamaehistoria.stories.BlockDefinition;
import io.github.sefiraat.crystamaehistoria.stories.BlockTier;
import io.github.sefiraat.crystamaehistoria.stories.Story;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryChances;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesV2DataType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@UtilityClass
public class StoryUtils {

    /**
     * Returns true if the block is able to have stories (is in the map)
     *
     * @param itemStack The {@link ItemStack} to check
     * @return true if in the stories map
     */
    @ParametersAreNonnullByDefault
    public static boolean canBeStoried(ItemStack itemStack, int tier) {
        return canBeStoried(itemStack, tier, isStoried(itemStack));
    }

    /**
     * 同 {@link #canBeStoried(ItemStack, int)}，但接受调用方已读取的 storied 标记，
     * 避免同一 tick 内多次 getItemMeta 克隆（机械每 tick 判定链使用）。
     */
    @ParametersAreNonnullByDefault
    public static boolean canBeStoried(ItemStack itemStack, int tier, boolean storied) {
        final Material material = itemStack.getType();
        final BlockDefinition definition = CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().get(material);

        return definition != null && definition.getBlockTier().tier <= tier && isAllowed(itemStack, storied);
    }

    private static final Set<Material> metaBypass = EnumSet.of(
        Material.BEE_NEST,
        Material.BEEHIVE,
        Material.ENCHANTED_BOOK,
        Material.FILLED_MAP,
        Material.FIREWORK_ROCKET,
        Material.FIREWORK_STAR,
        Material.LINGERING_POTION,
        Material.PLAYER_HEAD,
        Material.POTION,
        Material.SPLASH_POTION,
        Material.SPAWNER,
        Material.SUSPICIOUS_STEW,
        Material.TIPPED_ARROW,
        Material.WRITTEN_BOOK,
        Material.AXOLOTL_BUCKET,
        Material.COD_BUCKET,
        Material.PUFFERFISH_BUCKET,
        Material.SALMON_BUCKET,
        Material.TROPICAL_FISH_BUCKET
    );

    @ParametersAreNonnullByDefault
    private static boolean isAllowed(ItemStack itemStack, boolean storied) {
        SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);
        if (slimefunItem == null) {
            return metaBypass.contains(itemStack.getType()) || storied || !itemStack.hasItemMeta();
        } else {
            return itemStack.getType() == Material.SPAWNER;
        }
    }

    /**
     * Returns true if the has been storied. This does not mean that is HAS
     * stories, only that it has started to be processed byu a chronicler
     *
     * @param itemStack The {@link ItemStack} to check
     * @return true if has previously been chronicled at any point
     */
    @ParametersAreNonnullByDefault
    public static boolean isStoried(@Nonnull ItemStack itemStack) {
        return isStoried(itemStack.getItemMeta());
    }

    /**
     * 同 {@link #isStoried(ItemStack)}，但接受调用方已读取的 ItemMeta，
     * 避免机械每 tick 判定链的重复克隆。
     */
    public static boolean isStoried(@Nullable ItemMeta itemMeta) {
        return itemMeta != null && PersistentDataAPI.hasBoolean(itemMeta, Keys.PDC_IS_STORIED);
    }

    /**
     * Sets the ItemStack's PDC Storied to True. Also sets an initial story object
     *
     * @param itemStack The {@link ItemStack} whose meta will have the PDC element added to
     */
    @ParametersAreNonnullByDefault
    public static void makeStoried(ItemStack itemStack) {
        // 单次元数据往返：故事上限与 storied 标记共用同一次克隆与应用
        // （原实现 getStoryLimits 内部再克隆一次；getInitialStoryLimits 的及早求值顺序保持不变）
        final ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataAPI.setBoolean(itemMeta, Keys.PDC_IS_STORIED, true);
        setStoryLimits(itemMeta, PersistentDataAPI.getJsonObject(
            itemMeta, Keys.PDC_POTENTIAL_STORIES, getInitialStoryLimits(itemStack)));
        itemStack.setItemMeta(itemMeta);
    }

    /**
     * Sets the ItemStack's PDC StoryList
     *
     * @param itemMeta   The {@link ItemMeta} to add the PDC element to
     * @param jsonObject The {@link JsonObject} to add to the PDC
     */
    @ParametersAreNonnullByDefault
    private static void setStoryLimits(ItemMeta itemMeta, JsonObject jsonObject) {
        PersistentDataAPI.setJsonObject(itemMeta, Keys.PDC_POTENTIAL_STORIES, jsonObject);
    }

    /**
     * Returns true if the has been storied. This does not mean that is HAS
     * stories, only that it has started to be processed byu a chronicler
     *
     * @param itemStack The {@link ItemStack} to check
     * @return true if has previously been chronicled at any point
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static JsonObject getStoryLimits(ItemStack itemStack) {
        return PersistentDataAPI.getJsonObject(
            itemStack.getItemMeta(),
            Keys.PDC_POTENTIAL_STORIES,
            getInitialStoryLimits(itemStack)
        );
    }

    /**
     * Creates a new JsonObject for a newly storied item.
     * We do this now to 'lock in' the story potential
     *
     * @param itemStack The {@link ItemStack} to compare against the storied map
     * @return New {@link JsonObject} with content for story count and tier.
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static JsonObject getInitialStoryLimits(ItemStack itemStack) {
        Material material = itemStack.getType();
        BlockDefinition definition = CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().get(material);
        Preconditions.checkNotNull(
            definition,
            "The selected material does not have a story definition. This shouldn't happen, SefiDumb™"
        );
        int availableStoryCount = ThreadLocalRandom.current()
                                                   .nextInt(
                                                       definition.getBlockTier().minStories,
                                                       definition.getBlockTier().maxStories + 1
                                                   );
        int tier = definition.getBlockTier().tier;
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(Keys.JS_S_AVAILABLE_STORIES, new JsonPrimitive(availableStoryCount));
        jsonObject.add(Keys.JS_S_TIER, new JsonPrimitive(tier));
        return jsonObject;
    }

    /**
     * Returns true if there is room for more stories
     *
     * @param itemStack The {@link ItemStack} to check
     */
    @ParametersAreNonnullByDefault
    public static boolean hasRemainingStorySlots(ItemStack itemStack) {
        return hasRemainingStorySlots(itemStack.getItemMeta());
    }

    /**
     * 同 {@link #hasRemainingStorySlots(ItemStack)}，但接受调用方已读取的 ItemMeta。
     */
    public static boolean hasRemainingStorySlots(@Nullable ItemMeta itemMeta) {
        return getMaxStoryAmount(itemMeta) - getStoryAmount(itemMeta) > 0;
    }

    /**
     * Gets the ItemStack's remaining possible stories
     *
     * @param itemStack The {@link ItemStack} to check
     */
    @ParametersAreNonnullByDefault
    public static int getRemainingStoryAmount(ItemStack itemStack) {
        return getMaxStoryAmount(itemStack) - getStoryAmount(itemStack.getItemMeta());
    }

    /**
     * Gets the Item's max number of StoryList
     *
     * @param itemStack The {@link ItemStack} to add the PDC element to
     */
    @ParametersAreNonnullByDefault
    public static int getMaxStoryAmount(ItemStack itemStack) {
        // PDC 中的 JsonObject 不可信（改造客户端可伪造部分键/非数字/坏 JSON）：
        // 异常或缺失一律按 0 处理（无剩余槽位），避免每 tick 异常卡死机械
        try {
            final JsonObject limits = getStoryLimits(itemStack);
            if (limits == null || !limits.has(Keys.JS_S_AVAILABLE_STORIES)
                || !limits.get(Keys.JS_S_AVAILABLE_STORIES).isJsonPrimitive()
            ) {
                return 0;
            }
            return limits.get(Keys.JS_S_AVAILABLE_STORIES).getAsInt();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * 同 {@link #getMaxStoryAmount(ItemStack)}，但接受调用方已读取的 ItemMeta。
     */
    public static int getMaxStoryAmount(@Nullable ItemMeta itemMeta) {
        // PDC 中的 JsonObject 不可信（改造客户端可伪造部分键/非数字/坏 JSON）：
        // 异常或缺失一律按 0 处理（无剩余槽位），避免每 tick 异常卡死机械
        try {
            if (itemMeta == null) {
                return 0;
            }
            final JsonObject limits = PersistentDataAPI.getJsonObject(itemMeta, Keys.PDC_POTENTIAL_STORIES, null);
            if (limits == null || !limits.has(Keys.JS_S_AVAILABLE_STORIES)
                || !limits.get(Keys.JS_S_AVAILABLE_STORIES).isJsonPrimitive()
            ) {
                return 0;
            }
            return limits.get(Keys.JS_S_AVAILABLE_STORIES).getAsInt();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * Gets the ItemStack's current number of StoryList
     *
     * @param itemMeta The {@link ItemMeta} to get the count from
     */
    @ParametersAreNonnullByDefault
    public static int getStoryAmount(ItemMeta itemMeta) {
        return PersistentDataAPI.getInt(itemMeta, Keys.PDC_CURRENT_NUMBER_OF_STORIES, 0);
    }

    @ParametersAreNonnullByDefault
    public static void requestNewStory(ItemStack itemstack) {
        final Story story = pickStory(itemstack);
        if (story != null) {
            applyStory(itemstack, story);
            incrementStoryAmount(itemstack);
        }
    }

    /**
     * 仅选取一条常规故事（不写物品）：供单次往返提交路径（{@link #commitStory}）
     * 先取故事再落盘。选取逻辑与旧 requestNewStory 完全一致（稀有度滚动 →
     * 类型池 → 故事池），空池返回 null。
     */
    @Nullable
    @ParametersAreNonnullByDefault
    public static Story pickStory(ItemStack itemstack) {
        final StoriesManager manager = CrystamaeHistoria.getStoriesManager();
        final BlockDefinition definition = manager.getBlockDefinitionMap().get(itemstack.getType());
        // blocks.yml 可被编辑：定义缺失（含元素池为空）时跳过，不能 NPE/越界
        if (definition == null || definition.getPools().isEmpty()) {
            return null;
        }
        final BlockTier tier = definition.getBlockTier();
        final StoryChances chance = tier.storyChances;
        final List<StoryType> pool = definition.getPools();
        int rnd = ThreadLocalRandom.current().nextInt(1, 101);

        final StoryRarity rarity;
        if (rnd <= chance.getMythical()) {
            rarity = StoryRarity.MYTHICAL;
        } else if (rnd <= chance.getEpic()) {
            rarity = StoryRarity.EPIC;
        } else if (rnd <= chance.getRare()) {
            rarity = StoryRarity.RARE;
        } else if (rnd <= chance.getUncommon()) {
            rarity = StoryRarity.UNCOMMON;
        } else {
            rarity = StoryRarity.COMMON;
        }
        return pickFromPool(itemstack, pool, rarity);
    }

    @ParametersAreNonnullByDefault
    @Nullable
    private static Story pickFromPool(ItemStack itemstack, List<StoryType> p, StoryRarity rarity) {
        // 与 addStory 的选取逻辑一致（独立出来供提交路径复用）
        final StoryType st = p.get(ThreadLocalRandom.current().nextInt(0, p.size()));
        // 稀有度×类型索引查表（原实现每次对整稀有度故事表做 stream 过滤 + 收集）
        final List<Story> availableStories = CrystamaeHistoria.getStoriesManager().getStories(rarity, st);
        // generic-stories.yml 可被编辑：该稀有度下无此类型故事时跳过，
        // 原 nextInt(0, 0) 抛 IllegalArgumentException 使记录者面板每 tick 报错
        if (availableStories == null || availableStories.isEmpty()) {
            return null;
        }
        return availableStories.get(ThreadLocalRandom.current().nextInt(0, availableStories.size()));
    }

    @ParametersAreNonnullByDefault
    public static void addStory(ItemStack itemStack, List<StoryType> p, StoryRarity rarity) {
        final StoryType st = p.get(ThreadLocalRandom.current().nextInt(0, p.size()));
        // 稀有度×类型索引查表（原实现每次对整稀有度故事表做 stream 过滤 + 收集）
        final List<Story> availableStories = CrystamaeHistoria.getStoriesManager().getStories(rarity, st);
        // generic-stories.yml 可被编辑：该稀有度下无此类型故事时跳过，
        // 原 nextInt(0, 0) 抛 IllegalArgumentException 使记录者面板每 tick 报错
        if (availableStories == null || availableStories.isEmpty()) {
            return;
        }
        final Story story = availableStories.get(ThreadLocalRandom.current().nextInt(0, availableStories.size()));
        applyStory(itemStack, story);
        incrementStoryAmount(itemStack);
    }

    @ParametersAreNonnullByDefault
    public static void applyStory(ItemStack itemStack, Story story) {
        final ItemMeta im = itemStack.getItemMeta();
        List<Story> storyList = getAllStories(itemStack);
        if (storyList == null) {
            storyList = new ArrayList<>();
        }
        storyList.add(story);
        writeStories(im, storyList);
        itemStack.setItemMeta(im);
    }

    /**
     * 故事列表统一写入（v2 瘦编码）：写 v2 键并移除残留的 v1 键，
     * 物品上不留双份编码。v1 物品一经任何写路径触碰即迁移为 v2。
     */
    @ParametersAreNonnullByDefault
    private static void writeStories(ItemMeta itemMeta, List<Story> storyList) {
        DataTypeMethods.setCustom(itemMeta, Keys.PDC_STORIES_V2, PersistentStoriesV2DataType.TYPE, storyList);
        itemMeta.getPersistentDataContainer().remove(Keys.PDC_STORIES);
    }

    /**
     * Sets the ItemStack's current number of StoryList
     *
     * @param itemStack The {@link ItemStack} to increment the story amount
     */
    @ParametersAreNonnullByDefault
    public static void incrementStoryAmount(ItemStack itemStack) {
        setStoryAmount(itemStack, getStoryAmount(itemStack.getItemMeta()) + 1);
    }

    @ParametersAreNonnullByDefault
    @Nullable
    public static List<Story> getAllStories(ItemStack itemStack) {
        return getAllStories(itemStack.getItemMeta());
    }

    /**
     * 同 {@link #getAllStories(ItemStack)}，但接受调用方已持有的 ItemMeta，
     * 供单次往返的提交/移除路径复用同一次克隆。
     * v2 瘦编码优先；v2 结构损坏时降级回退 v1（crafted 物品优雅降级，
     * 与 v1 逐条目跳过同级，不产生 tick 异常）。
     */
    @Nullable
    public static List<Story> getAllStories(@Nullable ItemMeta itemMeta) {
        if (itemMeta == null) {
            return null;
        }
        try {
            final List<Story> v2 = DataTypeMethods.getCustom(itemMeta, Keys.PDC_STORIES_V2, PersistentStoriesV2DataType.TYPE);
            if (v2 != null) {
                return v2;
            }
        } catch (IllegalStateException e) {
            // v2 结构损坏：按 v1（或无数据）继续，不覆盖性丢弃
        }
        return DataTypeMethods.getCustom(itemMeta, Keys.PDC_STORIES, PersistentStoriesDataType.TYPE);
    }

    /**
     * Sets the ItemStack's current number of StoryList
     *
     * @param itemStack The {@link ItemStack} to add the PDC element to
     * @param amount    The amount of stories to set
     */
    @ParametersAreNonnullByDefault
    public static void setStoryAmount(ItemStack itemStack, int amount) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataAPI.setInt(itemMeta, Keys.PDC_CURRENT_NUMBER_OF_STORIES, amount);
        if (amount >= getMaxStoryAmount(itemStack)) {
            itemMeta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        itemStack.setItemMeta(itemMeta);
    }

    @ParametersAreNonnullByDefault
    public static void requestUniqueStory(ItemStack itemstack) {
        final Story unique = pickUniqueStory(itemstack);
        if (unique != null) {
            applyStory(itemstack, unique);
        }
    }

    /**
     * 仅选取方块独特故事（不写物品）：供单次往返提交路径使用。缺失返回 null。
     */
    @Nullable
    @ParametersAreNonnullByDefault
    public static Story pickUniqueStory(ItemStack itemstack) {
        final StoriesManager m = CrystamaeHistoria.getStoriesManager();
        final BlockDefinition s = m.getBlockDefinitionMap().get(itemstack.getType());
        // blocks.yml 可被编辑：定义或独特故事缺失时跳过
        if (s == null || s.getUnique() == null) {
            return null;
        }
        return s.getUnique();
    }

    @ParametersAreNonnullByDefault
    public static int removeStory(ItemStack itemStack, Story story) {
        final ItemMeta im = itemStack.getItemMeta();
        final List<Story> storyList = getAllStories(itemStack);
        Preconditions.checkNotNull(storyList, "No storyList found when trying to remove.");
        storyList.remove(story);
        writeStories(im, storyList);
        itemStack.setItemMeta(im);
        return storyList.size();
    }

    /**
     * 单次元数据往返的故事提交（记录者面板每条故事落盘路径，热路径）。
     * <p>
     * 等价于旧序列 {@code applyStory + incrementStoryAmount [+ applyStory(unique)]
     * + StoriesManager.rebuildStoriedStack}：8-10 次元数据克隆与 3-4 次应用归并为
     * 1 次克隆 + 1 次应用，故事列表反序列化 2-3 次、序列化 1-2 次归并为各 1 次。
     * 最终物品状态（故事列表 PDC、故事计数、满槽附魔标记、名称与 lore）逐字段一致；
     * 异常时保持原状态不落盘（原子性优于旧序列的中间态部分落盘）。
     *
     * @param mainStory   本次发掘的常规故事；null 表示池空未选中（计数不变，
     *                    与旧 addStory 空池跳过一致），此时仅可能提交独特故事
     * @param uniqueStory 最后一格发掘时附带的方块独特故事（可 null）
     */
    @ParametersAreNonnullByDefault
    public static void commitStory(ItemStack itemStack, @Nullable Story mainStory, @Nullable Story uniqueStory) {
        if (mainStory == null && uniqueStory == null) {
            return;
        }
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        List<Story> storyList = getAllStories(itemMeta);
        if (storyList == null) {
            storyList = new ArrayList<>();
        }
        if (mainStory != null) {
            storyList.add(mainStory);
        }
        if (uniqueStory != null) {
            storyList.add(uniqueStory);
        }
        writeStories(itemMeta, storyList);
        if (mainStory != null) {
            // 常规故事计数 +1（独特故事不计数，与旧 incrementStoryAmount 仅由 addStory 调用一致）
            final int newAmount = getStoryAmount(itemMeta) + 1;
            PersistentDataAPI.setInt(itemMeta, Keys.PDC_CURRENT_NUMBER_OF_STORIES, newAmount);
            if (newAmount >= getMaxStoryAmount(itemMeta)) {
                itemMeta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        StoriesManager.rebuildStoriedStack(itemStack, itemMeta, storyList);
        itemStack.setItemMeta(itemMeta);
    }

    /**
     * 单次元数据往返的故事移除（现实祭坛提取路径，热路径）。
     * <p>
     * 等价于旧序列 {@code removeStory [+ StoriesManager.rebuildStoriedStack]}：
     * 列表清空时不重建名称/lore（调用方随即销毁物品）。
     *
     * @param itemMeta 调用方为该物品持有的元数据快照（与 knownList 同源）
     * @param knownList 调用方经 {@link #getAllStories(ItemMeta)} 取得的列表（可 null，按空列表处理）
     * @return 移除后剩余故事数
     */
    @ParametersAreNonnullByDefault
    public static int removeStoryAndRebuild(
        ItemStack itemStack, ItemMeta itemMeta, Story story, @Nullable List<Story> knownList
    ) {
        final List<Story> storyList = knownList != null ? knownList : new ArrayList<>();
        storyList.remove(story);
        writeStories(itemMeta, storyList);
        final int remaining = storyList.size();
        if (remaining > 0) {
            StoriesManager.rebuildStoriedStack(itemStack, itemMeta, storyList);
        }
        itemStack.setItemMeta(itemMeta);
        return remaining;
    }
}

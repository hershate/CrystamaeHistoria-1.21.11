package io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.realisationaltar;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.player.PlayerStatistics;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.AbstractCache;
import io.github.sefiraat.crystamaehistoria.stories.BlockDefinition;
import io.github.sefiraat.crystamaehistoria.stories.Story;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.GildingUtils;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.ParticleUtils;
import io.github.sefiraat.crystamaehistoria.utils.StoryUtils;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkDataType;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkV2DataType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RealisationAltarCache extends AbstractCache {

    @Getter
    private final Map<BlockPosition, RealisedCrystalState> crystalStoryMap = new HashMap<>();
    /** 镀金晶簇粒子参数（不可变值对象，跨调用共享——r59 族） */
    private static final Particle.DustOptions GILDED_DUST = new Particle.DustOptions(Color.YELLOW, 2);

    private final int tier;
    /**
     * 实体拾取扫描中心（机械上方 1 格）。机械位置放置后固定，懒初始化缓存——
     * 免每 tick 的 Location 克隆+偏移两次分配。调用方不修改该实例。
     */
    private Location pickupLocation;
    /**
     * 故事判定备忘录：物品实例未变且未被本机械修改时（processItem 显式置空失效），
     * isStoried && !hasRemainingStorySlots 判定恒定——稳态每 tick 一次引用比较。
     */
    private ItemStack verdictItem;
    private boolean verdictReady;

    @ParametersAreNonnullByDefault
    public RealisationAltarCache(BlockMenu blockMenu, int tier) {
        super(blockMenu);
        this.tier = tier;

        final String activePlayerString = BlockStorage.getLocationInfo(blockMenu.getLocation(), Keys.BS_CP_ACTIVE_PLAYER);
        if (activePlayerString != null) {
            try {
                this.activePlayer = UUID.fromString(activePlayerString);
            } catch (IllegalArgumentException e) {
                // 持久化 UUID 损坏（BlockStorage 数据不可信）：保持 null，仅影响统计归属
            }
        }
    }

    protected void process() {
        tryGrow();
        final ItemStack inputItem = blockMenu.getItemInSlot(RealisationAltar.INPUT_SLOT);

        // No item inserted, try to pick up (T5 +) or shutdown
        if (inputItem == null || inputItem.getType() == Material.AIR) {
            if (this.tier >= 5) {
                tryInsertItem();
            }
            return;
        }

        // 判定备忘录：同一物品实例直接复用上次判定（processItem 修改后显式失效）
        if (inputItem != verdictItem) {
            final ItemMeta inputMeta = inputItem.getItemMeta();
            verdictItem = inputItem;
            verdictReady = StoryUtils.isStoried(inputMeta) && !StoryUtils.hasRemainingStorySlots(inputMeta);
        }
        if (verdictReady) {
            rejectOverage(inputItem);
            if (processItem(inputItem)) {
                saveMap();
            }
            // processItem 可能修改/消耗物品（removeStory/reject/setAmount(0)）：失效备忘录
            verdictItem = null;
        }
    }

    private void tryInsertItem() {
        final Collection<Entity> entities = getWorld().getNearbyEntities(
            getPickupLocation(),
            0.3,
            0.3,
            0.3,
            Item.class::isInstance
        );

        if (!entities.isEmpty()) {
            // 直接迭代取首元素：集合已按 Item 过滤，stream().findFirst() 的
            // 流分配 + Optional 包装为纯开销（每 tick 每 T5 机械）
            final Item item = entities.isEmpty() ? null : (Item) entities.iterator().next();
            final ItemStack itemStack = item.getItemStack();
            final ItemStack clone = itemStack.asQuantity(1);

            this.blockMenu.replaceExistingItem(RealisationAltar.INPUT_SLOT, clone);

            final int amount = CrystamaeHistoria.getSupportedPluginManager().getStackAmount(item);

            if (amount == 1) {
                item.remove();
            } else {
                CrystamaeHistoria.getSupportedPluginManager().setStackAmount(item, amount - 1);
            }
        }
    }

    private void tryGrow() {
        if (crystalStoryMap.isEmpty()) {
            return;
        }
        final Iterator<Map.Entry<BlockPosition, RealisedCrystalState>> iterator = crystalStoryMap.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<BlockPosition, RealisedCrystalState> entry = iterator.next();
            final RealisedCrystalState state = entry.getValue();
            // 驻留条目解析缓存：晶簇位置构造即定恒不变，每 tick 的 getBlock() 块解析
            // 与粒子中心 Location 两次分配为纯浪费（成熟晶簇存活至破坏——常驻每 tick 路径）
            Block block = state.cachedBlock;
            if (block == null) {
                block = entry.getKey().getBlock();
                state.cachedBlock = block;
                state.particleLocation = block.getLocation().add(0.5, 0.2, 0.5);
            }
            final Material material = block.getType();

            if (state.isGilded()) {
                summonGildedParticles(state.particleLocation);
            }

            switch (material) {
                case SMALL_AMETHYST_BUD:
                    if (GeneralUtils.testChance(1, 10)) {
                        block.setType(Material.MEDIUM_AMETHYST_BUD);
                        summonGrowParticles(state.particleLocation);
                    }
                    break;
                case MEDIUM_AMETHYST_BUD:
                    if (GeneralUtils.testChance(1, 20)) {
                        block.setType(Material.LARGE_AMETHYST_BUD);
                        summonGrowParticles(state.particleLocation);
                    }
                    break;
                case LARGE_AMETHYST_BUD:
                    summonFullyGrownParticles(state.particleLocation);
                    break;
                default:
                    iterator.remove();
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void rejectOverage(ItemStack i) {
        if (i.getAmount() > 1) {
            final ItemStack i2 = i.clone();
            i.setAmount(1);
            i2.setAmount(i2.getAmount() - 1);
            blockMenu.getBlock().getWorld().dropItemNaturally(blockMenu.getLocation(), i2);
        }
    }

    @ParametersAreNonnullByDefault
    private boolean processItem(ItemStack itemStack) {
        final BlockDefinition definition = CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().get(itemStack.getType());
        if (definition == null) {
            // blocks.yml 可被编辑：物品带故事标记但材质已无定义时直接退回，不能 NPE 卡死 tick
            reject(itemStack);
            return false;
        }
        if (definition.getBlockTier().tier <= this.tier + 1) {
            if (GeneralUtils.testChance(1, 6)) {
                final int x = ThreadLocalRandom.current().nextInt(-3, 4);
                final int z = ThreadLocalRandom.current().nextInt(-3, 4);
                final Block potentialBlock = blockMenu.getBlock().getRelative(x, 0, z);
                if (potentialBlock.isEmpty() && potentialBlock.getRelative(BlockFace.DOWN).getType().isSolid()) {
                    // 单次元数据往返提取：故事列表/镀金标记读取与移除写回复用同一次克隆
                    // （旧链 getAllStories + isGilded + removeStory + rebuildStoriedStack 约 7 次克隆）
                    final ItemMeta itemMeta = itemStack.getItemMeta();
                    final List<Story> storyList = StoryUtils.getAllStories(itemMeta);
                    if (storyList == null || storyList.isEmpty()) {
                        // 数据损坏（有故事槽位标记但无故事内容/列表缺失）：退回物品，
                        // 避免 get(0) 越界（列表缺失原实现 NPE 穿透 tick，按空处理失败关闭）
                        reject(itemStack);
                        return false;
                    }
                    final Story story = storyList.get(0);
                    final boolean isGilded = GildingUtils.isGilded(itemMeta);

                    potentialBlock.setType(Material.SMALL_AMETHYST_BUD);
                    crystalStoryMap.put(
                        new BlockPosition(potentialBlock.getLocation()),
                        new RealisedCrystalState(story.getRarity(), story.getId(), isGilded)
                    );
                    if (StoryUtils.removeStoryAndRebuild(itemStack, itemMeta, story, storyList) == 0) {
                        if (activePlayer != null) {
                            PlayerStatistics.addRealisation(activePlayer, definition);
                        }
                        itemStack.setAmount(0);
                    }
                    summonGrowParticles(potentialBlock);
                    summonConsumeParticles(blockMenu.getBlock());
                    return true;
                }
            }
        } else {
            reject(itemStack);
        }
        return false;
    }

    public void saveMap() {
        final Chunk chunk = blockMenu.getBlock().getChunk();
        final BlockPosition position = new BlockPosition(blockMenu.getLocation());
        final List<Story> stories = new ArrayList<>();
        for (Map.Entry<BlockPosition, RealisedCrystalState> entry : crystalStoryMap.entrySet()) {
            RealisedCrystalState state = entry.getValue();
            // generic-stories.yml 可被编辑：故事查不到时跳过该条目，不能让落盘整体失败
            final Story source = CrystamaeHistoria.getStoriesManager().getStory(state.storyId, state.storyRarity);
            if (source == null) {
                continue;
            }
            Story story = source.copy();
            story.setBlockPosition(entry.getKey());
            story.setGilded(state.gilded);
            stories.add(story);
        }
        // v2 扁平编码：每次成功提取步骤触发的全量落盘，PDC 键操作 5N → 5
        PersistentStoryChunkV2DataType.writeChunkStories(
            chunk, Keys.newKey(String.valueOf(position.getPosition())), stories);
    }

    private void summonGrowParticles(@Nonnull Location location) {
        ParticleUtils.displayParticleEffect(location, Particle.CRIMSON_SPORE, 0.4, 3);
    }

    private void summonFullyGrownParticles(@Nonnull Location location) {
        ParticleUtils.displayParticleEffect(location, Particle.WAX_OFF, 0.4, 3);
    }

    private void summonGildedParticles(@Nonnull Location location) {
        ParticleUtils.displayParticleEffect(location, 0.4, 3, GILDED_DUST);
    }

    private void summonGrowParticles(@Nonnull Block block) {
        // 生成事件级一次性调用（无驻留语义），保留 Block 便捷重载
        ParticleUtils.displayParticleEffect(block.getLocation().add(0.5, 0.2, 0.5), Particle.CRIMSON_SPORE, 0.4, 3);
    }

    private void summonConsumeParticles(@Nonnull Block block) {
        final Location location = block.getLocation().add(0.5, 0.2, 0.5);
        ParticleUtils.displayParticleEffect(location, Particle.FLASH, 0.4, 2);
    }

    protected void reject(@Nullable ItemStack itemStack) {
        if (itemStack != null) {
            final ItemStack rejectedSpawn = itemStack.clone();
            itemStack.setAmount(0);
            blockMenu.getBlock().getWorld().dropItemNaturally(blockMenu.getLocation(), rejectedSpawn);
        }
    }

    protected void loadMap() {
        final Chunk chunk = blockMenu.getBlock().getChunk();
        final BlockPosition position = new BlockPosition(blockMenu.getLocation());
        final List<Story> stories = PersistentStoryChunkV2DataType.readChunkStories(
            chunk,
            Keys.newKey(String.valueOf(position.getPosition()))
        );
        if (stories != null) {
            for (Story story : stories) {
                // 持久化数据不可信：缺少方块位置的故事条目不能进入映射（否则 tryGrow 中 NPE）
                if (story.getBlockPosition() == null) {
                    continue;
                }
                crystalStoryMap.put(
                    story.getBlockPosition(),
                    new RealisedCrystalState(story.getRarity(), story.getId(), story.isGilded())
                );
            }
        }
    }

    protected void kill() {
        Iterator<BlockPosition> iterator = crystalStoryMap.keySet().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next().getBlock();
            block.setType(Material.AIR);
            iterator.remove();
            summonConsumeParticles(block);
        }
        final Chunk chunk = blockMenu.getBlock().getChunk();
        final BlockPosition position = new BlockPosition(blockMenu.getLocation());
        PersistentDataAPI.remove(chunk, Keys.newKey(String.valueOf(position.getPosition())));
        clearMap();
    }

    private void clearMap() {
        PersistentDataAPI.remove(blockMenu.getBlock().getChunk(), Keys.RESOLUTION_CRYSTAL_MAP);
    }

    protected World getWorld() {
        return blockMenu.getLocation().getWorld();
    }

    protected Location getLocation() {
        return blockMenu.getLocation();
    }

    private Location getPickupLocation() {
        if (pickupLocation == null) {
            pickupLocation = blockMenu.getLocation().add(0.5, 1, 0.5);
        }
        return pickupLocation;
    }

    public class RealisedCrystalState {
        private final StoryRarity storyRarity;
        private final String storyId;
        private final boolean gilded;
        // 位置恒定（构造即定）的驻留解析缓存：tryGrow 每 tick 消费
        @Nullable
        private Block cachedBlock;
        @Nullable
        private Location particleLocation;

        private RealisedCrystalState(StoryRarity storyRarity, String storyId, boolean gilded) {
            this.storyRarity = storyRarity;
            this.storyId = storyId;
            this.gilded = gilded;
        }

        public StoryRarity getStoryRarity() {
            return storyRarity;
        }

        public String getStoryId() {
            return storyId;
        }

        public boolean isGilded() {
            return gilded;
        }
    }

}

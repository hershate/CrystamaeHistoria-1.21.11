package io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.chroniclerpanel;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.player.PlayerStatistics;
import io.github.sefiraat.crystamaehistoria.runnables.spells.FloatingHeadAnimation;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.AbstractCache;
import io.github.sefiraat.crystamaehistoria.stories.BlockDefinition;
import io.github.sefiraat.crystamaehistoria.stories.Story;
import io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.StoryUtils;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class ChroniclerPanelCache extends AbstractCache {

    private final int tier;
    @Nullable
    private Material workingOn;
    private boolean working;
    private BlockDefinition blockDefinition;
    private FloatingHeadAnimation animation;
    private Location blockMiddle;
    private boolean lightDimming = true;
    private UUID armorStandUUID;
    private Location pickupLocation;
    /**
     * 故事判定备忘录：物品实例未变且未被本机械修改时（makeStoried/processStack 等
     * 修改点显式置空失效），storied/可记录/剩余槽位的判定结果恒定。
     * 稳态每 tick 从元数据克隆 + 故事上限 JSON 解析降为一次引用比较。
     */
    private ItemStack verdictItem;
    private boolean verdictStoried;
    private boolean verdictCanBeStoried;
    private boolean verdictHasRemaining;
    private int verdictRemaining;

    @ParametersAreNonnullByDefault
    public ChroniclerPanelCache(BlockMenu blockMenu, int tier) {
        super(blockMenu);
        this.tier = tier;

        // BlockStorage 为持久化数据，不可信：损坏的值不能阻断缓存构造（否则机械注册失败后永久失效）
        final String workingOnString = BlockStorage.getLocationInfo(blockMenu.getLocation(), Keys.BS_CP_WORKING_ON);
        if (workingOnString != null) {
            try {
                setWorking(blockMenu.getBlock(), Material.valueOf(workingOnString));
            } catch (IllegalArgumentException e) {
                CrystamaeHistoria.getInstance().getLogger().warning(
                    "记录者面板 " + blockMenu.getLocation() + " 的存储材质数据损坏（" + workingOnString + "），已重置工作状态");
                BlockStorage.addBlockInfo(blockMenu.getLocation(), Keys.BS_CP_WORKING_ON, null);
            }
        }

        final String activePlayerString = BlockStorage.getLocationInfo(blockMenu.getLocation(), Keys.BS_CP_ACTIVE_PLAYER);
        if (activePlayerString != null) {
            try {
                this.activePlayer = UUID.fromString(activePlayerString);
            } catch (IllegalArgumentException e) {
                // UUID 损坏时保持 activePlayer 为 null（仅影响统计归属）
            }
        }
    }

    @ParametersAreNonnullByDefault
    protected void setWorking(Block block, Material material) {
        final Block lightBlock = block.getRelative(BlockFace.UP);

        blockMiddle = block.getLocation().clone().add(0.5, 0.5, 0.5);
        workingOn = material;
        working = true;

        BlockStorage.addBlockInfo(block, Keys.BS_CP_WORKING_ON, material.toString());
        if (lightBlock.getType() == Material.AIR) {
            lightBlock.setType(Material.LIGHT);
        }
        startAnimation();
        blockDefinition = CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().get(material);
    }

    private void startAnimation() {
        final ArmorStand armourStand = getDisplayStand();

        ArmourStandUtils.panelAnimationReset(armourStand, blockMenu.getBlock());
        animation = new FloatingHeadAnimation(armourStand);
        animation.runTaskTimer(CrystamaeHistoria.getInstance(), 0, FloatingHeadAnimation.SPEED);
    }

    @ParametersAreNonnullByDefault
    private ArmorStand getDisplayStand() {
        if (armorStandUUID == null) {
            final String uuidString = BlockStorage.getLocationInfo(getLocation(), "ch_display_stand");
            if (uuidString != null) {
                try {
                    armorStandUUID = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    // 持久化 UUID 损坏，走重建分支
                    armorStandUUID = null;
                }
            }
        }
        if (armorStandUUID != null) {
            final Entity entity = Bukkit.getEntity(armorStandUUID);
            if (entity instanceof ArmorStand) {
                return (ArmorStand) entity;
            }
            // 记录的展示架已不存在（被杀/清除），重建并覆盖记录，避免后续每次调用 NPE
        }
        final Block block = blockMenu.getBlock();
        final ArmorStand armorStand = (ArmorStand) block.getWorld().spawnEntity(getLocation().add(0.5, -0.6, 0.5), EntityType.ARMOR_STAND);
        ArmourStandUtils.setDisplay(armorStand);
        BlockStorage.addBlockInfo(block.getLocation(), "ch_display_stand", armorStand.getUniqueId().toString());
        armorStandUUID = armorStand.getUniqueId();
        return armorStand;
    }

    protected Location getLocation() {
        return blockMenu.getLocation().clone();
    }

    /**
     * 实体拾取扫描中心（机械上方 0.5 格）。机械位置放置后固定，
     * 懒初始化缓存——免每 tick 的 Location 克隆+偏移两次分配。
     * 调用方（getNearbyEntities）不修改该实例。
     */
    private Location getPickupLocation() {
        if (pickupLocation == null) {
            pickupLocation = blockMenu.getLocation().add(0.5, 0.5, 0.5);
        }
        return pickupLocation;
    }

    protected void process() {
        final Block block = blockMenu.getBlock();
        final ItemStack inputItem = blockMenu.getItemInSlot(ChroniclerPanel.INPUT_SLOT);

        // No item inserted, try to pick up (T5 +) or shutdown
        if (inputItem == null || inputItem.getType() == Material.AIR) {
            if (this.tier >= 5) {
                tryInsertItem();
            }
            return;
        }

        // 判定备忘录：同一物品实例直接复用上次判定（修改点显式失效）
        if (inputItem != verdictItem) {
            refreshVerdict(inputItem);
        }

        if (!verdictCanBeStoried) {
            reject(inputItem);
            shutdown();
            return;
        }

        rejectOverage(inputItem);

        if (!verdictStoried) {
            StoryUtils.makeStoried(inputItem);
            // makeStoried 写入了新的故事上限，立即重判（仅新物品的首个 tick）
            refreshVerdict(inputItem);
        }

        if (!verdictHasRemaining) {
            if (this.tier >= 5) {
                pushOutItem();
            }
            shutdown();
            return;
        }

        // A block is in the Input slot. Does the current item being worked on match the input?
        final Material inputItemType = inputItem.getType();
        if (!working || workingOn != inputItemType) {
            // Either not working or workingOn has changed
            setWorking(block, inputItemType);
            ArmourStandUtils.setDisplayItem(getDisplayStand(), workingOn);
        } else {
            // Working with an item in slot while workingOn matches means we can process the item
            animateLight();
            processStack(inputItem);
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

        if (entities.isEmpty()) {
            shutdown();
        } else {
            final Item item = (Item) entities.stream().findFirst().orElse(null);
            final ItemStack itemStack = item.getItemStack();
            final ItemStack clone = itemStack.asQuantity(1);

            this.blockMenu.replaceExistingItem(ChroniclerPanel.INPUT_SLOT, clone);

            final int amount = CrystamaeHistoria.getSupportedPluginManager().getStackAmount(item);

            if (amount == 1) {
                item.remove();
            } else {
                CrystamaeHistoria.getSupportedPluginManager().setStackAmount(item, amount - 1);
            }
        }
    }

    private void pushOutItem() {
        final ItemStack inputItem = this.blockMenu.getItemInSlot(ChroniclerPanel.INPUT_SLOT);
        if (inputItem == null || inputItem.getType() == Material.AIR) {
            return;
        }
        // blockMiddle 仅在 setWorking 后赋值：面板从未工作时（例如直接塞入已满故事的物品）为 null，
        // 原实现会 NPE 导致该 tick 报错且物品永远卡死在槽位
        final Location base = this.blockMiddle != null ? this.blockMiddle : blockMenu.getLocation().clone().add(0.5, 0.5, 0.5);
        final Location pushLocation = base.clone().subtract(0, 1, 0);
        pushLocation.getWorld().dropItem(pushLocation, inputItem.clone());
        this.blockMenu.replaceExistingItem(ChroniclerPanel.INPUT_SLOT, null);
    }

    public void shutdown() {
        if (working) {
            setNotWorking(blockMenu.getBlock());
            ArmourStandUtils.clearDisplayItem(getDisplayStand());
        }
    }

    @ParametersAreNonnullByDefault
    protected void rejectOverage(ItemStack i) {
        if (i.getAmount() > 1) {
            final ItemStack i2 = i.clone();
            i.setAmount(1);
            i2.setAmount(i2.getAmount() - 1);
            blockMenu.getBlock().getWorld().dropItemNaturally(blockMenu.getLocation(), i2);
        }
    }

    protected void reject(@Nullable ItemStack itemStack) {
        if (itemStack != null) {
            final ItemStack rejectedSpawn = itemStack.clone();
            itemStack.setAmount(0);
            blockMenu.getBlock().getWorld().dropItemNaturally(blockMenu.getLocation(), rejectedSpawn);
        }
    }

    @ParametersAreNonnullByDefault
    private void setNotWorking(Block block) {
        final Block lightBlock = block.getRelative(BlockFace.UP);

        workingOn = null;
        working = false;
        BlockStorage.addBlockInfo(block, Keys.BS_CP_WORKING_ON, null);

        if (lightBlock.getType() == Material.LIGHT) {
            lightBlock.setType(Material.AIR);
        }

        if (animation != null) {
            animation.cancel();
        }

        ArmourStandUtils.panelAnimationReset(getDisplayStand(), block);
    }

    /**
     * 以单次元数据读取刷新故事判定备忘录（canBeStoried 的 storied 标记取
     * makeStoried 之前的值，与原时序一致）。
     */
    private void refreshVerdict(ItemStack inputItem) {
        final ItemMeta meta = inputItem.getItemMeta();
        verdictItem = inputItem;
        verdictStoried = StoryUtils.isStoried(meta);
        verdictCanBeStoried = StoryUtils.canBeStoried(inputItem, this.tier + 1, verdictStoried);
        verdictRemaining = StoryUtils.getMaxStoryAmount(meta) - StoryUtils.getStoryAmount(meta);
        verdictHasRemaining = verdictRemaining > 0;
    }

    @ParametersAreNonnullByDefault
    private void processStack(ItemStack i) {
        summonParticles();
        // If this block isn't storied, make it storied then add the initial story set
        // （process() 已在同一 tick 内完成同一判定且其间无修改，直接用备忘录值）
        if (verdictHasRemaining) {
            final int remaining = verdictRemaining;
            final int req = blockDefinition.getBlockTier().chroniclingChance;
            if (GeneralUtils.testChance(req, 10000)) {
                // We can chronicle a story
                // 单次元数据往返提交：选取 + 落盘（含计数/满槽附魔）+ 名称/lore 重建
                // 归一为 1 次克隆 + 1 次应用（旧链 8-10 次克隆 + 3-4 次应用）
                final Story story = StoryUtils.pickStory(i);
                final Story unique = remaining == 1 ? StoryUtils.pickUniqueStory(i) : null;
                if (story != null || unique != null) {
                    StoryUtils.commitStory(i, story, unique);
                }
                if (remaining == 1 && activePlayer != null) {
                    // That was the last story, unlock unique and set research
                    if (!PlayerStatistics.hasUnlockedUniqueStory(activePlayer, blockDefinition)) {
                        PlayerStatistics.unlockUniqueStory(activePlayer, blockDefinition);
                    }
                    PlayerStatistics.addChronicle(activePlayer, blockDefinition);
                }
                blockMenu.getBlock().getWorld().strikeLightningEffect(blockMiddle);
                // 提交可能修改了物品元数据：失效备忘录，下 tick 重判
                verdictItem = null;
            }
        }
    }

    private void animateLight() {
        final Block block = blockMenu.getBlock().getRelative(BlockFace.UP);
        if (block.getType() == Material.LIGHT) {
            final Light light = (Light) block.getBlockData();
            int level = light.getLevel();
            if (level >= 15) {
                light.setLevel(level - 1);
                lightDimming = true;
            } else if (level <= 5) {
                light.setLevel(level + 1);
                lightDimming = false;
            } else {
                light.setLevel(lightDimming ? level - 1 : level + 1);
            }
            block.setBlockData(light);
        }
    }

    private void summonParticles() {
        final Location location = blockMenu.getLocation();
        for (int i = 0; i < 2; i++) {
            final Location l = location.clone().add(ThreadLocalRandom.current().nextDouble(0, 1.1), 1, ThreadLocalRandom.current().nextDouble(0, 1.1));
            location.getWorld().spawnParticle(Particle.ENCHANT, l, 0, 0.2, 0, -0.2, 0);
        }
    }

    protected void kill() {
        setNotWorking(blockMenu.getBlock());
        getDisplayStand().remove();
    }

    protected World getWorld() {
        return blockMenu.getLocation().getWorld();
    }

    protected Location getLocation(boolean centered) {
        if (centered) {
            return getLocation().add(0.5, 0.5, 0.5);
        } else {
            return getLocation();
        }
    }

}

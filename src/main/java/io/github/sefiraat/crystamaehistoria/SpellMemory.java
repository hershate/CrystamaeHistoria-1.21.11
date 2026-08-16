package io.github.sefiraat.crystamaehistoria;

import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.DisplayItem;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicFallingBlock;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicProjectile;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicSummon;
import io.github.sefiraat.crystamaehistoria.runnables.spells.SpellTickRunnable;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpellMemory {

    @Getter
    private final Map<MagicProjectile, Pair<CastInformation, Long>> projectileMap = new HashMap<>();
    @Getter
    private final Map<MagicFallingBlock, Pair<CastInformation, Long>> fallingBlockMap = new HashMap<>();
    // UUID 反查索引：ProjectileHitEvent/EntityChangeBlockEvent 为世界级高频事件（任意原版箭矢
    // 命中/沙砾落地均触发），原先对 projectileMap 的 stream 线性扫描在空表时也要付出整条
    // 流水线分配成本。索引与主表在 register/unregister 同步维护，命中路径降为 O(1)。
    private final Map<UUID, MagicProjectile> projectileIndex = new HashMap<>();
    private final Map<UUID, MagicFallingBlock> fallingBlockIndex = new HashMap<>();
    // 无敌状态注册表：PDC_IS_INVULNERABLE 原先写入实体 NBT 且每次伤害事件读取；
    // 唯一写入方 Protectorate 的过期仅 1050ms（远短于任何重启耗时），会话内存表
    // 语义等价且免去实体 PDC 读写的 NBT 往返与残留键。
    @Getter
    private final Map<UUID, Long> invulnerableEntities = new HashMap<>();
    @Getter
    private final Map<UUID, Pair<CastInformation, Long>> strikeMap = new HashMap<>();
    @Getter
    private final Map<SpellTickRunnable, Integer> tickingCastables = new HashMap<>();
    @Getter
    private final Map<BlockPosition, Long> blocksToRemove = new HashMap<>();
    @Getter
    private final Map<MagicSummon, Long> summonedEntities = new HashMap<>();
    @Getter
    private final Map<UUID, Long> playersWithFlight = new HashMap<>();
    @Getter
    private final Map<UUID, Long> playersWithFrozenTime = new HashMap<>();
    @Getter
    private final Map<UUID, Long> playersWithFrozenWeather = new HashMap<>();
    @Getter
    private final Map<UUID, Long> inhibitedEndermen = new HashMap<>();
    @Getter
    private final Map<BoundingBox, Long> noSpawningAreas = new HashMap<>();
    @Getter
    private final Map<DisplayItem, Long> displayItems = new HashMap<>();
    @Getter
    private final Map<UUID, Location> sleepingBags = new HashMap<>();

    public void clearAll() {
        // Cancels all outstanding spells being cast
        for (SpellTickRunnable spellTickRunnable : tickingCastables.keySet()) {
            spellTickRunnable.cancel();
        }
        tickingCastables.clear();

        // Clear all projectiles created from spells
        removeProjectiles(true);
        projectileMap.clear();

        // Clear all projectiles created from spells
        removeFallingBlocks(true);
        fallingBlockMap.clear();

        // Clear all spawned entities created from spells
        removeEntities(true);
        summonedEntities.clear();

        // Remove all temporary blocks
        removeBlocks(true);
        blocksToRemove.clear();

        // Remove and disable all players flight
        removeFlight(true);
        playersWithFlight.clear();

        // Reset all players personal time
        removeFrozenTime(true);
        playersWithFrozenTime.clear();

        // Reset all players personal weather
        removeFrozenWeather(true);
        playersWithFrozenWeather.clear();

        // Reenable Enderman teleporting
        removeEnderman(true);
        inhibitedEndermen.clear();

        // Clear all registered lightning strikes that never fired their callbacks
        removeStrikes(true);
        strikeMap.clear();

        // Re-enable Chunk Spawning
        enableSpawningInArea(true);
        noSpawningAreas.clear();

        // Remove Floating display items
        removeDisplayItems(true);
        displayItems.clear();

        // Remove Sleeping Bags
        removeSleepingBags();
        sleepingBags.clear();

        // 会话内无敌标记随之失效（与其它临时效果一致，关服即结束）
        invulnerableEntities.clear();
        projectileIndex.clear();
        fallingBlockIndex.clear();

    }

    /**
     * 登记魔法弹射物（主表 + UUID 索引同步维护）。
     * 取代旧 {@code getProjectileMap().put(...)} 直写——绕过本方法会导致索引失配。
     */
    public void registerProjectile(@Nonnull MagicProjectile magicProjectile,
                                   @Nonnull Pair<CastInformation, Long> value) {
        projectileMap.put(magicProjectile, value);
        projectileIndex.put(magicProjectile.getProjectileUUID(), magicProjectile);
    }

    /** 注销魔法弹射物（幂等，kill 自移除路径）。 */
    public void unregisterProjectile(@Nonnull MagicProjectile magicProjectile) {
        projectileMap.remove(magicProjectile);
        projectileIndex.remove(magicProjectile.getProjectileUUID());
    }

    /**
     * 按实体 UUID 反查魔法弹射物（O(1)）。非魔法弹射物返回 null。
     */
    @Nullable
    public MagicProjectile getProjectileByUuid(@Nonnull UUID projectileUuid) {
        return projectileIndex.get(projectileUuid);
    }

    /** 登记魔法下落方块（主表 + UUID 索引同步维护）。 */
    public void registerFallingBlock(@Nonnull MagicFallingBlock magicFallingBlock,
                                     @Nonnull Pair<CastInformation, Long> value) {
        fallingBlockMap.put(magicFallingBlock, value);
        fallingBlockIndex.put(magicFallingBlock.getFallingBlockUUID(), magicFallingBlock);
    }

    /** 注销魔法下落方块（幂等，kill 自移除路径）。 */
    public void unregisterFallingBlock(@Nonnull MagicFallingBlock magicFallingBlock) {
        fallingBlockMap.remove(magicFallingBlock);
        fallingBlockIndex.remove(magicFallingBlock.getFallingBlockUUID());
    }

    /**
     * 按实体 UUID 反查魔法下落方块（O(1)）。非魔法下落方块返回 null。
     */
    @Nullable
    public MagicFallingBlock getFallingBlockByUuid(@Nonnull UUID fallingBlockUuid) {
        return fallingBlockIndex.get(fallingBlockUuid);
    }

    /** 标记实体无敌至指定时刻（毫秒时间戳）。 */
    public void markInvulnerable(@Nonnull UUID entityUuid, long expiryMillis) {
        invulnerableEntities.put(entityUuid, expiryMillis);
    }

    /**
     * 查询实体无敌到期时刻；未登记返回 null。过期条目由调用方按需移除或由
     * 周期清扫回收（见 {@link #removeInvulnerable(boolean)}）。
     */
    @Nullable
    public Long getInvulnerabilityExpiry(@Nonnull UUID entityUuid) {
        return invulnerableEntities.get(entityUuid);
    }

    /** 移除单条无敌标记（命中过期条目时）。 */
    public void removeInvulnerability(@Nonnull UUID entityUuid) {
        invulnerableEntities.remove(entityUuid);
    }

    /** 周期清扫过期无敌标记（无副作用，仅内存条目回收）。 */
    public void removeInvulnerable(boolean forceRemoveAll) {
        if (invulnerableEntities.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final Iterator<Map.Entry<UUID, Long>> iterator = invulnerableEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Long> entry = iterator.next();
            if (forceRemoveAll || entry.getValue() < time) {
                iterator.remove();
            }
        }
    }

    public void removeProjectiles(boolean forceRemoveAll) {
        if (projectileMap.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        // kill()/run() 都会改映射表（kill 自移除、run 的消费者可能间接触发移除），
        // 故先单遍扫描收集、扫描结束后统一执行——常规情况（全部存活）零复制分配
        final List<MagicProjectile> toKill = new ArrayList<>();
        final List<MagicProjectile> toRun = new ArrayList<>();
        for (Map.Entry<MagicProjectile, Pair<CastInformation, Long>> entry : projectileMap.entrySet()) {
            final MagicProjectile magicProjectile = entry.getKey();
            if (time > entry.getValue().getSecondValue() || forceRemoveAll) {
                toKill.add(magicProjectile);
            } else if (magicProjectile.getProjectile() == null) {
                // 实体已消失（命中后事件时序差、区块卸载等）：清理条目，避免残留与后续空引用
                toKill.add(magicProjectile);
            } else {
                // 驱动弹射物的 tick 消费者（如 StarFall/Chaos/Hellscape 的拖尾粒子）。
                // 上游从未驱动该回调，导致注册的弹射物周期效果一直未生效
                toRun.add(magicProjectile);
            }
        }
        for (MagicProjectile magicProjectile : toKill) {
            magicProjectile.kill();
        }
        for (MagicProjectile magicProjectile : toRun) {
            magicProjectile.run();
        }
    }

    public void removeFallingBlocks(boolean forceRemoveAll) {
        if (fallingBlockMap.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final List<MagicFallingBlock> toKill = new ArrayList<>();
        for (Map.Entry<MagicFallingBlock, Pair<CastInformation, Long>> entry : fallingBlockMap.entrySet()) {
            if (time > entry.getValue().getSecondValue() || forceRemoveAll) {
                toKill.add(entry.getKey());
            }
        }
        for (MagicFallingBlock magicFallingBlock : toKill) {
            magicFallingBlock.kill();
        }
    }

    public void removeEntities(boolean forceRemoveAll) {
        if (summonedEntities.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        // kill()/run() 都会改映射表（kill 自移除、run 的消费者可能间接触发移除），先收集后执行
        final List<MagicSummon> toKill = new ArrayList<>();
        final List<MagicSummon> toRun = new ArrayList<>();
        for (Map.Entry<MagicSummon, Long> entry : summonedEntities.entrySet()) {
            final MagicSummon magicSummon = entry.getKey();
            if (time > entry.getValue() || magicSummon.getMob() == null || forceRemoveAll) {
                toKill.add(magicSummon);
            } else if (magicSummon.getPlayer() == null) {
                // 主人已离线：召唤物的 AI/跟随逻辑全部失效（多个 tick 消费者直接链式引用玩家，
                // 离线时 NPE 会中断整个 TemporaryEffectsRunnable 清理链）——按 mobgoals 的
                // 离线自毁语义一致处理
                toKill.add(magicSummon);
            } else {
                toRun.add(magicSummon);
            }
        }
        for (MagicSummon magicSummon : toKill) {
            magicSummon.kill();
        }
        for (MagicSummon magicSummon : toRun) {
            magicSummon.run();
        }
    }

    public void removeBlocks(boolean forceRemoveAll) {
        if (blocksToRemove.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final Iterator<Map.Entry<BlockPosition, Long>> iterator = blocksToRemove.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<BlockPosition, Long> entry = iterator.next();
            if (forceRemoveAll || entry.getValue() < time) {
                try {
                    entry.getKey().getBlock().setType(Material.AIR);
                    iterator.remove();
                } catch (IllegalStateException e) {
                    // BlockPosition 以 WeakReference 持有世界，世界卸载后 getBlock() 会抛出 IllegalStateException。
                    // 此时保留条目，等待世界重新加载后的下一轮再清理（不能抛出，否则会中断本轮后续所有清理）
                }
            }
        }
    }

    public void removeStrikes(boolean forceRemoveAll) {
        if (strikeMap.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final Iterator<Map.Entry<UUID, Pair<CastInformation, Long>>> iterator = strikeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Pair<CastInformation, Long>> entry = iterator.next();
            if (forceRemoveAll || entry.getValue().getSecondValue() < time) {
                // 闪电视觉上转瞬即逝，此处仅清理未被 LightningStrikeEvent 消费掉的残留条目，防止泄漏
                iterator.remove();
            }
        }
    }

    public void removeFlight(boolean forceRemoveAll) {
        if (playersWithFlight.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final Iterator<Map.Entry<UUID, Long>> iterator = playersWithFlight.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Long> entry = iterator.next();
            if (forceRemoveAll || entry.getValue() < time) {
                final Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
                // 玩家已离线时飞行状态随会话重置，条目同样必须移除，否则永久泄漏
                iterator.remove();
            }
        }
    }

    public void removeFrozenTime(boolean forceRemoveAll) {
        if (playersWithFrozenTime.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final Iterator<Map.Entry<UUID, Long>> iterator = playersWithFrozenTime.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Long> entry = iterator.next();
            if (forceRemoveAll || entry.getValue() < time) {
                final Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.resetPlayerTime();
                }
                // 玩家已离线时个人时间随会话重置，条目同样必须移除，否则永久泄漏
                iterator.remove();
            }
        }
    }

    public void removeFrozenWeather(boolean forceRemoveAll) {
        if (playersWithFrozenWeather.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final Iterator<Map.Entry<UUID, Long>> iterator = playersWithFrozenWeather.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Long> entry = iterator.next();
            if (forceRemoveAll || entry.getValue() < time) {
                final Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.resetPlayerWeather();
                }
                // 玩家已离线时个人天气随会话重置，条目同样必须移除，否则永久泄漏
                iterator.remove();
            }
        }
    }

    public void removeEnderman(boolean forceRemoveAll) {
        if (inhibitedEndermen.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final Iterator<Map.Entry<UUID, Long>> iterator = inhibitedEndermen.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Long> entry = iterator.next();
            if (forceRemoveAll || entry.getValue() < time) {
                iterator.remove();
            }
        }
    }

    public void enableSpawningInArea(boolean forceRemoveAll) {
        if (noSpawningAreas.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final Iterator<Map.Entry<BoundingBox, Long>> iterator = noSpawningAreas.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<BoundingBox, Long> entry = iterator.next();
            if (forceRemoveAll || entry.getValue() < time) {
                iterator.remove();
            }
        }
    }

    public void removeDisplayItems(boolean forceRemoveAll) {
        if (displayItems.isEmpty()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final List<DisplayItem> toKill = new ArrayList<>();
        for (Map.Entry<DisplayItem, Long> entry : displayItems.entrySet()) {
            if (forceRemoveAll || entry.getValue() < time) {
                toKill.add(entry.getKey());
            }
        }
        for (DisplayItem displayItem : toKill) {
            displayItem.kill();
            // DisplayItem.kill() 不会自移除映射表，须在此显式移除
            displayItems.remove(displayItem);
        }
    }

    public void removeSleepingBags() {
        // setType 不触碰映射表本身，可直接遍历（无过期概念，全量置 AIR）
        for (Map.Entry<UUID, Location> entry : sleepingBags.entrySet()) {
            final Location location = entry.getValue();
            if (location.isWorldLoaded()) {
                location.getBlock().setType(Material.AIR);
            }
        }
    }

    public void removeFlight(Player player) {
        if (playersWithFlight.containsKey(player.getUniqueId())) {
            player.setAllowFlight(false);
            player.setFlying(false);
            playersWithFlight.remove(player.getUniqueId());
        }
    }

    public void stopBlockRemoval(Block block) {
        BlockPosition blockPosition = new BlockPosition(block);
        blocksToRemove.remove(blockPosition);
    }
}

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpellMemory {

    @Getter
    private final Map<MagicProjectile, Pair<CastInformation, Long>> projectileMap = new HashMap<>();
    @Getter
    private final Map<MagicFallingBlock, Pair<CastInformation, Long>> fallingBlockMap = new HashMap<>();
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

    }

    public void removeProjectiles(boolean forceRemoveAll) {
        Set<MagicProjectile> set = new HashSet<>(projectileMap.keySet());
        for (MagicProjectile magicProjectile : set) {
            long expiry = projectileMap.get(magicProjectile).getSecondValue();
            if (System.currentTimeMillis() > expiry || forceRemoveAll) {
                magicProjectile.kill();
            } else if (magicProjectile.getProjectile() == null) {
                // 实体已消失（命中后事件时序差、区块卸载等）：清理条目，避免残留与后续空引用
                magicProjectile.kill();
            } else {
                // 驱动弹射物的 tick 消费者（如 StarFall/Chaos/Hellscape 的拖尾粒子）。
                // 上游从未驱动该回调，导致注册的弹射物周期效果一直未生效
                magicProjectile.run();
            }
        }
    }

    public void removeFallingBlocks(boolean forceRemoveAll) {
        Set<MagicFallingBlock> set = new HashSet<>(fallingBlockMap.keySet());
        for (MagicFallingBlock magicFallingBlock : set) {
            long expiry = fallingBlockMap.get(magicFallingBlock).getSecondValue();
            if (System.currentTimeMillis() > expiry || forceRemoveAll) {
                magicFallingBlock.kill();
            }
        }
    }

    public void removeEntities(boolean forceRemoveAll) {
        Set<MagicSummon> set = new HashSet<>(CrystamaeHistoria.getSummonedEntityMap().keySet());
        for (MagicSummon magicSummon : set) {
            long expiry = summonedEntities.get(magicSummon);
            if (System.currentTimeMillis() > expiry || magicSummon.getMob() == null || forceRemoveAll) {
                magicSummon.kill();
            } else {
                magicSummon.run();
            }
        }
    }

    public void removeBlocks(boolean forceRemoveAll) {
        long time = System.currentTimeMillis();
        final Set<Map.Entry<BlockPosition, Long>> set = new HashSet<>(blocksToRemove.entrySet());
        for (Map.Entry<BlockPosition, Long> entry : set) {
            if (forceRemoveAll || entry.getValue() < time) {
                try {
                    entry.getKey().getBlock().setType(Material.AIR);
                    blocksToRemove.remove(entry.getKey());
                } catch (IllegalStateException e) {
                    // BlockPosition 以 WeakReference 持有世界，世界卸载后 getBlock() 会抛出 IllegalStateException。
                    // 此时保留条目，等待世界重新加载后的下一轮再清理（不能抛出，否则会中断本轮后续所有清理）
                }
            }
        }
    }

    public void removeStrikes(boolean forceRemoveAll) {
        long time = System.currentTimeMillis();
        final Set<Map.Entry<UUID, Pair<CastInformation, Long>>> set = new HashSet<>(strikeMap.entrySet());
        for (Map.Entry<UUID, Pair<CastInformation, Long>> entry : set) {
            if (forceRemoveAll || entry.getValue().getSecondValue() < time) {
                // 闪电视觉上转瞬即逝，此处仅清理未被 LightningStrikeEvent 消费掉的残留条目，防止泄漏
                strikeMap.remove(entry.getKey());
            }
        }
    }

    public void removeFlight(boolean forceRemoveAll) {
        long time = System.currentTimeMillis();
        final Set<Map.Entry<UUID, Long>> set = new HashSet<>(playersWithFlight.entrySet());
        for (Map.Entry<UUID, Long> entry : set) {
            if (forceRemoveAll || entry.getValue() < time) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
                // 玩家已离线时飞行状态随会话重置，条目同样必须移除，否则永久泄漏
                playersWithFlight.remove(entry.getKey());
            }
        }
    }

    public void removeFrozenTime(boolean forceRemoveAll) {
        long time = System.currentTimeMillis();
        final Set<Map.Entry<UUID, Long>> set = new HashSet<>(playersWithFrozenTime.entrySet());
        for (Map.Entry<UUID, Long> entry : set) {
            if (forceRemoveAll || entry.getValue() < time) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.resetPlayerTime();
                }
                // 玩家已离线时个人时间随会话重置，条目同样必须移除，否则永久泄漏
                playersWithFrozenTime.remove(entry.getKey());
            }
        }
    }

    public void removeFrozenWeather(boolean forceRemoveAll) {
        long time = System.currentTimeMillis();
        final Set<Map.Entry<UUID, Long>> set = new HashSet<>(playersWithFrozenWeather.entrySet());
        for (Map.Entry<UUID, Long> entry : set) {
            if (forceRemoveAll || entry.getValue() < time) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.resetPlayerWeather();
                }
                // 玩家已离线时个人天气随会话重置，条目同样必须移除，否则永久泄漏
                playersWithFrozenWeather.remove(entry.getKey());
            }
        }
    }

    public void removeEnderman(boolean forceRemoveAll) {
        long time = System.currentTimeMillis();
        final Set<Map.Entry<UUID, Long>> set = new HashSet<>(inhibitedEndermen.entrySet());
        for (Map.Entry<UUID, Long> entry : set) {
            if (forceRemoveAll || entry.getValue() < time) {
                inhibitedEndermen.remove(entry.getKey());
            }
        }
    }

    public void enableSpawningInArea(boolean forceRemoveAll) {
        long time = System.currentTimeMillis();
        final Set<Map.Entry<BoundingBox, Long>> set = new HashSet<>(noSpawningAreas.entrySet());
        for (Map.Entry<BoundingBox, Long> entry : set) {
            if (forceRemoveAll || entry.getValue() < time) {
                noSpawningAreas.remove(entry.getKey());
            }
        }
    }

    public void removeDisplayItems(boolean forceRemoveAll) {
        long time = System.currentTimeMillis();
        final Set<Map.Entry<DisplayItem, Long>> set = new HashSet<>(displayItems.entrySet());
        for (Map.Entry<DisplayItem, Long> entry : set) {
            if (forceRemoveAll || entry.getValue() < time) {
                entry.getKey().kill();
                displayItems.remove(entry.getKey());
            }
        }
    }

    public void removeSleepingBags() {
        final Set<Map.Entry<UUID, Location>> set = new HashSet<>(sleepingBags.entrySet());
        for (Map.Entry<UUID, Location> entry : set) {
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

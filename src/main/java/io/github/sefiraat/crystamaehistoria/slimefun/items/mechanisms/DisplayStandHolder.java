package io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms;

import io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

public abstract class DisplayStandHolder extends AbstractCache {

    protected DisplayStandHolder(BlockMenu blockMenu) {
        super(blockMenu);
    }

    @Override
    public void kill(Location location) {
        // 必须在 clearBlockInfo 之前取出展示架：BlockStorage 信息清掉后就找不到记录的 UUID 了，
        // 原实现会生成一个全新盔甲架再删掉，真正的展示架则永久残留世界（实体泄漏）
        final ArmorStand existingStand = findDisplayStand();
        super.kill(location);
        if (existingStand != null) {
            existingStand.remove();
        }
    }

    protected World getWorld() {
        return blockMenu.getLocation().getWorld();
    }

    protected Location getLocation() {
        return blockMenu.getLocation().clone();
    }

    protected Location getLocation(boolean centered) {
        if (centered) {
            return getLocation().add(0.5, 0.5, 0.5);
        } else {
            return getLocation();
        }
    }

    /**
     * 查找已记录的展示架。记录缺失、UUID 损坏或实体已不存在时返回 null，不新建。
     */
    @Nullable
    protected ArmorStand findDisplayStand() {
        final String uuidString = BlockStorage.getLocationInfo(getLocation(), "ch_display_stand");
        if (uuidString == null) {
            return null;
        }
        try {
            final Entity entity = Bukkit.getEntity(UUID.fromString(uuidString));
            return entity instanceof ArmorStand ? (ArmorStand) entity : null;
        } catch (IllegalArgumentException e) {
            // 持久化的 UUID 损坏（BlockStorage 数据不可信），按缺失处理
            return null;
        }
    }

    @ParametersAreNonnullByDefault
    protected ArmorStand getDisplayStand() {
        final ArmorStand existingStand = findDisplayStand();
        if (existingStand != null) {
            return existingStand;
        }
        // 记录缺失/损坏/实体已死亡：重建展示架并覆盖记录
        final Block block = blockMenu.getBlock();
        final ArmorStand armorStand = (ArmorStand) block.getWorld().spawnEntity(getLocation().add(0.5, -1.7, 0.5), EntityType.ARMOR_STAND);
        ArmourStandUtils.setDisplay(armorStand);
        BlockStorage.addBlockInfo(block.getLocation(), "ch_display_stand", armorStand.getUniqueId().toString());
        return armorStand;
    }

}

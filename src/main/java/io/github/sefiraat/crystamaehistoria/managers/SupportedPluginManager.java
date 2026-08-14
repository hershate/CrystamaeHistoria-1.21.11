package io.github.sefiraat.crystamaehistoria.managers;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 可选插件支持管理器。
 * <p>
 * 原实现通过 mcMMO、SlimeTinker、ExoticGarden、WildStacker、RoseStacker、
 * Netheopoiesis、Networks、HeadLimiter 等可选插件扩展兼容性。
 * 为使插件在仅安装 Slimefun 的 1.21 服务器上稳定运行，
 * 这些第三方集成已全部移除：
 * <ul>
 *     <li>伤害归属标记（mcMMO/SlimeTinker）不再需要，直接造成伤害</li>
 *     <li>掉落物堆叠（WildStacker/RoseStacker）回退为原版堆叠数量</li>
 *     <li>ExoticGarden 植物判定恒为否</li>
 * </ul>
 * 保留原有方法签名，避免调用方改动；在目标环境中行为与原版逻辑一致。
 */
public class SupportedPluginManager {

    @SuppressWarnings("unused")
    public SupportedPluginManager() {
        CrystamaeHistoria.getInstance();
    }

    /**
     * 对实体造成伤害并归属于玩家。
     * 原本会在 mcMMO/SlimeTinker 存在时短暂标记实体以避免刷经验，
     * 移除这两个集成后直接造成伤害即可。
     *
     * @param livingEntity 受伤的 {@link LivingEntity}
     * @param player       伤害归属的 {@link Player}
     * @param damage       伤害量
     */
    @ParametersAreNonnullByDefault
    public void playerDamageWithoutAttribution(LivingEntity livingEntity, Player player, double damage) {
        livingEntity.damage(damage, player);
    }

    /**
     * 获取落地物品的堆叠数量（原版数量）。
     *
     * @param item 落地物品
     * @return 堆叠数量
     */
    public int getStackAmount(Item item) {
        return item.getItemStack().getAmount();
    }

    /**
     * 设置落地物品的堆叠数量。
     *
     * @param item   落地物品
     * @param amount 目标数量
     */
    public void setStackAmount(Item item, int amount) {
        item.getItemStack().setAmount(amount);
    }
}

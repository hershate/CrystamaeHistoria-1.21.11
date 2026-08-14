package io.github.sefiraat.crystamaehistoria.managers;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.thebusybiscuit.exoticgarden.items.BonemealableItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 可选插件支持管理器。
 * <p>
 * 保留 Slimefun 附属插件的可选集成（ExoticGarden、SlimeTinker、HeadLimiter、
 * Networks、Netheopoiesis）：这些集成全部由运行时守卫保护，
 * 在仅安装 Slimefun 的服务器上不会激活，行为与原实现一致。
 * <p>
 * 已移除的集成（mcMMO、SlimeTinker 之外的 Slimefun 无关插件）：
 * <ul>
 *     <li>mcMMO 伤害归属标记：直接造成伤害</li>
 *     <li>WildStacker/RoseStacker 掉落物堆叠：回退为原版堆叠数量</li>
 * </ul>
 */
@Getter
public class SupportedPluginManager {

    private static final NamespacedKey IGNORE_DAMAGE_KEY = new NamespacedKey(Slimefun.instance(), "ignore_damage");
    private static SupportedPluginManager instance;

    private boolean exoticGarden;
    private boolean slimeTinker;
    private boolean headLimiter;
    private boolean networks;
    private boolean netheopoiesis;

    public SupportedPluginManager() {
        instance = this;
        Bukkit.getScheduler().runTaskLater(CrystamaeHistoria.getInstance(), this::postSetup, 1);
        this.netheopoiesis = Bukkit.getPluginManager().isPluginEnabled("Netheopoiesis");
    }

    private void postSetup() {
        this.exoticGarden = Bukkit.getPluginManager().isPluginEnabled("ExoticGarden");
        this.slimeTinker = Bukkit.getPluginManager().isPluginEnabled("SlimeTinker");
        this.headLimiter = Bukkit.getPluginManager().isPluginEnabled("HeadLimiter");
        this.networks = Bukkit.getPluginManager().isPluginEnabled("Networks");
    }

    /**
     * 对实体造成伤害并归属于玩家。
     * SlimeTinker 存在时会短暂标记实体以避免其利用伤害归属；
     * mcMMO 集成已移除，不再处理其经验归属标记。
     *
     * @param livingEntity 受伤的 {@link LivingEntity}
     * @param player       伤害归属的 {@link Player}
     * @param damage       伤害量
     */
    @ParametersAreNonnullByDefault
    public void playerDamageWithoutAttribution(LivingEntity livingEntity, Player player, double damage) {
        markIgnoreDamage(livingEntity);
        livingEntity.damage(damage, player);
        clearIgnoreDamageMarker(livingEntity);
    }

    @ParametersAreNonnullByDefault
    public void markIgnoreDamage(LivingEntity livingEntity) {
        if (slimeTinker) {
            PersistentDataAPI.setBoolean(livingEntity, IGNORE_DAMAGE_KEY, true);
        }
    }

    @ParametersAreNonnullByDefault
    public void clearIgnoreDamageMarker(LivingEntity livingEntity) {
        if (slimeTinker) {
            PersistentDataAPI.remove(livingEntity, IGNORE_DAMAGE_KEY);
        }
    }

    @ParametersAreNonnullByDefault
    public boolean isExoticGardenPlant(Block block) {
        return exoticGarden
            && BlockStorage.hasBlockInfo(block)
            && BlockStorage.check(block) instanceof BonemealableItem;
    }

    /**
     * Gets the SlimefunItem for the ExoticPlant if it exists
     *
     * @param block The {@link Block} to check
     * @return Returns null if there is not a plant (or Exotic is not installed) or the
     * the SlimefunItem if applicable.
     */
    @Nullable
    @ParametersAreNonnullByDefault
    public SlimefunItem getExoticGardenPlant(Block block) {
        if (exoticGarden && BlockStorage.hasBlockInfo(block)) {
            SlimefunItem slimefunItem = BlockStorage.check(block);
            if (slimefunItem instanceof BonemealableItem) {
                return slimefunItem;
            }
        }
        return null;
    }

    /**
     * 获取落地物品的堆叠数量。
     * WildStacker/RoseStacker 集成已移除，回退为原版堆叠数量。
     *
     * @param item 落地物品
     * @return 堆叠数量
     */
    public int getStackAmount(Item item) {
        return item.getItemStack().getAmount();
    }

    /**
     * 设置落地物品的堆叠数量。
     * WildStacker/RoseStacker 集成已移除，回退为原版堆叠数量。
     *
     * @param item   落地物品
     * @param amount 目标数量
     */
    public void setStackAmount(Item item, int amount) {
        item.getItemStack().setAmount(amount);
    }

    public static boolean isExoticGarden() {
        return instance.exoticGarden;
    }

    public static boolean isSlimeTinker() {
        return instance.slimeTinker;
    }

    public static boolean isHeadLimiter() {
        return instance.headLimiter;
    }

    public static boolean isNetworks() {
        return instance.networks;
    }

    public boolean isNetheopoiesis() {
        return netheopoiesis;
    }
}

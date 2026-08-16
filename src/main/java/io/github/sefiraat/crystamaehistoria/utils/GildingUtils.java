package io.github.sefiraat.crystamaehistoria.utils;

import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class GildingUtils {

    /**
     * Returns true if the has been gilded.
     *
     * @param itemStack The {@link ItemStack} to check
     * @return true if has previously been gilded at any point
     */
    @ParametersAreNonnullByDefault
    public static boolean isGilded(ItemStack itemStack) {
        return isGilded(itemStack.getItemMeta());
    }

    /**
     * 同 {@link #isGilded(ItemStack)}，但接受调用方已持有的元数据快照
     * （现实祭坛单次往返提取路径使用，省一次克隆）。
     */
    public static boolean isGilded(@Nullable ItemMeta itemMeta) {
        return PersistentDataAPI.hasBoolean(itemMeta, Keys.PDC_IS_GILDED);
    }

    /**
     * Sets the ItemStack's PDC Gilded to True. Also sets an initial story object
     *
     * @param itemStack The {@link ItemStack} whose meta will have the PDC element added to
     */
    @ParametersAreNonnullByDefault
    public static void makeGilded(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataAPI.setBoolean(itemMeta, Keys.PDC_IS_GILDED, true);
        // 伪造物品可能通过镀金前置但无 lore（getLore 为 @Nullable），判空避免 NPE
        List<String> lore = itemStack.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add("");
        lore.add("" + ChatColor.YELLOW + ChatColor.BOLD + "已镀金");
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
    }

}

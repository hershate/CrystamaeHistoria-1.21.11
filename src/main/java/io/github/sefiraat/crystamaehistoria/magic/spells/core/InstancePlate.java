package io.github.sefiraat.crystamaehistoria.magic.spells.core;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.CastResult;
import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.player.PlayerStatistics;
import io.github.sefiraat.crystamaehistoria.utils.TextUtils;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class InstancePlate {

    /**
     * 已记录过施法异常的法术集合（日志限流，防止高频施放刷爆日志；条目数以法术总数为上界）
     */
    private static final Set<SpellType> LOGGED_FAILED_SPELLS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final int tier;
    private final SpellType storedSpell;
    @Setter
    private int crysta;
    @Setter
    private long cooldown = 0;

    @ParametersAreNonnullByDefault
    public InstancePlate(int tier, SpellType storedSpell, int crysta) {
        this.tier = tier;
        this.storedSpell = storedSpell;
        this.crysta = crysta;
    }

    @ParametersAreNonnullByDefault
    public static void setPlateLore(ItemStack itemStack, @Nullable InstancePlate instancePlate) {
        final String magic = instancePlate != null ? TextUtils.toTitleCase(instancePlate.storedSpell.getSpell().getName()) : "无";
        final String crysta = instancePlate != null ? String.valueOf(instancePlate.crysta) : "0";
        final String[] lore = new String[]{
            "经过魔法充能的魔法板",
            "",
            ThemeType.CLICK_INFO.getColor() + "法术: " + ThemeType.NOTICE.getColor() + magic,
            ThemeType.CLICK_INFO.getColor() + "充能: " + ThemeType.NOTICE.getColor() + crysta
        };
        final ChatColor passiveColor = ThemeType.PASSIVE.getColor();
        final List<String> finalLore = new ArrayList<>();
        final ItemMeta itemMeta = itemStack.getItemMeta();

        finalLore.add("");
        for (String s : lore) {
            finalLore.add(passiveColor + s);
        }
        finalLore.add("");
        finalLore.add(ThemeType.applyThemeToString(ThemeType.CLICK_INFO, ThemeType.CRAFTING.getLoreLine()));

        itemMeta.setLore(finalLore);
        itemStack.setItemMeta(itemMeta);
    }

    @ParametersAreNonnullByDefault
    public CastResult tryCastSpell(CastInformation castInformation) {
        final Spell spell = storedSpell.getSpell();
        final int crystaCost = spell.getCrystaCost(castInformation);

        // Is the spell disabled in spells.yml?
        if (!CrystamaeHistoria.getConfigManager().spellEnabled(spell)) {
            return CastResult.SPELL_DISABLED;
        }

        // Is enough crysta currently stored in the plate?
        if (crysta < crystaCost) {
            return CastResult.CAST_FAIL_NO_CRYSTA;
        }

        // Is the spell still on cooldown?
        if (cooldown > System.currentTimeMillis()) {
            return CastResult.ON_COOLDOWN;
        }

        castInformation.setSpellType(storedSpell);
        // 前置校验已全部通过，此交互必然执行法术——现在解析并冻结视线目标，
        // 保证后续回调（含跨 tick）读到施法瞬间值（旧实现在构造器急切 raycast）
        castInformation.freezeTargetsOnCast();
        // 先结算消耗与冷却，再执行法术（失败关闭）：
        // 原实现先施法后扣费，若施法回调抛异常则消耗与冷却都不生效，玩家可零成本无限重试
        this.crysta -= crystaCost;
        final long cdSeconds = (long) (spell.getCooldownSeconds(castInformation) * 1000);
        this.cooldown = System.currentTimeMillis() + cdSeconds;
        PlayerStatistics.addUsage(castInformation.getCaster(), storedSpell);
        try {
            spell.castSpell(castInformation);
        } catch (Exception e) {
            // 断路器：法术回调异常不应穿透到交互事件（否则整条事件链报错），充能已扣，按施法成功返回。
            // 同一法术仅记录首次异常：恶意玩家高频施放有缺陷法术时避免日志风暴
            if (LOGGED_FAILED_SPELLS.add(storedSpell)) {
                CrystamaeHistoria.getInstance().getLogger()
                    .log(java.util.logging.Level.WARNING, "法术 " + storedSpell + " 执行异常（后续同类异常不再记录）", e);
            }
        }
        return CastResult.CAST_SUCCESS;
    }

    public void addCrysta(int amount) {
        // 溢出防御：无上限累积（每次充值受池容量约束但可无限次）在极端长周期下
        // 会越过 int 上限变负，法术板将永久不可用——钳制在上限
        final long newCrysta = (long) this.crysta + amount;
        this.crysta = newCrysta > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) newCrysta;
    }

    public void removeCrysta(int amount) {
        this.crysta -= amount;
    }
}

package io.github.sefiraat.crystamaehistoria.listeners;

import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.CastResult;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.Stave;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

public class SpellCastListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        // 右键交互会为主手与副手各派发一次事件；法杖只读主手，
        // 副手事件重复进入会因冷却触发"施法失败"提示，覆盖刚成功的 action bar 消息
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        final Player player = e.getPlayer();
        final ItemStack stack = player.getInventory().getItemInMainHand();
        // 材质门控：三阶法杖（Basic/Advanced/Arcane）均以 STICK 注册，
        // 其余材质的交互（常态）免 getByItem 的元数据克隆 + PDC 读
        if (stack.getType() != Material.STICK) {
            return;
        }
        final SlimefunItem slimefunItem = SlimefunItem.getByItem(stack);
        if (slimefunItem instanceof Stave) {
            Stave stave = (Stave) slimefunItem;
            // 先解析栏位再反序列化法杖 PDC：栏位为 null（如 PHYSICAL 交互）时
            // 免去整张法术板映射的 PDC 反序列化
            SpellSlot slot = SpellSlot.getByPlayerAndAction(player, e.getAction());
            if (slot == null) {
                return;
            }
            // 单槽局部读取：冷却/缺晶能/空槽等失败前置路径只反序列化本槽法术板
            // （免去其余槽位 3/4 的反序列化）；整个交互仅一次 getItemMeta 克隆，
            // 成功后以同一 meta 快照全量重读合并写回
            final ItemMeta staveMeta = stack.getItemMeta();
            InstanceStave staveInstance = InstanceStave.forSlot(stack, slot, staveMeta);
            CastInformation castInformation = new CastInformation(player, stave.getLevel());
            CastResult castResult = staveInstance.tryCastSpell(slot, castInformation);
            if (castResult == CastResult.CAST_SUCCESS) {
                final InstanceStave writeBack = InstanceStave.forWriteBack(
                    stack, slot, staveInstance.getSpellInstanceMap().get(slot), staveMeta);
                if (writeBack != null) {
                    // 写回：PDC 与 lore 共用同一 meta 快照（法术回调不触碰手持物品元数据）
                    PersistentStaveV2DataType.writeStaveMap(staveMeta, writeBack.getSpellInstanceMap()
                    );
                    writeBack.buildLore(staveMeta);
                    stack.setItemMeta(staveMeta);
                }
                player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                    ThemeType.SUCCESS.getColor() + "释放法术: " + castInformation.getSpellType().getSpell().getName()
                ));
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                    ThemeType.WARNING.getColor() + "施法失败: " + castResult.getMessage())
                );
            }
        }
    }
}
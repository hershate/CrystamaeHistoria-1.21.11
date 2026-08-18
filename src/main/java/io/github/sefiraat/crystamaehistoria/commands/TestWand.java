package io.github.sefiraat.crystamaehistoria.commands;

import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave;
import io.github.sefiraat.crystamaehistoria.slimefun.Tools;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

public class TestWand extends SubCommand {

    public TestWand() {
        super("test-wand", "获取包含选定法术的法杖", true);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length != 2) {
                return;
            }
            final int power;
            try {
                power = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("法杖等级必须为 1-2 的整数!");
                return;
            }
            if (power <= 2) {
                ItemStack stave;
                if (power == 1) {
                    stave = Tools.getStaveBasic().getItem().clone();
                } else if (power == 2) {
                    stave = Tools.getStaveAdvanced().getItem().clone();
                } else {
                    return;
                }

                // 按 id 解析 SpellType。现状全部 69 个法术 id 与枚举常量名完全一致
                // （原 valueOf 亦安全——初判"必抛 IAE"系文件名比对方法论错误所致误报），
                // 但该不变量无任何强制约束——按 id 循环解析消除对隐式耦合的依赖
                SpellType spellType = null;
                for (SpellType st : SpellType.getCachedValues()) {
                    if (st.getId().equals(args[0])) {
                        spellType = st;
                        break;
                    }
                }
                if (spellType == null) {
                    player.sendMessage("法术不存在!");
                    return;
                }

                final InstanceStave staveInstance = new InstanceStave(stave);
                final Map<SpellSlot, InstancePlate> map = staveInstance.getSpellInstanceMap();
                final InstancePlate plateInstance = new InstancePlate(1, spellType, 9999);
                map.put(SpellSlot.LEFT_CLICK, plateInstance);
                ItemMeta itemMeta = stave.getItemMeta();
                PersistentStaveV2DataType.writeStaveMap(itemMeta, staveInstance.getSpellInstanceMap()
                );
                stave.setItemMeta(itemMeta);
                staveInstance.buildLore();
                player.getInventory().addItem(stave);
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void complete(CommandSender commandSender, String[] strings, List<String> list) {
        if (strings.length == 1) {
            for (SpellType spell : SpellType.getEnabledSpells()) {
                list.add(spell.getId());
            }
        }
    }

}
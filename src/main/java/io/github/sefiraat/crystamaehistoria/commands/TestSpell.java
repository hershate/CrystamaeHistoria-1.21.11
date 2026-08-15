package io.github.sefiraat.crystamaehistoria.commands;

import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class TestSpell extends SubCommand {

    public TestSpell() {
        super("test-spell", "施展选定的法术", true);
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
                player.sendMessage(ThemeType.ERROR.getColor() + "强度必须为 1-5 的整数!");
                return;
            }
            // 原实现仅有上界检查：负强度会以负法杖等级参与伤害/范围缩放（负伤害反而治疗目标）
            if (power >= 1 && power <= 5) {
                Spell spell = SpellType.getById(args[0]);
                if (spell != null) {
                    spell.castSpell(new CastInformation((Player) sender, power));
                } else {
                    player.sendMessage(ThemeType.ERROR.getColor() + "法术不存在或无法释放!");
                }
            } else {
                player.sendMessage(ThemeType.ERROR.getColor() + "强度必须为 1-5 的整数!");
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void complete(CommandSender commandSender, String[] strings, List<String> list) {
        if (strings.length == 1) {
            for (SpellType spell : SpellType.getEnabledSpells()) {
                list.add(spell.name());
            }
        }
    }

}
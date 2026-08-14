package io.github.sefiraat.crystamaehistoria.commands;

import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 插件主命令（/crystamaeHistoria，别名 /ch、/hist），负责分派子命令。
 * <p>
 * 原实现来自 InfinityLib 的 AddonCommand/ParentCommand，
 * 为移除 InfinityLib 依赖而在本项目内等价移植。
 */
public class HistoriaCommand implements TabExecutor {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final String commandName;

    @ParametersAreNonnullByDefault
    public HistoriaCommand(PluginCommand command) {
        this.commandName = command.getName();
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    public void addSub(@Nonnull SubCommand subCommand) {
        subCommands.put(subCommand.name(), subCommand);
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));

        if (subCommand == null) {
            sender.sendMessage(ThemeType.ERROR.getColor() + "未知的子命令: " + args[0]);
            sendHelp(sender);
            return true;
        }

        if (!subCommand.canUse(sender)) {
            sender.sendMessage(ThemeType.ERROR.getColor() + "你没有权限使用该命令.");
            return true;
        }

        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ThemeType.MAIN.getColor() + "魔法水晶编年史 命令列表:");
        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.canUse(sender)) {
                sender.sendMessage(
                    ThemeType.CLICK_INFO.getColor() + "/" + commandName + " " + subCommand.name()
                    + ThemeType.PASSIVE.getColor() + " - " + subCommand.description()
                );
            }
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, @Nonnull String[] args) {
        List<String> strings = new ArrayList<>();

        if (args.length == 1) {
            String arg = args[0].toLowerCase(Locale.ROOT);
            for (SubCommand subCommand : subCommands.values()) {
                if (subCommand.canUse(sender) && subCommand.name().startsWith(arg)) {
                    strings.add(subCommand.name());
                }
            }
            return strings;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand != null && subCommand.canUse(sender)) {
            subCommand.complete(sender, Arrays.copyOfRange(args, 1, args.length), strings);
        }

        List<String> returnList = new ArrayList<>();
        String arg = args[args.length - 1].toLowerCase(Locale.ROOT);
        for (String item : strings) {
            if (item.toLowerCase(Locale.ROOT).contains(arg)) {
                returnList.add(item);
                if (returnList.size() >= 64) {
                    break;
                }
            }
        }
        return returnList;
    }
}

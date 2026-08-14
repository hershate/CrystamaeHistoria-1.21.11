package io.github.sefiraat.crystamaehistoria.commands;

import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * 子命令基类。
 * <p>
 * 原实现来自 InfinityLib（io.github.mooy1.infinitylib.commands.SubCommand），
 * 为移除 InfinityLib 依赖而在本项目内等价移植，保持相同的构造器与方法签名。
 */
@ParametersAreNonnullByDefault
public abstract class SubCommand {

    private final Predicate<CommandSender> permission;
    private final String description;
    private final String name;

    protected SubCommand(String name, String description, boolean op) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.description = description;
        this.permission = op ? CommandSender::isOp : sender -> true;
    }

    protected abstract void execute(CommandSender sender, String[] args);

    protected abstract void complete(CommandSender sender, String[] args, List<String> completions);

    public final boolean canUse(CommandSender sender) {
        return permission.test(sender);
    }

    @Nonnull
    public final String name() {
        return name;
    }

    @Nonnull
    public final String description() {
        return description;
    }

}

package bench;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 基准 1：施法前置校验 ConfigManager.spellEnabled(Spell) 的读取路径
 *
 * - 旧：FileConfiguration.getBoolean(id)（真实 paper-api YamlConfiguration，69 个键）
 * - 新：实例字段 isEnabled() 读取（Lombok 生成的 getter 字节码形态等价的模型类；
 *       SpellType 枚举加载需要 Slimefun 运行时，无法脱离服务器实例化，
 *       故以同形态方法建模——报告中已注明此方法论差异）
 */
public final class BenchSpellEnabled {

    /** 模型类：与 Lombok @Getter 生成的 isEnabled() 字节码形态一致 */
    static final class CachedSpell {
        private final boolean enabled;
        CachedSpell(boolean enabled) { this.enabled = enabled; }
        boolean isEnabled() { return enabled; }
    }

    public static void run(Harness h) throws java.io.IOException {
        // 生成 69 键 spells.yml（与真实法术数量一致）；写在 benchmark/build 下，
        // 避免 git-bash 的 /tmp 与 Windows JVM 路径不兼容
        new File("benchmark/build").mkdirs();
        File file = new File("benchmark/build/spells-bench.yml");
        try (PrintWriter w = new PrintWriter(file, StandardCharsets.UTF_8)) {
            for (int i = 0; i < 69; i++) {
                w.println("spell_" + i + ": true");
            }
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        final CachedSpell[] cached = new CachedSpell[69];
        for (int i = 0; i < 69; i++) cached[i] = new CachedSpell(true);
        final String[] keys = new String[69];
        for (int i = 0; i < 69; i++) keys[i] = "spell_" + i;

        h.bench("spellEnabled", "old_yaml_getBoolean", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                if (cfg.getBoolean(keys[i & 63])) bh += 1;
            }
            return bh;
        }, 3000, 20, 100_000);

        h.bench("spellEnabled", "new_field_isEnabled", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                if (cached[i & 63].isEnabled()) bh += 1;
            }
            return bh;
        }, 3000, 20, 1_000_000);
    }
}

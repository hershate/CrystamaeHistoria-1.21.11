package bench;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 基准 2：SpellMemory 周期清理（每秒 TemporaryEffectsRunnable 驱动）的扫描模式
 *
 * 用真实 java.util 容器与真实键类型（UUID）测量新旧两种模式的确切操作序列：
 * - 旧：new HashSet<>(map.keySet()) 整表复制 + 逐 key 二次 map.get(k)（弹射物/下落方块/召唤物形态）
 *       或 new HashSet<>(map.entrySet()) 复制（flight/time/weather/enderman 形态）
 * - 新：map.isEmpty() 早退（空表）或 entrySet 单遍扫描零复制（未过期全存活）
 *
 * 场景：empty=空表（服务器绝大多数时间的常态）、full100=100 条全部存活。
 * 实体句柄（MagicProjectile 等）需要 Bukkit 服务器运行时，无法脱离服务器构造；
 * 本基准测量的是同一方法内容器操作的完整序列（分配 + 遍历 + 条件判断），
 * 差异全部来自该序列本身，与键的实际类型无关（UUID 与实体句柄同为一等对象键）。
 */
public final class BenchSpellMemoryScan {

    public static void run(Harness h) {
        // ---- 空表场景 ----
        final Map<UUID, Long> emptyMap = new HashMap<>();

        h.bench("spellMemoryScan.empty", "old_keyset_copy", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                Set<UUID> set = new HashSet<>(emptyMap.keySet());
                for (UUID k : set) {
                    bh += emptyMap.get(k);
                }
            }
            return bh;
        }, 3000, 20, 100_000);

        h.bench("spellMemoryScan.empty", "new_isEmpty_earlyExit", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                if (emptyMap.isEmpty()) {
                    bh += 1;
                }
            }
            return bh;
        }, 3000, 20, 1_000_000);

        // ---- 100 条全部存活场景 ----
        final Map<UUID, Long> fullMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            fullMap.put(UUID.randomUUID(), Long.MAX_VALUE);
        }

        h.bench("spellMemoryScan.full100", "old_keyset_copy_plus_get", size -> {
            long bh = 0;
            final long now = 1234567890123L;
            for (int i = 0; i < size; i++) {
                Set<UUID> set = new HashSet<>(fullMap.keySet());
                for (UUID k : set) {
                    if (now > fullMap.get(k)) {
                        bh += 1;
                    }
                }
            }
            return bh;
        }, 3000, 20, 20_000);

        h.bench("spellMemoryScan.full100", "old_entryset_copy", size -> {
            long bh = 0;
            final long now = 1234567890123L;
            for (int i = 0; i < size; i++) {
                Set<Map.Entry<UUID, Long>> set = new HashSet<>(fullMap.entrySet());
                for (Map.Entry<UUID, Long> e : set) {
                    if (now > e.getValue()) {
                        bh += 1;
                    }
                }
            }
            return bh;
        }, 3000, 20, 20_000);

        h.bench("spellMemoryScan.full100", "new_entryset_scan", size -> {
            long bh = 0;
            final long now = 1234567890123L;
            for (int i = 0; i < size; i++) {
                for (Map.Entry<UUID, Long> e : fullMap.entrySet()) {
                    if (now > e.getValue()) {
                        bh += 1;
                    }
                }
            }
            return bh;
        }, 3000, 20, 50_000);
    }
}

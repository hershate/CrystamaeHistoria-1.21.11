package bench;

/**
 * 基准测试入口。输出 TSV：bench\tvariant\tmedian_ns\tmin_ns\tp95_ns
 * 由 run.sh 多次调用（多 fork），结果聚合进 results/。
 * 第 24 轮变体：lore 展示写入的组件级缓存（真实 adventure 序列化器）。
 */
public final class BenchRunner {
    public static void main(String[] args) throws Exception {
        Harness h = new Harness();
        System.out.println("bench\tvariant\tmedian_ns_op\tmin_ns_op\tp95_ns_op");
        BenchLoreComponents.run(h);
        System.err.println("blackhole=" + h.getBlackhole());
    }
}

package bench;

/**
 * 轻量基准测试计时器：时间驱动预热（保证 JIT 编译完成）+ 分批测量 + 中位数聚合。
 * 黑洞值经 System.out 汇出，防止死码消除。
 */
public final class Harness {

    public interface Op {
        /** 执行一批 size 次操作，返回累计黑洞值 */
        long run(int size);
    }

    private long blackhole;

    /**
     * @param name     基准名
     * @param variant  变体名
     * @param op       被测操作（一批 size 次迭代）
     * @param warmupMs 预热时长（毫秒）
     * @param batches  测量批数
     * @param size     每批迭代次数
     */
    public void bench(String name, String variant, Op op, long warmupMs, int batches, int size) {
        // 预热：时间驱动，直至 JIT 稳定
        long warmupEnd = System.nanoTime() + warmupMs * 1_000_000L;
        while (System.nanoTime() < warmupEnd) {
            blackhole += op.run(size);
        }
        // 测量
        long[] nanos = new long[batches];
        for (int i = 0; i < batches; i++) {
            long start = System.nanoTime();
            blackhole += op.run(size);
            nanos[i] = System.nanoTime() - start;
        }
        java.util.Arrays.sort(nanos);
        double median = nanos[batches / 2] / (double) size;
        double min = nanos[0] / (double) size;
        double p95 = nanos[(int) (batches * 0.95)] / (double) size;
        System.out.printf("%s\t%s\t%.2f\t%.2f\t%.2f%n", name, variant, median, min, p95);
    }

    public long getBlackhole() {
        return blackhole;
    }
}

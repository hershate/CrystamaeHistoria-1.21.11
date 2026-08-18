package io.github.sefiraat.crystamaehistoria.stories;

import io.github.sefiraat.crystamaehistoria.stories.definition.StoryChances;

import javax.annotation.ParametersAreNonnullByDefault;

public class BlockTier {

    public final int tier;
    /**
     * 每 tick 生成一个故事的概率，分母为 10000
     * （实现见 ChroniclerPanelCache#processStack 的 {@code testChance(req, 10000)}）。
     * 例：T1 面板 700 = 每 tick 7%；数值随面板等级升高而下降（T1=700 .. T5=300）。
     */
    public final int chroniclingChance;
    public final int maxStories;
    public final int minStories;
    public final StoryChances storyChances;


    @ParametersAreNonnullByDefault
    public BlockTier(int tier, int chroniclingChance, int maxStories, int minStories, StoryChances storyChances) {
        this.tier = tier;
        this.chroniclingChance = chroniclingChance;
        this.maxStories = maxStories;
        this.minStories = minStories;
        this.storyChances = storyChances;
    }
}

package io.github.sefiraat.crystamaehistoria.stories;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryShardProfile;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Story {

    @Nonnull
    private final String id;
    @Nullable
    private final String author;
    @Nullable
    private final String sponsor;
    @Nonnull
    private final StoryRarity rarity;
    @Nonnull
    private final StoryType type;
    @Nonnull
    private final StoryShardProfile storyShardProfile;
    @Nonnull
    private final List<String> storyStrings;
    @Setter
    @Nullable
    private BlockPosition blockPosition;
    @Setter
    private boolean gilded = false;

    /**
     * @noinspection ConstantConditions
     */
    @ParametersAreNonnullByDefault
    public Story(ConfigurationSection section, StoryRarity storyRarity) {
        // 启动期每个故事构造一次（1220+ 次）：shards 列表只读一次复用
        final List<Integer> shards = section.getIntegerList("shards");

        this.id = section.getString("name");

        StoryType storyType = StoryType.getByName(section.getString("type"));

        if (shards.size() != 9) {
            CrystamaeHistoria.getInstance().getLogger().warning(
                MessageFormat.format("The following story does not have a correctly setup shard profile: {0}", this.id)
            );
        }

        if (storyType == null) {
            CrystamaeHistoria.getInstance().getLogger().warning(
                MessageFormat.format("A block story has a badly typed element -> {0}", this.id)
            );
        }

        this.rarity = storyRarity;
        this.type = storyType;
        this.storyShardProfile = new StoryShardProfile(shards);
        this.storyStrings = section.getStringList("lore");
        this.author = section.getString("author");
        this.sponsor = section.getString("sponsor");
    }

    @ParametersAreNonnullByDefault
    private Story(Story story) {
        this.rarity = story.rarity;
        this.id = story.getId();
        this.type = story.type;
        this.storyShardProfile = story.getStoryShardProfile();
        this.storyStrings = story.storyStrings;
        this.author = story.author;
        this.sponsor = story.sponsor;
        this.blockPosition = story.blockPosition;
        this.gilded = story.gilded;
    }

    /**
     * 展示名缓存：输出仅依赖构造后不可变的输入（稀有度/id），原实现每次调用
     * 重建组件并做 toLegacyText 转换（Paper 侧组件→legacy 为该路径主导成本）。
     * 故事池实例全局共享，热路径（每条故事提交/提取的 lore 重建、图鉴构建）
     * 反复命中同一实例。主线程单线程访问；偶发重复计算无害（幂等）。
     */
    @Nullable
    private String cachedDisplayName;

    /**
     * 故事正文行缓存：输入（storyStrings/author/sponsor）构造后不可变。
     * 以不可变列表返回（调用方仅 addAll/可变参数消费，禁止变异共享缓存）。
     */
    @Nullable
    private List<String> cachedStoryLore;

    public String getDisplayName() {
        if (cachedDisplayName == null) {
            final TextComponent rarityComponent = new TextComponent(getDisplayRarity());
            final TextComponent nameComponent = new TextComponent(this.id);

            rarityComponent.setColor(ThemeType.getByRarity(this.rarity).getColor());
            rarityComponent.setBold(true);
            nameComponent.setColor(ThemeType.CLICK_INFO.getColor());
            cachedDisplayName = BaseComponent.toLegacyText(rarityComponent, nameComponent);
        }
        return cachedDisplayName;
    }

    public String getDisplayRarity() {
        return "[" + ThemeType.getByRarity(rarity).getLoreLine() + "] ";
    }

    public List<String> getStoryLore() {
        if (cachedStoryLore == null) {
            final ChatColor passive = ThemeType.PASSIVE.getColor();
            final List<String> l = new ArrayList<>();

            for (String s : storyStrings) {
                final TextComponent line = new TextComponent(s);

                line.setColor(passive);
                line.setItalic(false);
                l.add(BaseComponent.toLegacyText(line));
            }
            if (author != null) {
                l.add("");
                l.add(ThemeType.PASSIVE.getColor() + "作者: " + author);
            }
            if (sponsor != null) {
                l.add("");
                l.add(ThemeType.PASSIVE.getColor() + "赞助者: " + sponsor);
            }
            cachedStoryLore = List.copyOf(l);
        }
        return cachedStoryLore;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Story) {
            Story story = (Story) obj;
            return this.id.equals(story.id)
                && this.rarity == story.rarity
                && this.type == story.type;
        } else {
            return false;
        }
    }

    public Story copy() {
        return new Story(this);
    }
}

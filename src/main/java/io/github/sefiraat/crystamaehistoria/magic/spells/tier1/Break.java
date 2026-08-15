package io.github.sefiraat.crystamaehistoria.magic.spells.tier1;

import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.SpellCoreBuilder;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.RecipeSpell;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public class Break extends Spell {

    public Break() {
        SpellCoreBuilder spellCoreBuilder = new SpellCoreBuilder(10, true, 50, true, 5, true)
            .makeInstantSpell(this::cast);
        setSpellCore(spellCoreBuilder.build());
    }

    @ParametersAreNonnullByDefault
    public void cast(CastInformation castInformation) {
        Player player = castInformation.getCasterAsPlayer();
        Block block = player.getTargetBlockExact((int) getRange(castInformation));
        // 视线范围内无方块（例如看向天空）时 getTargetBlockExact 返回 null，跳过本次效果
        if (block == null) {
            return;
        }
        GeneralUtils.tryBreakBlock(castInformation.getCaster(), block);
    }

    @Nonnull
    @Override
    public RecipeSpell getRecipe() {
        return new RecipeSpell(
            1,
            StoryType.ELEMENTAL,
            StoryType.MECHANICAL,
            StoryType.ALCHEMICAL
        );
    }

    @Nonnull
    @Override
    public String getName() {
        return "破坏";
    }

    @Nonnull
    @Override
    public String[] getLore() {
        return new String[]{
            "破坏看向的方块"
        };
    }

    @Nonnull
    @Override
    public String getId() {
        return "BREAK";
    }

    @Nonnull
    @Override
    public Material getMaterial() {
        return Material.CRACKED_STONE_BRICKS;
    }
}

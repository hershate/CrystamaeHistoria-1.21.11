package io.github.sefiraat.crystamaehistoria.magic.spells.tier1;

import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.SpellCoreBuilder;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicFallingBlock;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.RecipeSpell;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.SpellUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PlutosDecent extends Spell {

    protected static final List<Material> MATERIALS = new ArrayList<>();

    static {
        MATERIALS.add(Material.BLACKSTONE_SLAB);
        MATERIALS.add(Material.BLACKSTONE_STAIRS);
        MATERIALS.add(Material.BLACKSTONE_WALL);
        MATERIALS.add(Material.CRACKED_POLISHED_BLACKSTONE_BRICKS);
    }

    public PlutosDecent() {
        SpellCoreBuilder spellCoreBuilder = new SpellCoreBuilder(25, false, 60, false, 75, true)
            .makeDamagingSpell(5, true, 1, false)
            .makeProjectileSpell(this::cast, 0.5, true, 1, true)
            .makeProjectileVsBlockSpell(this::blockLands);
        setSpellCore(spellCoreBuilder.build());
    }

    private void cast(CastInformation castInformation) {
        final Player player = castInformation.getCasterAsPlayer();
        final int range = (int) getRange(castInformation);
        final Block targetBlock = player.getTargetBlockExact(range);

        if (targetBlock != null) {
            final Location target = targetBlock.getLocation().add(0.5, 0.5, 0.5);
            final List<Block> blocks = new ArrayList<>();
            final int radius = getRadius(castInformation);
            final org.bukkit.World world = target.getWorld();

            // 三重循环的 (x,y,z) 偏移组合构造上互异，getBlockAt 结果必不重复：
            // 原 List.contains 去重（O(n²)）为无效工作（r18 TunnelBore 同族）
            for (int y = -radius; y < radius; y++) {
                for (int x = -radius; x < radius; x++) {
                    for (int z = -radius; z < radius; z++) {
                        if (Math.sqrt((double) (x * x) + (y * y) + (z * z)) <= range) {
                            final Block block = world.getBlockAt(
                                x + target.getBlockX(),
                                y + target.getBlockY(),
                                z + target.getBlockZ()
                            );
                            if (GeneralUtils.hasPermission(player, block, Interaction.PLACE_BLOCK)
                            ) {
                                blocks.add(block);
                            }
                        }
                    }
                }
            }
            spawnBlocks(castInformation, blocks);
        }
    }

    private void blockLands(CastInformation castInformation) {
        // 流星从 40 格高空落下需要数秒，施法者可能已下线：
        // 无源爆炸使用不带实体的重载，避免 getCasterAsPlayer() 空引用
        Location location = castInformation.getHitBlock().getLocation();
        final Player caster = castInformation.getCasterAsPlayer();
        if (caster != null) {
            location.getWorld().createExplosion(
                caster,
                location,
                getRadius(castInformation) + 1F,
                true,
                true
            );
        } else {
            location.getWorld().createExplosion(
                location,
                getRadius(castInformation) + 1F,
                true,
                true
            );
        }
    }

    private int getRadius(CastInformation castInformation) {
        final int radius;

        switch (castInformation.getStaveLevel()) {
            case 3:
            case 4:
                radius = 2;
                break;
            case 5:
                radius = 3;
                break;
            default:
                radius = 1;
                break;
        }
        return radius;
    }

    private void spawnBlocks(CastInformation castInformation, List<Block> blocks) {
        for (Block block : blocks) {
            MagicFallingBlock magicFallingBlock = SpellUtils.summonMagicFallingBlock(
                castInformation,
                block.getLocation().add(0, 40, 0),
                MATERIALS.get(ThreadLocalRandom.current().nextInt(MATERIALS.size())),
                5
            );
            magicFallingBlock.setVelocity(block.getLocation(), 2);
        }
    }

    @Nonnull
    @Override
    public RecipeSpell getRecipe() {
        return new RecipeSpell(
            1,
            StoryType.ELEMENTAL,
            StoryType.ALCHEMICAL,
            StoryType.HUMAN
        );
    }

    @Nonnull
    @Override
    public String getName() {
        return "冥王星腰带";
    }
    
    @Nonnull
    @Override
    public String[] getLore() {
        return new String[]{
            "召唤一颗流星来打击你的敌人"
        };
    }

    @Nonnull
    @Override
    public String getId() {
        return "PLUTOS_DESCENT";
    }

    @Nonnull
    @Override
    public Material getMaterial() {
        return Material.COBBLED_DEEPSLATE;
    }
}

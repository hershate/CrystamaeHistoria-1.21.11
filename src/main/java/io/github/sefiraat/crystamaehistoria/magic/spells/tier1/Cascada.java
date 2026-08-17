package io.github.sefiraat.crystamaehistoria.magic.spells.tier1;

import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.SpellCoreBuilder;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicProjectile;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.RecipeSpell;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.SpellUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Cascada extends Spell {

    public Cascada() {
        final SpellCoreBuilder spellCoreBuilder = new SpellCoreBuilder(60, true, 0, false, 30, false)
            .makeProjectileSpell(this::fireProjectile, 2, false, 0, false)
            .makeProjectileVsBlockSpell(this::projectileHit)
            .makeProjectileVsEntitySpell(this::projectileHit);
        setSpellCore(spellCoreBuilder.build());
    }

    @ParametersAreNonnullByDefault
    public void fireProjectile(CastInformation castInformation) {
        final Location location = castInformation.getCastLocation();
        final Location aimLocation = location.clone().add(0, 1.5, 0).add(location.getDirection().multiply(2));
        final MagicProjectile magicProjectile = SpellUtils.summonMagicProjectile(castInformation, EntityType.SNOWBALL, aimLocation);
        magicProjectile.setVelocity(location.getDirection(), 1.5);
        magicProjectile.disableGravity();
    }

    @ParametersAreNonnullByDefault
    public void projectileHit(CastInformation castInformation) {
        final UUID caster = castInformation.getCaster();
        final Location location = castInformation.getProjectileLocation();
        final int range = (int) getProjectileAoe(castInformation) + castInformation.getStaveLevel();
        final List<Block> blocks = new ArrayList<>();
        final World world = location.getWorld();

        // 三重循环的 (x,y,z) 偏移组合构造上互异，getBlockAt 结果必不重复：
        // 原 List.contains 去重（O(n²)）为无效工作（r18 TunnelBore 同族）
        for (int y = -range; y < range; y++) {
            for (int x = -range; x < range; x++) {
                for (int z = -range; z < range; z++) {
                    if (Math.sqrt((double) (x * x) + (y * y) + (z * z)) > range) {
                        continue;
                    }

                    final Block block = world.getBlockAt(
                        x + location.getBlockX(),
                        y + location.getBlockY(),
                        z + location.getBlockZ()
                    );
                    if (GeneralUtils.hasPermission(caster, block, Interaction.BREAK_BLOCK)) {
                        blocks.add(block);
                    }
                }
            }
        }

        for (Block block : blocks) {
            final BlockData blockData = block.getBlockData();
            final Entity entity = block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0.5, 0.5), blockData);
            entity.setVelocity(new Vector(0, 2, 0));
            block.setType(Material.AIR, false);
        }
    }

    @Nonnull
    @Override
    public RecipeSpell getRecipe() {
        return new RecipeSpell(
            1,
            StoryType.MECHANICAL,
            StoryType.HISTORICAL,
            StoryType.PHILOSOPHICAL
        );
    }

    @Nonnull
    @Override
    public String getName() {
        return "抬高地面";
    }

    @Nonnull
    @Override
    public String[] getLore() {
        return new String[]{
            "将目标地面抬升至空中",
            "落下后会伤害仍在目标区域的生物"
        };
    }

    @Nonnull
    @Override
    public String getId() {
        return "CASCADA";
    }

    @Nonnull
    @Override
    public Material getMaterial() {
        return Material.PODZOL;
    }
}

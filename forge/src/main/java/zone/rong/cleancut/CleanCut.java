package zone.rong.cleancut;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * All of CleanCut's logic. The mixin only decides <em>where</em> vanilla is
 * interrupted; everything about <em>what</em> we hit instead lives here.
 *
 * <p>This is the Mojang-mapped copy, shared by the Forge branch. The Fabric
 * branch has its own Yarn-mapped copy of the same behaviour.
 */
public final class CleanCut {

    private CleanCut() {
    }

    /**
     * The entity the player meant to hit through the block at {@code pos}, or
     * {@code null} to let vanilla have the block.
     */
    public static Entity findTarget(Minecraft client, BlockState state, BlockPos pos) {
        LocalPlayer player = client.player;
        Level level = client.level;
        if (player == null || level == null || client.gameMode == null) {
            return null;
        }
        // Anything you can walk through is fair game: grass, flowers, torches,
        // snow layers, carpets. Blocks with real collision are left to vanilla.
        if (!state.getCollisionShape(level, pos).isEmpty()) {
            return null;
        }
        double reach = reach(client);
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 end = start.add(direction.x * reach, direction.y * reach, direction.z * reach);
        return closestEntity(level, player, start, stopAtBlock(level, player, start, end));
    }

    /**
     * Reach lives on the interaction manager up to 1.20.4; from 1.20.5 it is an
     * attribute, read through the player.
     */
    private static double reach(Minecraft client) {
        //? if <1.20.5 {
        return client.gameMode.getPickRange();
        //?} else {
        /*return client.player.entityInteractionRange();
        *///?}
    }

    /**
     * Entities behind a wall are still behind a wall - shorten the search to the
     * first block with actual collision.
     */
    private static Vec3 stopAtBlock(Level level, LocalPlayer player, Vec3 start, Vec3 end) {
        HitResult hit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static Entity closestEntity(Level level, LocalPlayer player, Vec3 start, Vec3 end) {
        AABB searchBox = new AABB(start, end).inflate(1.0);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(player, searchBox, candidate -> canTarget(player, candidate))) {
            AABB hitBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hit = hitBox.clip(start, end);
            if (!hit.isPresent()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
            }
        }
        return closest;
    }

    /** The quality-of-life half of the mod: don't swing at things you like. */
    private static boolean canTarget(LocalPlayer player, Entity entity) {
        if (entity == player || entity.isSpectator() || !entity.isPickable()) {
            return false;
        }
        if (entity.isAlliedTo(player) || isVehicleOf(player, entity)) {
            return false;
        }
        return !(entity instanceof TamableAnimal) || !((TamableAnimal) entity).isOwnedBy(player);
    }

    private static boolean isVehicleOf(LocalPlayer player, Entity entity) {
        for (Entity vehicle = player.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            if (vehicle == entity) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code InteractionResult} was a plain enum until 1.21.5, when it became a
     * sealed interface and the enum's helpers went with it.
     */
    public static boolean accepted(InteractionResult result) {
        //? if <1.16 {
        /*return result == InteractionResult.SUCCESS;
        *///?} elif <1.21.5 {
        return result.consumesAction();
        //?} else {
        /*return result instanceof InteractionResult.Success;
        *///?}
    }
}

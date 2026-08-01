package zone.rong.cleancut;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * All of CleanCut's logic. The mixin only decides <em>where</em> vanilla is
 * interrupted; everything about <em>what</em> we hit instead lives here.
 *
 * <p>Classes that moved or were renamed between Minecraft versions are written
 * out fully qualified inside Stonecutter blocks, so the imports above stay
 * valid on every version we build for.
 */
public final class CleanCut {

    private CleanCut() {
    }

    /**
     * The entity the player meant to hit through the block at {@code pos}, or
     * {@code null} to let vanilla have the block.
     */
    public static Entity findTarget(MinecraftClient client, BlockState state, BlockPos pos) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null || client.interactionManager == null) {
            return null;
        }
        // Anything you can walk through is fair game: grass, flowers, torches,
        // snow layers, carpets. Blocks with real collision are left to vanilla.
        if (!state.getCollisionShape(world, pos).isEmpty()) {
            return null;
        }
        double reach = reach(client);
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.x * reach, direction.y * reach, direction.z * reach);
        return closestEntity(world, player, start, stopAtBlock(world, player, start, end));
    }

    /** Reach is a game mode value up to 1.20.4 and an attribute from 1.20.5 on. */
    private static double reach(MinecraftClient client) {
        //? if <1.20.5 {
        return client.interactionManager.getReachDistance();
        //?} elif <1.21.2 {
        /*return client.player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE);
        *///?} else {
        /*return client.player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ENTITY_INTERACTION_RANGE);
        *///?}
    }

    /**
     * Entities behind a wall are still behind a wall - shorten the search to the
     * first block with actual collision.
     */
    private static Vec3d stopAtBlock(ClientWorld world, ClientPlayerEntity player, Vec3d start, Vec3d end) {
        //? if >=1.16 {
        HitResult hit = world.raycast(new net.minecraft.world.RaycastContext(start, end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE, player));
        //?} else {
        /*HitResult hit = world.rayTrace(new net.minecraft.world.RayTraceContext(start, end,
                net.minecraft.world.RayTraceContext.ShapeType.COLLIDER,
                net.minecraft.world.RayTraceContext.FluidHandling.NONE, player));
        *///?}
        return hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
    }

    private static Entity closestEntity(ClientWorld world, ClientPlayerEntity player, Vec3d start, Vec3d end) {
        Box searchBox = new Box(start, end).expand(1.0);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        //? if >=1.17 {
        for (Entity entity : world.getOtherEntities(player, searchBox, candidate -> canTarget(player, candidate))) {
        //?} else {
        /*for (Entity entity : world.getEntities(player, searchBox, candidate -> canTarget(player, candidate))) {
        *///?}
            Box hitBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            //? if >=1.16 {
            Optional<Vec3d> hit = hitBox.raycast(start, end);
            //?} else {
            /*Optional<Vec3d> hit = hitBox.rayTrace(start, end);
            *///?}
            if (!hit.isPresent()) {
                continue;
            }
            double distance = start.squaredDistanceTo(hit.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
            }
        }
        return closest;
    }

    /** The quality-of-life half of the mod: don't swing at things you like. */
    private static boolean canTarget(ClientPlayerEntity player, Entity entity) {
        if (entity == player || entity.isSpectator()) {
            return false;
        }
        //? if >=1.16 {
        if (!entity.canHit()) {
        //?} else {
        /*if (!entity.collides()) {
        *///?}
            return false;
        }
        if (entity.isTeammate(player) || isVehicleOf(player, entity)) {
            return false;
        }
        return !(entity instanceof TameableEntity) || !((TameableEntity) entity).isOwner(player);
    }

    private static boolean isVehicleOf(ClientPlayerEntity player, Entity entity) {
        for (Entity vehicle = player.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            if (vehicle == entity) {
                return true;
            }
        }
        return false;
    }

    /** {@code ActionResult} only grew its convenience methods in 1.16. */
    public static boolean accepted(ActionResult result) {
        //? if >=1.16 {
        return result.isAccepted();
        //?} else {
        /*return result == ActionResult.SUCCESS;
        *///?}
    }
}

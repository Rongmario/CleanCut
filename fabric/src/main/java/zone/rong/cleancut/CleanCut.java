package zone.rong.cleancut;

//? if <26 {
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
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
*///?}

import java.util.Optional;

/**
 * All of CleanCut's logic. The mixin only decides <em>where</em> vanilla is
 * interrupted; everything about <em>what</em> we hit instead lives here.
 *
 * <p>Classes that moved or were renamed between Minecraft versions are written
 * out fully qualified inside Stonecutter blocks, so the imports above stay
 * valid on every version we build for.
 *
 * <p>From Minecraft 26 there is no Yarn, because there is nothing left to name:
 * the game ships unobfuscated and the names in the jar are Mojang's. Every
 * member below therefore exists in both sets of names. The blocks are written
 * one per member and never nested, so each alternative reads as ordinary code.
 */
public final class CleanCut {

    private CleanCut() {
    }

    /**
     * The entity the player meant to hit through the block at {@code pos}, or
     * {@code null} to let vanilla have the block.
     */
    //? if <26 {
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
    //?} else {
    /*public static Entity findTarget(Minecraft client, BlockState state, BlockPos pos) {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
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
    *///?}

    /**
     * Reach lives on the interaction manager up to 1.20.4; from 1.20.5 it is an
     * attribute, read through the player.
     */
    //? if <1.20.5 {
    private static double reach(MinecraftClient client) {
        return client.interactionManager.getReachDistance();
    }
    //?} elif <26 {
    /*private static double reach(MinecraftClient client) {
        return client.player.getEntityInteractionRange();
    }
    *///?} else {
    /*private static double reach(Minecraft client) {
        return client.player.entityInteractionRange();
    }
    *///?}

    /**
     * Entities behind a wall are still behind a wall - shorten the search to the
     * first block with actual collision.
     */
    //? if <1.16.2 {
    /*private static Vec3d stopAtBlock(ClientWorld world, ClientPlayerEntity player, Vec3d start, Vec3d end) {
        HitResult hit = world.rayTrace(new net.minecraft.world.RayTraceContext(start, end,
                net.minecraft.world.RayTraceContext.ShapeType.COLLIDER,
                net.minecraft.world.RayTraceContext.FluidHandling.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
    }
    *///?} elif <26 {
    private static Vec3d stopAtBlock(ClientWorld world, ClientPlayerEntity player, Vec3d start, Vec3d end) {
        HitResult hit = world.raycast(new net.minecraft.world.RaycastContext(start, end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
    }
    //?} else {
    /*private static Vec3 stopAtBlock(ClientLevel level, LocalPlayer player, Vec3 start, Vec3 end) {
        HitResult hit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }
    *///?}

    //? if <1.16.2 {
    /*private static Entity closestEntity(ClientWorld world, ClientPlayerEntity player, Vec3d start, Vec3d end) {
        Box searchBox = new Box(start, end).expand(1.0);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : world.getEntities(player, searchBox, candidate -> canTarget(player, candidate))) {
            Box hitBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            Optional<Vec3d> hit = hitBox.rayTrace(start, end);
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
    *///?} elif <26 {
    private static Entity closestEntity(ClientWorld world, ClientPlayerEntity player, Vec3d start, Vec3d end) {
        Box searchBox = new Box(start, end).expand(1.0);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : world.getOtherEntities(player, searchBox, candidate -> canTarget(player, candidate))) {
            Box hitBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            Optional<Vec3d> hit = hitBox.raycast(start, end);
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
    //?} else {
    /*private static Entity closestEntity(ClientLevel level, LocalPlayer player, Vec3 start, Vec3 end) {
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
    *///?}

    /** The quality-of-life half of the mod: don't swing at things you like. */
    //? if <1.19.1 {
    /*private static boolean canTarget(ClientPlayerEntity player, Entity entity) {
        if (entity == player || entity.isSpectator() || !entity.collides()) {
            return false;
        }
        if (entity.isTeammate(player) || isVehicleOf(player, entity)) {
            return false;
        }
        return !(entity instanceof TameableEntity) || !((TameableEntity) entity).isOwner(player);
    }
    *///?} elif <26 {
    private static boolean canTarget(ClientPlayerEntity player, Entity entity) {
        if (entity == player || entity.isSpectator() || !entity.canHit()) {
            return false;
        }
        if (entity.isTeammate(player) || isVehicleOf(player, entity)) {
            return false;
        }
        return !(entity instanceof TameableEntity) || !((TameableEntity) entity).isOwner(player);
    }
    //?} else {
    /*private static boolean canTarget(LocalPlayer player, Entity entity) {
        if (entity == player || entity.isSpectator() || !entity.isPickable()) {
            return false;
        }
        if (entity.isAlliedTo(player) || isVehicleOf(player, entity)) {
            return false;
        }
        return !(entity instanceof TamableAnimal) || !((TamableAnimal) entity).isOwnedBy(player);
    }
    *///?}

    //? if <26 {
    private static boolean isVehicleOf(ClientPlayerEntity player, Entity entity) {
    //?} else {
    /*private static boolean isVehicleOf(LocalPlayer player, Entity entity) {
    *///?}
        for (Entity vehicle = player.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            if (vehicle == entity) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code ActionResult} only grew its convenience methods in 1.15. On 26 it
     * is {@code InteractionResult} - Mojang's name for the same thing, and a
     * sealed interface rather than an enum since 1.21.5.
     */
    //? if <1.15 {
    /*public static boolean accepted(ActionResult result) {
        return result == ActionResult.SUCCESS;
    }
    *///?} elif <26 {
    public static boolean accepted(ActionResult result) {
        return result.isAccepted();
    }
    //?} else {
    /*public static boolean accepted(InteractionResult result) {
        return result instanceof InteractionResult.Success;
    }
    *///?}
}

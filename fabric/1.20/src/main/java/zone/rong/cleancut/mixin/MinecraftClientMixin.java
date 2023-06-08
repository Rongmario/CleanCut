package zone.rong.cleancut.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientWorld world;

    @Redirect(method = "doAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;", ordinal = 0))
    private BlockState onAttack(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return state;
        }
        Entity entity = searchEntityForInteraction(state, world, pos);
        if (entity != null) {
            this.interactionManager.attackEntity(this.player, entity);
            return Blocks.AIR.getDefaultState();
        }
        return state;
    }

    @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock" +
            "(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult onItemUse(ClientPlayerInteractionManager instance, ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        Entity entity = searchEntityForInteraction(this.world.getBlockState(hitResult.getBlockPos()), this.world, hitResult.getBlockPos());
        if (entity != null) {
            ActionResult actionResult = this.interactionManager.interactEntityAtLocation(this.player, entity, new EntityHitResult(entity), hand);
            if (!actionResult.isAccepted()) {
                actionResult = this.interactionManager.interactEntity(this.player, entity, hand);
            }
            if (!actionResult.isAccepted()) {
                return ActionResult.PASS;
            }
            if (actionResult.shouldSwingHand()) {
                this.player.swingHand(hand);
                return ActionResult.FAIL;
            }
        }
        return this.interactionManager.interactBlock(this.player, hand, hitResult);
    }

    private Entity searchEntityForInteraction(BlockState state, World world, BlockPos pos) {
        if (allowBlock(state, world, pos)) {
            float reach = this.interactionManager.getReachDistance();
            Vec3d camera = this.player.getCameraPosVec(1.0F);
            Vec3d rotation = this.player.getRotationVec(1.0F);
            Vec3d end = searchClosestBlock(camera, camera.add(rotation.x * reach, rotation.y * reach, rotation.z * reach), world);
            EntityHitResult result = ProjectileUtil.getEntityCollision(world, this.player, camera, end, new Box(camera, end), entity -> allowEntity(entity));
            return result == null ? null : result.getEntity();
        }
        return null;
    }

    private boolean allowBlock(BlockState state, World world, BlockPos pos) {
        return state.getCollisionShape(world, pos).isEmpty();
    }

    private boolean allowEntity(Entity entity) {
        if (entity.isSpectator()) {
            return false;
        }
        if (!entity.canHit()) {
            return false;
        }
        if (entity instanceof Tameable) {
            UUID ownedUuid = ((Tameable) entity).getOwnerUuid();
            if (ownedUuid != null && ownedUuid.equals(this.player.getUuid())) {
                return false;
            }
        }
        return !getRidingEntities().contains(entity);
    }

    private Vec3d searchClosestBlock(Vec3d from, Vec3d to, World world) {
        HitResult hitResult = world.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, this.player));
        if (hitResult.getType() != Type.MISS) {
            return hitResult.getPos();
        }
        return to;
    }

    private Collection<Entity> getRidingEntities() {
        Collection<Entity> riding = new HashSet<>();
        Entity entity = this.player;
        while (entity.hasVehicle()) {
            entity = entity.getVehicle();
            riding.add(entity);
        }
        return riding;
    }

}

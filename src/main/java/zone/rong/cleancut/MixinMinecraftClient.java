package zone.rong.cleancut;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Shadow @Nullable public ClientWorld world;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public HitResult crosshairTarget;

    @Redirect(method = "doAttack", at = @At(value = "INVOKE",
            target = "net/minecraft/client/network/ClientPlayerInteractionManager.attackBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"))
    private boolean beforeStartingToBreakBlock(ClientPlayerInteractionManager interactionManager, BlockPos pos, Direction direction) {
        BlockState state = world.getBlockState(pos);
        if (state.getCollisionShape(this.world, pos).isEmpty() || state.getHardness(this.world, pos) == 0.0F) {
            float reach = interactionManager.getReachDistance();
            Vec3d camera = player.getCameraPosVec(1.0F);
            Vec3d rotation = player.getRotationVec(1.0F);
            Vec3d end = camera.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);
            EntityHitResult result = ProjectileUtil.raycast(player, camera, end, new Box(camera, end), e -> !e.isSpectator() && e.isAttackable(), reach * reach);
            if (result != null) {
                interactionManager.attackEntity(player, result.getEntity());
                this.player.swingHand(Hand.MAIN_HAND);
                return false;
            }
        }
        BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
        return interactionManager.attackBlock(pos, blockHitResult.getSide());
    }

}

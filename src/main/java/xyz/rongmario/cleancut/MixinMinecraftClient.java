package xyz.rongmario.cleancut;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Shadow public ClientPlayerEntity player;
    @Shadow public ClientWorld world;

    @Redirect(method = "doAttack", at = @At(value = "INVOKE", target = "net/minecraft/client/network/ClientPlayerInteractionManager.attackBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"))
    private boolean checkForEntity(ClientPlayerInteractionManager manager, BlockPos pos, Direction direction) {
        Vec3d camera = player.getCameraPosVec(1.0F);
        Vec3d rotation = player.getRotationVec(1.0F);
        float reach = manager.getReachDistance();
        Vec3d end = camera.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);
        EntityHitResult result = ProjectileUtil.getEntityCollision(world, player, camera, end, new Box(camera, end), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.and(e -> e != null && e.collides() && e instanceof LivingEntity));
        if (result != null) {
            manager.attackEntity(player, result.getEntity());
            return true;
        }
        return manager.attackBlock(pos, direction);
    }

}

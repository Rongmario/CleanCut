package zone.rong.cleancut.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow @Nullable public ClientLevel level;
    @Shadow @Nullable public LocalPlayer player;

    @Shadow @Nullable public MultiPlayerGameMode gameMode;

    @Inject(method = "startAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"),
            locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void beforeStartingToBreakBlock(CallbackInfoReturnable<Boolean> cir, boolean flag, InteractionKeyMappingTriggered inputEvent, Type hitType,
                                            BlockHitResult blockhitresult, BlockPos blockpos) {
        BlockState state = level.getBlockState(blockpos);
        if (state.getCollisionShape(this.level, blockpos).isEmpty() || state.getDestroySpeed(this.level, blockpos) == 0.0F) {
            double reach = player.getAttribute(ForgeMod.REACH_DISTANCE.get()).getValue();
            reach = player.isCreative() ? reach : reach - 0.5F;
            Vec3 from = player.getEyePosition(1.0F);
            Vec3 look = player.getViewVector(1.0F);
            Vec3 to = from.add(look.x * reach, look.y * reach, look.z * reach);
            AABB aabb = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D, 1.0D, 1.0D);
            EntityHitResult newResult = ProjectileUtil.getEntityHitResult(player, from, to, aabb, entity -> !entity.isSpectator() && entity.isAttackable(), reach * reach);
            if (newResult != null) {
                this.gameMode.attack(player, newResult.getEntity());
                cir.setReturnValue(false);
                this.player.swing(InteractionHand.MAIN_HAND);
            }
        }

    }

}

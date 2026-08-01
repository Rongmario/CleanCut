package zone.rong.cleancut.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import zone.rong.cleancut.CleanCut;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow @Nullable public MultiPlayerGameMode gameMode;
    @Shadow @Nullable public LocalPlayer player;
    // 1.14.4 calls the client's level MultiPlayerLevel; 1.15 renamed it to
    // ClientLevel. A shadowed field has to name the type exactly, so unlike
    // CleanCut itself this can't just say Level.
    //? if >=1.15 {
    @Shadow @Nullable public net.minecraft.client.multiplayer.ClientLevel level;
    //?} else {
    /*@Shadow @Nullable public net.minecraft.client.multiplayer.MultiPlayerLevel level;
    *///?}

    /**
     * Vanilla checks whether the targeted block is air before starting to break
     * it. If it isn't air but also isn't solid, we look for an entity behind it
     * and report air, which drops vanilla into its "swing at nothing" path.
     */
    //? if >=1.15 {
    @Redirect(method = "startAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            ordinal = 0))
    private BlockState cleancut$attackThroughBlock(net.minecraft.client.multiplayer.ClientLevel level, BlockPos pos) {
    //?} else {
    /*@Redirect(method = "startAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            ordinal = 0))
    private BlockState cleancut$attackThroughBlock(net.minecraft.client.multiplayer.MultiPlayerLevel level, BlockPos pos) {
    *///?}
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return state;
        }
        Entity entity = CleanCut.findTarget((Minecraft) (Object) this, state, pos);
        if (entity == null) {
            return state;
        }
        this.gameMode.attack(this.player, entity);
        return Blocks.AIR.defaultBlockState();
    }

    //? if >=1.19 {
    @Redirect(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult cleancut$interactThroughBlock(MultiPlayerGameMode gameMode, LocalPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = cleancut$interactEntity(hand, hitResult);
        return result != null ? result : gameMode.useItemOn(player, hand, hitResult);
    }
    //?} elif >=1.15 {
    /*@Redirect(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult cleancut$interactThroughBlock(MultiPlayerGameMode gameMode, LocalPlayer player, net.minecraft.client.multiplayer.ClientLevel level, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = cleancut$interactEntity(hand, hitResult);
        return result != null ? result : gameMode.useItemOn(player, level, hand, hitResult);
    }
    *///?} else {
    /*@Redirect(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/client/multiplayer/MultiPlayerLevel;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult cleancut$interactThroughBlock(MultiPlayerGameMode gameMode, LocalPlayer player, net.minecraft.client.multiplayer.MultiPlayerLevel level, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = cleancut$interactEntity(hand, hitResult);
        return result != null ? result : gameMode.useItemOn(player, level, hand, hitResult);
    }
    *///?}

    /**
     * @return the result of interacting with an entity behind the targeted
     *         block, or {@code null} if there was nothing to interact with.
     */
    @Nullable
    private InteractionResult cleancut$interactEntity(InteractionHand hand, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        Entity entity = CleanCut.findTarget((Minecraft) (Object) this, this.level.getBlockState(pos), pos);
        if (entity == null) {
            return null;
        }
        InteractionResult result = this.gameMode.interactAt(this.player, entity, new EntityHitResult(entity), hand);
        if (!CleanCut.accepted(result)) {
            result = this.gameMode.interact(this.player, entity, hand);
        }
        // Anything vanilla would call a hit stops here - the caller swings for
        // us. Anything else falls through to the block we were pointing at.
        return CleanCut.accepted(result) ? InteractionResult.SUCCESS : null;
    }
}

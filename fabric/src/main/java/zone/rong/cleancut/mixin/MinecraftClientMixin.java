package zone.rong.cleancut.mixin;

//? if <26 {
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
*///?}
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import zone.rong.cleancut.CleanCut;

// Minecraft 26 ships unobfuscated, so from there on the class this targets is
// called Minecraft and not MinecraftClient. Only the names change - it is the
// same class, and this stays MinecraftClientMixin so the mixin config doesn't
// have to know which version it is on.
//? if <26 {
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientWorld world;
//?} else {
/*@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow @Nullable public MultiPlayerGameMode gameMode;
    @Shadow @Nullable public LocalPlayer player;
    @Shadow @Nullable public ClientLevel level;
*///?}

    /**
     * Vanilla checks whether the targeted block is air before starting to break
     * it. If it isn't air but also isn't solid, we look for an entity behind it
     * and report air, which drops vanilla into its "swing at nothing" path.
     */
    //? if <26 {
    @Redirect(method = "doAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
            ordinal = 0))
    private BlockState cleancut$attackThroughBlock(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return state;
        }
        Entity entity = CleanCut.findTarget((MinecraftClient) (Object) this, state, pos);
        if (entity == null) {
            return state;
        }
        this.interactionManager.attackEntity(this.player, entity);
        return Blocks.AIR.getDefaultState();
    }
    //?} else {
    /*@Redirect(method = "startAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            ordinal = 0))
    private BlockState cleancut$attackThroughBlock(ClientLevel level, BlockPos pos) {
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
    *///?}

    //? if <1.19 {
    /*@Redirect(method = "doItemUse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult cleancut$interactThroughBlock(ClientPlayerInteractionManager manager, ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult) {
        ActionResult result = cleancut$interactEntity(hand, hitResult);
        return result != null ? result : manager.interactBlock(player, world, hand, hitResult);
    }
    *///?} elif <26 {
    @Redirect(method = "doItemUse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult cleancut$interactThroughBlock(ClientPlayerInteractionManager manager, ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        ActionResult result = cleancut$interactEntity(hand, hitResult);
        return result != null ? result : manager.interactBlock(player, hand, hitResult);
    }
    //?} else {
    /*@Redirect(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult cleancut$interactThroughBlock(MultiPlayerGameMode gameMode, LocalPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = cleancut$interactEntity(hand, hitResult);
        return result != null ? result : gameMode.useItemOn(player, hand, hitResult);
    }
    *///?}

    /**
     * @return the result of interacting with an entity behind the targeted
     *         block, or {@code null} if there was nothing to interact with.
     */
    @Nullable
    //? if <26 {
    private ActionResult cleancut$interactEntity(Hand hand, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        Entity entity = CleanCut.findTarget((MinecraftClient) (Object) this, this.world.getBlockState(pos), pos);
        if (entity == null) {
            return null;
        }
        ActionResult result = this.interactionManager.interactEntityAtLocation(this.player, entity, new EntityHitResult(entity), hand);
        if (!CleanCut.accepted(result)) {
            result = this.interactionManager.interactEntity(this.player, entity, hand);
        }
        // Anything vanilla would call a hit stops here - the caller swings for
        // us. Anything else falls through to the block we were pointing at.
        return CleanCut.accepted(result) ? ActionResult.SUCCESS : null;
    }
    //?} else {
    /*private InteractionResult cleancut$interactEntity(InteractionHand hand, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        Entity entity = CleanCut.findTarget((Minecraft) (Object) this, this.level.getBlockState(pos), pos);
        if (entity == null) {
            return null;
        }
        // 26 merged the two: interact now takes the hit result, and the
        // separate interactAt is gone.
        InteractionResult result = this.gameMode.interact(this.player, entity, new EntityHitResult(entity), hand);
        // Anything vanilla would call a hit stops here - the caller swings for
        // us. Anything else falls through to the block we were pointing at.
        return CleanCut.accepted(result) ? InteractionResult.SUCCESS : null;
    }
    *///?}
}

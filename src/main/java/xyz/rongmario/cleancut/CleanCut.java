package xyz.rongmario.cleancut;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod("cleancut")
public class CleanCut {

    public CleanCut() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onLeftClickBlock);
    }

    @SuppressWarnings("ConstantConditions")
    private void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        System.out.println("Clicked");
        Level world = event.getWorld();
        if (!world.isClientSide) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState clicked = event.getWorld().getBlockState(event.getPos());
        if (!clicked.getCollisionShape(world, pos).isEmpty() || clicked.getDestroySpeed(world, pos) != 0.0F) {
            return;
        }
        Player player = event.getPlayer();
        double reach = player.getAttribute(ForgeMod.REACH_DISTANCE.get()).getValue();
        reach = player.isCreative() ? reach : reach - 0.5F;
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 to = from.add(look.x * reach, look.y * reach, look.z * reach);
        AABB aabb = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult result = ProjectileUtil.getEntityHitResult(player, from, to, aabb, entity -> !entity.isSpectator() && entity.isAttackable(), reach * reach);
        if (result != null) {
            event.setCanceled(true);
            // player.attackTargetEntityWithCurrentItem(result.getEntity());
            Minecraft.getInstance().gameMode.attack(player, result.getEntity());
        }
    }

}
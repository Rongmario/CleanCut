package xyz.rongmario.cleancut;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod("cleancut")
public class CleanCut {

    public CleanCut() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onLeftClickBlock);
    }

    private void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        PlayerEntity player = event.getPlayer();
        Vec3d from = player.getEyePosition(1.0F);
        Vec3d look = player.getLook(1.0F);
        double reach = player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue();
        reach = player.isCreative() ? reach : reach - 0.5F;
        Vec3d to = from.add(look.x * reach, look.y * reach, look.z * reach);
        EntityRayTraceResult result = ProjectileHelper.rayTraceEntities(player.world, player, from, to, new AxisAlignedBB(from, to), EntityPredicates.CAN_AI_TARGET.and(e -> e != null && e.canBeCollidedWith() && e instanceof LivingEntity && !(e instanceof FakePlayer)));
        if (result != null) {
            event.setCanceled(true);
            player.attackTargetEntityWithCurrentItem(result.getEntity());
        }
    }

}

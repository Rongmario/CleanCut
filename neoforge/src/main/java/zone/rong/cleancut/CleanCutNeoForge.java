package zone.rong.cleancut;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge insists on an entrypoint even for a mod that is nothing but a mixin.
 *
 * <p>{@code @Mod} only learned to declare a side in NeoForge 21 (Minecraft
 * 1.21). Before that the mod metadata is the only place that says client-only,
 * which it does.
 */
//? if <1.21 {
@Mod("cleancut")
//?} else {
/*@Mod(value = "cleancut", dist = Dist.CLIENT)
*///?}
public class CleanCutNeoForge {

    public CleanCutNeoForge() {
    }
}

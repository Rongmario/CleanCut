package zone.rong.cleancut;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge insists on an entrypoint even for a mod that is nothing but a mixin.
 */
@Mod(value = "cleancut", dist = Dist.CLIENT)
public class CleanCutNeoForge {

    public CleanCutNeoForge() {
    }
}

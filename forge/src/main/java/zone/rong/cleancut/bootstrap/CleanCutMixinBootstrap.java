package zone.rong.cleancut.bootstrap;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Starts Mixin on the Forge versions that don't ship it - 1.14.4, 1.15 and
 * 1.15.1. From 1.15.2 (Forge 31.2) onwards Forge does all of this itself and
 * this class isn't compiled into the jar at all; see {@code shadesMixin} in
 * {@code forge/build.gradle}.
 *
 * <p>The mechanism: Forge's mod directory scanner reads
 * {@code META-INF/services} out of the jars in {@code mods/} before the game
 * starts, so a service declared here is picked up even though the mod itself
 * hasn't loaded yet. Mixin's own launch plugin arrives the same way, out of the
 * copy of Mixin shaded alongside this class. All that's left for us is to run
 * Mixin's bootstrap and hand it the mod's config.
 */
public final class CleanCutMixinBootstrap implements ITransformationService {

    @Override
    public String name() {
        return "cleancut";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public void beginScanning(IEnvironment environment) {
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        MixinBootstrap.init();
        Mixins.addConfiguration("cleancut.mixins.json");
    }

    @Override
    public List<ITransformer> transformers() {
        return Collections.emptyList();
    }
}

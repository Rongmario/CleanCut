package zone.rong.cleancut.bootstrap;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Starts Mixin on the Forge versions that don't ship it - 1.14.4, 1.15 and
 * 1.15.1. From 1.15.2 (Forge 31.2) onwards Forge does all of this itself and
 * this class isn't compiled into the jar at all; see {@code shadesMixin} in
 * {@code forge/build.gradle}.
 *
 * <p>Forge's mod directory scanner reads {@code META-INF/services} out of the
 * jars in {@code mods/} before the game starts, which is what gets this class
 * loaded at all. Everything from there is working against ModLauncher rather
 * than with it, because on these versions one jar being both the thing that
 * provides Mixin and an ordinary mod is not a case anybody designed for. The
 * usual answer is a second jar - ZekerZhayard's MixinBootstrap - which we'd
 * rather not make people install, so the two things that second jar gets for
 * free are done by hand below.
 */
public final class CleanCutMixinBootstrap implements ITransformationService {

    @Override
    public String name() {
        return "cleancut";
    }

    /**
     * Registering the launch plugin isn't enough on its own: the plugin holds a
     * reference to the Mixin service that it only picks up when something calls
     * its {@code init}, and until then every call into it fails on a null. On a
     * Forge that ships Mixin, the caller is Mixin's own transformation service,
     * which ModLauncher runs because Mixin is on the class path. Ours isn't, so
     * that service was never found - but the class is right here in the jar, so
     * run it ourselves rather than reproducing what it does.
     *
     * <p>It expects to find the launch plugin registered by the time this runs,
     * which is why that happens back in {@link #onLoad}.
     */
    @Override
    public void initialize(IEnvironment environment) {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(CleanCutMixinBootstrap.class.getClassLoader());
        try {
            Class<?> serviceClass = Class.forName("org.spongepowered.asm.launch.MixinTransformationServiceLegacy",
                    true, CleanCutMixinBootstrap.class.getClassLoader());
            ITransformationService mixin = (ITransformationService) serviceClass.getDeclaredConstructor().newInstance();
            mixin.initialize(environment);
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("CleanCut could not initialise Mixin", e);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Override
    public void beginScanning(IEnvironment environment) {
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        // Mixin looks its platform agents and container handles up by name, and
        // its ModLauncher class provider resolves those names against the
        // context class loader. This early in startup that is still the system
        // loader, which cannot see a jar in mods/ - so the lookups miss the very
        // copy of Mixin that is running them, the root container comes back null
        // and MixinPlatformManager throws. Classes Mixin references directly are
        // fine, because those link through whichever loader read this class out
        // of the jar. Pointing the context loader at that same loader makes the
        // lookups agree with the links. Restored afterwards: it belongs to
        // ModLauncher, not us.
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(CleanCutMixinBootstrap.class.getClassLoader());
        try {
            MixinBootstrap.init();
            Mixins.addConfiguration("cleancut.mixins.json");
            registerMixinLaunchPlugin();
            stopForgeSkippingThisJar();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /**
     * Mixin transforms classes through a ModLauncher launch plugin, and
     * ModLauncher collects those from the system class path in its own
     * constructor - before any jar in {@code mods/} has been looked at. On the
     * versions where Forge ships Mixin that's fine, because Mixin is a library
     * and is already there. Ours isn't, so by the time we run, the plugin list
     * is built and Mixin isn't in it: mixin configs get queued and nothing ever
     * applies them, quietly.
     *
     * <p>So add it after the fact. The map behind the handler is a plain
     * {@code HashMap} and nothing has read it yet at this point - transformation
     * only starts once the transforming class loader exists, which is later.
     */
    private void registerMixinLaunchPlugin() throws IncompatibleEnvironmentException {
        try {
            Field handlerField = Launcher.class.getDeclaredField("launchPlugins");
            handlerField.setAccessible(true);
            Object handler = handlerField.get(Launcher.INSTANCE);

            Map<String, ILaunchPluginService> plugins = pluginMap(handler);
            if (plugins.containsKey("mixin")) {
                // Something else already brought Mixin - MixinBootstrap, or a
                // Forge that turned out to ship it after all. Leave it alone.
                return;
            }

            // Mixin's own launch plugin, declared in the copy of Mixin shaded
            // into this jar. Loaded through our loader because that is the only
            // one that can see it.
            Class<?> pluginClass = Class.forName("org.spongepowered.asm.launch.MixinLaunchPluginLegacy",
                    true, CleanCutMixinBootstrap.class.getClassLoader());
            ILaunchPluginService plugin = (ILaunchPluginService) pluginClass.getDeclaredConstructor().newInstance();
            plugins.put(plugin.name(), plugin);
        } catch (ReflectiveOperationException | ClassCastException e) {
            // Without the plugin the mod does nothing at all, and does it
            // silently. Better to say so.
            throw new IncompatibleEnvironmentException(
                    "CleanCut could not register Mixin with ModLauncher: " + e);
        }
    }

    /**
     * The handler keeps its plugins in its only {@code Map} field. Found by type
     * rather than by name so that a rename between 1.14.4 and 1.15.1 doesn't
     * matter - there is exactly one, on every version this runs on.
     */
    @SuppressWarnings("unchecked")
    private Map<String, ILaunchPluginService> pluginMap(Object handler) throws ReflectiveOperationException {
        for (Field field : handler.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return (Map<String, ILaunchPluginService>) field.get(handler);
            }
        }
        throw new NoSuchFieldException("no plugin map on " + handler.getClass().getName());
    }

    /**
     * A jar in {@code mods/} that declares a transformation service is treated
     * by Forge as a transformer and nothing else: it gets recorded as excluded,
     * and the mod locator then skips it. Ours declares one - that's how this
     * class got loaded - so without this the mod is never scanned. No
     * {@code mods.toml}, no entry in the mod list, and the mixin classes never
     * reach the class loader that would apply them.
     *
     * <p>Dropping ourselves from that exclusion list puts the jar back in front
     * of the mod locator. Safe to do here because Forge builds the list while
     * discovering services, which has already happened, and reads it during
     * scanning, which hasn't - it copies the list on each read.
     */
    private void stopForgeSkippingThisJar() {
        Path self = ownJar();
        if (self == null) {
            return;
        }
        try {
            Class<?> discoverer = Class.forName("net.minecraftforge.fml.loading.ModDirTransformerDiscoverer",
                    true, CleanCutMixinBootstrap.class.getClassLoader());
            Field field = discoverer.getDeclaredField("transformers");
            field.setAccessible(true);
            Object value = field.get(null);
            if (!(value instanceof List)) {
                return;
            }
            for (Iterator<?> it = ((List<?>) value).iterator(); it.hasNext(); ) {
                Object entry = it.next();
                if (entry instanceof Path && sameFile((Path) entry, self)) {
                    it.remove();
                }
            }
        } catch (ReflectiveOperationException | UnsupportedOperationException e) {
            // Not fatal on its own: Mixin still runs, the mod just won't be
            // listed. Loud enough to find, quiet enough not to stop the game.
            System.err.println("CleanCut could not re-register itself as a mod: " + e);
        }
    }

    private static boolean sameFile(Path left, Path right) {
        return left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }

    /** The jar this class was loaded from, or {@code null} if that's not a file. */
    private static Path ownJar() {
        try {
            return Paths.get(CleanCutMixinBootstrap.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<ITransformer> transformers() {
        return Collections.emptyList();
    }
}

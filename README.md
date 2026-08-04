# CleanCut

Fight enemies back through grass and other similar obstructions!

Point at a mob standing in tall grass, in flowers, behind a torch — anything
without real collision — and CleanCut hits the mob instead of swinging at the
plant. The same applies to right-clicking: you can shear the sheep standing in
the wheat. It leaves alone the things you didn't mean to hit: your tamed pets,
anything on your team, and whatever you're currently riding.

Client-side only. It does nothing on a server and doesn't need to be installed
on one.

## Supported versions

Every loader and every Minecraft version is built from this one branch.

Every Minecraft release each loader ever shipped for, from 1.14.4 to the
current snapshot — 95 jars in all.

| Loader   | Minecraft versions                                                            |
|----------|-------------------------------------------------------------------------------|
| Fabric   | 1.14.4, 1.15–1.15.2, 1.16–1.16.5, 1.17–1.17.1, 1.18–1.18.2, 1.19–1.19.4, 1.20–1.20.6, 1.21–1.21.11, 26.1–26.1.2, 26.2, 26.3-snapshot-7 |
| Quilt    | same jars as Fabric                                                           |
| Forge    | 1.14.4, 1.15–1.15.2, 1.16.3–1.16.5, 1.17.1, 1.18–1.18.2, 1.19–1.19.4, 1.20–1.20.4, 1.20.6, 1.21, 1.21.1, 1.21.3–1.21.11 |
| NeoForge | 1.20.2–1.20.6, 1.21–1.21.9, 26.1–26.1.2, 26.2                                 |

Quilt Loader reads `fabric.mod.json` directly, so the Fabric jar *is* the Quilt
jar — there's nothing extra to build, and releases are tagged for both.

Most gaps are releases that loader never shipped for: Forge went straight from
1.16 to 1.16.1 and from 1.17 to 1.17.1, and skipped 1.20.5 and 1.21.2 entirely.

Forge 1.20.3 is also missing: its recommended build asks for
`net.minecraftforge:bootstrap-dev:2.0.0`, which was never published. A later
build off the 49 line may well fix it. NeoForge covers 1.20.3 either way.

Forge 1.16.1 and 1.16.2 are the exception — Forge shipped them, but the
Architectury Loom this build uses cannot produce them on any Forge build: 1.16.1
dies remapping Minecraft on a name conflict, and on 1.16.2 the mixin annotation
processor is given no SRG mappings and so can't write a refmap. Fabric covers
both versions.

Nothing here ships a refmap any more. Current Loom defaults to
`useLegacyMixinAp = false`: instead of running Mixin's annotation processor to
emit a name map alongside the jar, it rewrites the mixin annotations themselves
into SRG inside `remapJar`. The jars are equivalent — the mapping is in the
class files rather than in a JSON file next to them — but it means
`cleancut.mixins.json` must *not* declare a `refmap`, and no build script here
may add Mixin as an `annotationProcessor`. Doing either points the build at
mapping data that is never generated.

On Forge 1.14.4, 1.15 and 1.15.1 the mod carries its own copy of Mixin, because
Forge only started bundling Mixin partway through 1.15.2. See
`forge/src/main/java/zone/rong/cleancut/bootstrap/`.

Shipping Mixin inside the mod jar rather than as a second file — the job
MixinBootstrap usually does — means doing by hand the three things a jar on the
class path gets for free. ModLauncher builds its launch plugin list before it
looks at `mods/`, so Mixin's plugin is registered afterwards; Forge treats a jar
declaring a transformation service as a transformer and not a mod, so the jar
takes itself back off that exclusion list; and Mixin resolves its own platform
classes through the context class loader, which this early can't see `mods/`.
`asm-util` is shaded alongside for the same reason — Mixin needs it and these
Forge versions don't ship it.

None of this is compiled into 1.15.2 or later, where Forge starts Mixin itself.
It is reflection into ModLauncher and FML internals, so these three targets are
worth launching after a toolchain change; building them only proves they
compile.

NeoForge starts at 1.20.2 because that is its first release. Its 1.20.1 build is
the Forge fork from before the rename and still lives under
`net.minecraftforge`, so the **Forge 1.20.1 jar is the NeoForge 1.20.1 jar** —
it's the same API, and building it twice would produce the same mod.

Only one snapshot is carried at a time, the current one. When the next lands,
the row moves rather than accumulating — a snapshot nobody can download any
more is not worth a build job.

The ceilings differ per loader — 1.21.11 on Forge, 26.2 on NeoForge, the
current snapshot on Fabric — for the reasons described under
[Newer Minecraft](#newer-minecraft).

## How the repository is laid out

```
settings.gradle          the version/loader matrix
stonecutter.gradle       which version the checked-out sources are written for
fabric/                  build script, mod metadata and Yarn-mapped sources
forge/                   build script, mod metadata and Mojang-mapped sources
neoforge/                same, for NeoForge
.github/targets.json     the same matrix again, for CI
```

[Stonecutter](https://stonecutter.codeberg.page/) turns one source tree into
every Minecraft version. Where an API moved between versions, the alternatives
sit next to each other and Stonecutter comments out the ones that don't apply:

```java
//? if >=1.17 {
for (Entity entity : world.getOtherEntities(player, searchBox, predicate)) {
//?} else {
/*for (Entity entity : world.getEntities(player, searchBox, predicate)) {
*///?}
```

Fabric builds against Yarn mappings and Forge/NeoForge against Mojang's own, so
those two source trees are separate copies of the same behaviour rather than one
shared file full of mapping conditionals. Each is small: a `CleanCut` class with
the logic, and a mixin that decides where vanilla gets interrupted.

The exception is Minecraft 26 on Fabric, where there is no Yarn to build
against and the same source tree has to speak both. There the Yarn and Mojang
spellings sit next to each other member by member, in the same `//? if <26`
form as everything else. Those blocks are never nested inside one another: where
a member also varies by version within Yarn, the alternatives are written as one
flat `if`/`elif`/`else` chain instead, so each arm stays a whole method you can
read.

## Building

```sh
./gradlew build                        # everything
./gradlew :fabric:1.20.1:build         # one target
./gradlew :neoforge:1.21.4:build
```

Jars land in `<loader>/versions/<version>/build/libs/`. Build with **JDK 21**,
except for the Minecraft 26 targets, which need **JDK 25**:

```sh
./gradlew :neoforge:26.2:build      # needs JAVA_HOME on a JDK 25
```

That is the JVM the Gradle daemon runs on, not what the mod compiles against —
each target sets its own `options.release`, so one daemon covers every release
level from 8 upwards. Loom refuses to set Minecraft 26 up under anything below
25 (`Minecraft 26.1 requires Java 25 but Gradle is using 21`), and the older
targets have only been exercised on 21. `.github/targets.json` carries the
version each target wants, and CI installs it per job. Set `JAVA_HOME` to
switch locally.

`./gradlew build` with no arguments builds every target in sequence, which takes
a while — the per-version Minecraft decompile dominates. CI builds them in
parallel instead, one job per target.

## Working on the code

The sources in `src/` are written for one version at a time — 1.20.2, the
version every branch has in common — and that's the version an IDE resolves
them against. To read and edit them as another version instead:

```sh
./gradlew "Set active project to 1.19.2"
./gradlew "Reset active project"       # back to 1.20.2
```

Building doesn't need either: each target's sources are generated into its own
`build/generated/stonecutter/` and `src/` is left alone, so building any number
of versions leaves `git status` clean. Note that both tasks above configure
every target, which is slow — they're for IDE work, not part of building.

## Newer Minecraft

This build is on Gradle 9, Stonecutter 0.9 and current Loom, because Minecraft
26 needs a Java 25 daemon and Gradle 8 cannot run on Java 25 at all — it rejects
the class files. The wrapper and the Stonecutter version are properties of the
whole build rather than of one branch, so that migration was all-or-nothing.

The obvious worry about moving the whole build forward is that current Loom
drops the oldest targets, and it very nearly did. Forge 1.14.4, 1.15 and 1.15.1
— the three that shade their own Mixin — failed with `Unable to locate
obfuscation mapping for @Redirect target`. The cause was on this side, not
Loom's: those three added Mixin as an `annotationProcessor` by hand, and current
Loom no longer passes the processor any mapping arguments because it no longer
uses the processor. Dropping those lines fixes all three. See the note on
refmaps under [Supported versions](#supported-versions), and don't reintroduce
them.

Every other target from 1.14.4 up builds unchanged, so there is one build here,
not a legacy one and a modern one.

What is left is per-loader, and none of it is a toolchain problem:

**NeoForge skips 1.21.10 and 1.21.11.** From 21.10 its published artifact no
longer carries `data/server.lzma` where Architectury Loom expects it. The
packaging moved out from under Loom, so a version bump on this side doesn't fix
it — building those two means NeoForge's own toolchain instead of Loom, which is
a different build rather than a different number. It picks back up at 26.1.
Fabric and Forge cover 1.21.10 and 1.21.11.

**Forge stops at 1.21.11.** On 26.x Loom throws an NPE setting Minecraft up on
the Forge platform. NeoForge 26.x goes through the same Loom and works, so this
is Forge-platform-specific.

**Fabric goes all the way, including the current snapshot.** 26.x has no Yarn —
intermediary exists, Yarn does not — because the game ships unobfuscated and the
names in the jar are Mojang's. So the Fabric sources carry both spellings, as
described under [How the repository is laid out](#how-the-repository-is-laid-out),
and the build drops the `mappings` dependency and switches Loom to its no-remap
plugin for those targets. The jar off the compiler is the jar that ships:
`remapJar` has nothing to remap.

Fabric Loader normalises Minecraft's version ids into its own semver, and
snapshots don't survive that intact — `26.3-snapshot-7` is `26.3-alpha.7` to the
loader. `fabric.mod.json` gets the loader's spelling, or the mod would refuse to
load on the version it was built for.

Yarn's unpick v3 is *not* a wall, despite an earlier note here saying so.

**NeoForge stops at 26.2, because that is as far as NeoForge goes.** There is no
26.3 build on their Maven, snapshot or otherwise, so there is nothing to build
against — this one is upstream, not here. Run the **Loader builds** workflow
against `26.3-snapshot-7` to see whether that has changed; if it has, adding it
is the three steps below.

## Adding a Minecraft version

1. Add it to the right `branch(...)` in `settings.gradle`.
2. Add a row to that loader's `versionData` table in `<loader>/build.gradle`
   with the mappings or loader build and the Java release level. The dependency
   ranges that go into the mod metadata are derived from those, not written out.
3. Add it to `.github/targets.json` so CI builds it, with the `java` the Gradle
   daemon needs for it — 21 for everything so far except Minecraft 26, which
   needs 25.

The build number step 2 wants is a fact about someone else's repository, so
don't guess it: run the **Loader builds** workflow from the Actions tab with the
Minecraft version you're adding, and it prints what Mojang, Fabric, NeoForge and
Forge have actually published for it.

Then build it. If an API moved, the compiler will say so, and the fix is another
`//? if` block around the two alternatives.

## Publishing

Releases go to Modrinth and CurseForge from `.github/workflows/publish.yml`,
driven by [mod-publish-plugin](https://github.com/modmuss50/mod-publish-plugin).
Both are wired up: the project IDs are in `gradle.properties` (`modrinth_id`,
`curseforge_id`) and the `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` secrets are set
on the repository. Clearing an ID makes that platform get skipped rather than
fail.

To cut a release:

1. Bump `mod_version` in `gradle.properties` and push it.
2. Run the workflow manually with `dry_run` ticked. That builds and validates
   every upload without sending anything.
3. Tag and publish a GitHub release as `v<mod_version>`. The release body
   becomes the changelog on both platforms.

Snapshot targets go to Modrinth only, marked alpha. CurseForge has no game
version to file them under.

The tag has to match `mod_version`, and the workflow stops before uploading
anything if it doesn't — a jar carries the version it was built with, and a
file published to CurseForge can't be replaced afterwards.

## License

MIT.

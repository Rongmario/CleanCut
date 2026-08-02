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

Every Minecraft release each loader ever shipped for, from 1.14.4 to 1.21.11 —
86 jars in all.

| Loader   | Minecraft versions                                                            |
|----------|-------------------------------------------------------------------------------|
| Fabric   | 1.14.4, 1.15–1.15.2, 1.16–1.16.5, 1.17–1.17.1, 1.18–1.18.2, 1.19–1.19.4, 1.20–1.20.6, 1.21–1.21.11 |
| Quilt    | same jars as Fabric                                                           |
| Forge    | 1.14.4, 1.15–1.15.2, 1.16.3–1.16.5, 1.17.1, 1.18–1.18.2, 1.19–1.19.4, 1.20–1.20.4, 1.20.6, 1.21, 1.21.1, 1.21.3–1.21.11 |
| NeoForge | 1.20.2–1.20.6, 1.21–1.21.9                                                    |

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

1.21.8 is the upper limit for every loader, for the toolchain reason described
under [Newer Minecraft](#newer-minecraft).

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

## Building

```sh
./gradlew build                        # everything
./gradlew :fabric:1.20.1:build         # one target
./gradlew :neoforge:1.21.4:build
```

Jars land in `<loader>/versions/<version>/build/libs/`. Build with **JDK 21** —
each target compiles down to the release level its Minecraft version needs, so
one JDK covers all 82. It has to be 21 and not something newer: this build is on
Gradle 8, which rejects JDK 25 with `Unsupported class file major version 69`.
Set `JAVA_HOME` if your default `java` is a later release.

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

The ceiling is 1.21.11 on Fabric and Forge, and 1.21.9 on NeoForge. Three
separate walls sit past that, and they are not the same wall.

**NeoForge stops at 1.21.9.** From 21.10 its published artifact no longer
carries `data/server.lzma` where Architectury Loom expects it, on both Loom
1.10 and 1.11. The packaging moved out from under Loom rather than Loom falling
behind, so a version bump on this side doesn't fix it — building 21.10+ means
NeoForge's own toolchain instead of Loom, which is a different build, not a
different number. Fabric and Forge still cover 1.21.10 and 1.21.11.

**26.1 and later need Java 25.** Loom refuses outright: `Minecraft 26.1
requires Java 25 but Gradle is using 21`. Gradle 8 cannot itself run on Java 25
— it rejects the class files — so this is a Gradle 9 migration, and it applies
to all three loaders at once. That is the real cost of the new version scheme,
and it lands before any mod code is even looked at.

**Fabric additionally has no Yarn for 26.x.** Intermediary exists, Yarn does
not. The Fabric sources here are written in Yarn names, so 26.x on Fabric means
either Mojang mappings — and the class names in `fabric/src` change wholesale,
since `MinecraftClient` becomes `Minecraft` and so on — or nothing. Forge and
NeoForge are already Mojang-mapped and don't have this problem.

26.3-snapshot-6 has no Forge or NeoForge build at all yet, so it is Fabric-only
even after the above.

What is *not* a wall, despite an earlier note here saying so: Yarn's unpick v3.
Loom 1.13.6 reads it on Gradle 8 quite happily, which is what got 1.21.9 to
1.21.11 built. Loom 1.16 and later are the ones that demand Gradle 9.

Getting to 26.x means moving the build to Gradle 9, Stonecutter 0.9 and current
Loom, on a Java 25 daemon — which risks the 1.14–1.16 targets, whose support in
current Loom is unverified — or keeping this build as it is and adding a second,
modern one beside it in the same branch.

## Adding a Minecraft version

1. Add it to the right `branch(...)` in `settings.gradle`.
2. Add a row to that loader's `versionData` table in `<loader>/build.gradle`
   with the mappings or loader build and the Java release level. The dependency
   ranges that go into the mod metadata are derived from those, not written out.
3. Add it to `.github/targets.json` so CI builds it.

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

The tag has to match `mod_version`, and the workflow stops before uploading
anything if it doesn't — a jar carries the version it was built with, and a
file published to CurseForge can't be replaced afterwards.

## License

MIT.

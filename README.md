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

Every Minecraft release each loader ever shipped for, from 1.14.4 to 1.21.8 —
80 jars in all.

| Loader   | Minecraft versions                                                            |
|----------|-------------------------------------------------------------------------------|
| Fabric   | 1.14.4, 1.15–1.15.2, 1.16–1.16.5, 1.17–1.17.1, 1.18–1.18.2, 1.19–1.19.4, 1.20–1.20.6, 1.21–1.21.8 |
| Quilt    | same jars as Fabric                                                           |
| Forge    | 1.14.4, 1.15–1.15.2, 1.16.3–1.16.5, 1.17.1, 1.18–1.18.2, 1.19–1.19.4, 1.20–1.20.4, 1.20.6, 1.21, 1.21.1, 1.21.3–1.21.8 |
| NeoForge | 1.20.2–1.20.6, 1.21–1.21.8                                                    |

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

1.21.8 is where this build stops, and the reason is the toolchain rather than
the mod. From 1.21.9 Yarn ships unpick v3 data, which needs Fabric Loom 1.17,
which needs Gradle 9 — and the Gradle wrapper and the Stonecutter version are
properties of the whole build, not of one branch, so this can't be mixed with
the Gradle 8 stack the older versions are on.

Forge and NeoForge do have 1.21.9+ builds, and being Mojang-mapped they never
touch unpick. They stop at 1.21.8 anyway so that all three loaders cover the
same range, and because the Architectury Loom pinned here predates those
versions. Raising their ceiling is a row in the `versionData` table plus
whatever the compiler then complains about — worth doing on its own, rather
than mixed into a Gradle 9 migration.

The same wall stands in front of Minecraft's new version scheme (26.1.2 and
later), which additionally has no Yarn mappings at all and would have to be
built against Mojang's.

Getting there means either moving the entire build to Gradle 9, Stonecutter
0.9 and current Loom — which risks the 1.14-1.16 targets, whose support in
current Loom is unverified — or keeping this build as it is and adding a
second, modern Gradle build beside it in the same branch.

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
One-time setup:

1. Put the project IDs in `gradle.properties` (`modrinth_id`, `curseforge_id`).
   An empty ID makes that platform get skipped rather than fail.
2. Add `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` as repository secrets.

Publishing a GitHub release then uploads every target, using the release body as
the changelog. To rehearse it, run the workflow manually with `dry_run` ticked —
that builds and validates the uploads without sending anything.

## License

MIT.

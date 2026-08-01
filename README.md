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

| Loader   | Minecraft versions                                                                             |
|----------|------------------------------------------------------------------------------------------------|
| Fabric   | 1.14.4, 1.15.2, 1.16.5, 1.17.1, 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21.1, 1.21.4 through 1.21.8 |
| Quilt    | same jars as Fabric                                                                            |
| Forge    | 1.18.2, 1.19.2, 1.19.4, 1.20.1                                                                 |
| NeoForge | 1.21.1, 1.21.4                                                                                 |

Quilt Loader reads `fabric.mod.json` directly, so the Fabric jar *is* the Quilt
jar — there's nothing extra to build, and releases are tagged for both.

Only the last patch of each Minecraft cycle is built, because that's what
Fabric, Forge and NeoForge themselves support. Adding an intermediate release is
a two-line change (see below).

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

Jars land in `<loader>/versions/<version>/build/libs/`. Any JDK 21 works — each
target compiles to the release level its Minecraft version needs.

## Working on the code

The checked-out sources are written for one version at a time. Switch with:

```sh
./gradlew "Set active project to 1.19.2"
./gradlew "Reset active project"       # back to 1.20.1, run before committing
```

Building a version also switches the sources to it, so if `git status` shows
churn in `src/` after a build, reset the active project and it will go away.

## Newer Minecraft

1.21.8 is where this build stops, and the reason is the toolchain rather than
the mod. From 1.21.9 Yarn ships unpick v3 data, which needs Fabric Loom 1.17,
which needs Gradle 9 — and the Gradle wrapper and the Stonecutter version are
properties of the whole build, not of one branch, so this can't be mixed with
the Gradle 8 stack the older versions are on.

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
   with the mappings/loader version and the Java release level.
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

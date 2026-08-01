# BlueMap-Zones

A [PaperMC](https://papermc.io/) plugin that turns [BlueMap](https://bluemap.bluecolored.de/) shape markers into in-world zones, announcing zone transitions to players as they cross boundaries.

Draw a polygon on your BlueMap web map, and BlueMap-Zones converts it into a chunk-accurate region in game. Players see a title card when they walk into it.

## Requirements

| | |
|---|---|
| Server | Paper 1.21+ |
| Java | 21 |
| Dependency | [BlueMap](https://github.com/BlueMap-Minecraft/BlueMap) (required, must be installed) |

## Installation

1. Download the latest `BlueMap-Zones-*.jar` from the [Releases](https://github.com/SkyeRangerDelta/BlueMap-Zones/releases) page.
2. Drop it into your server's `plugins/` directory alongside BlueMap.
3. Start the server once to generate the config files.
4. Edit `plugins/BlueMap-Zones/BMZ-Config.yml` to point at your map and marker set.
5. Run `/bmz-generate` (or restart) to build the zones.

## Configuration

Two files are generated in `plugins/BlueMap-Zones/`.

### `BMZ-Config.yml`

```yaml
Wilderness-Name: Wilderness   # Title shown outside every known zone
Maps:
  name: world                 # BlueMap map id to read markers from
  marker-sets: []             # Marker set id to load (currently only the first entry is used)
```

`Maps.name` must match the BlueMap **map id**, not the world's display name. `marker-sets` is a list, but at present only the first entry is read.

### `BMZ-NoticeExclusions.yml`

```yaml
Exclusions: []                # Player UUIDs opted out of zone notices
```

Managed automatically by `/bmz-toggle-notices` — no need to edit by hand.

## Commands

| Command | Permission | Default | Description |
|---|---|---|---|
| `/bmz-generate` | `bluemapzones.generate` | op | Rebuild zones from the configured marker set |
| `/bmz-toggle-notices` | `bluemapzones.toggle-notices` | everyone | Toggle zone change notices for yourself |
| `/bmz-reload-conf` | `bluemapzones.reload` | op | Reload the plugin configuration |

`bluemapzones.*` grants all three.

There is also a debug tool: right-click a block while holding a **compass** to print the chunk coordinates and the zone that owns them.

## How it works

BlueMap polygons are rasterized into 16×16 chunk IDs rather than block coordinates. The generator walks each marker's vertices, converts them to chunk IDs, and fills gaps between non-adjacent vertices with a Bresenham line so the boundary ring is chunk-contiguous. Generation runs off the main server thread.

Only the boundary ring is stored. Whether a player is *inside* a zone is resolved at runtime by casting four cardinal rays and checking which zone they all hit. A chunk that belongs to more than one shape is marked *conflicted*, and the plugin tracks each player's last non-conflicted zone so crossing a shared border doesn't produce a spurious re-announcement.

## Building from source

```bash
./gradlew build
```

The plugin JAR lands in `build/libs/`. Requires JDK 21; the Gradle wrapper handles the rest. Paper and BlueMap are `compileOnly` dependencies, so nothing is shaded into the output.

## Contributing

Branches: `master` is production, `development` is staging. Open pull requests against `development`.

Releases are automated with [semantic-release](https://semantic-release.gitbook.io/). Commit messages determine the version bump — either the project's legacy prefixes or [Conventional Commits](https://www.conventionalcommits.org/) works:

| Prefix | Bump |
|---|---|
| `Breaking:` / `feat!:` / `BREAKING CHANGE:` footer | major |
| `New:` / `feat:` | minor |
| `Fix:` / `fix:` / `Refactor:` | patch |
| `Chore:` / `CI:` / `Build:` | no release |

Merging to `development` publishes a release candidate (`-rc.N`); merging to `master` publishes a stable release. Both attach the built JAR to the GitHub Release.

## Author

[SkyeRangerDelta](https://github.com/SkyeRangerDelta) — [pldyn.net](https://pldyn.net/)

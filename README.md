# morphe-screenshot-patches

Personal [Morphe](https://morphe.software) patches for Instagram — removes the screenshot and screen recording restriction.

## What this does

Instagram sets the `FLAG_SECURE` window flag to block screenshots and screen recordings.
This patch intercepts every `Window.addFlags` and `Window.setFlags` call app-wide and strips that flag before it reaches the OS, so screenshots and recordings work normally again.

## Patches

| Patch | Description | Default |
|-------|-------------|---------|
| Remove screenshot restriction | Removes `FLAG_SECURE` from all Instagram windows | Yes |

## How to use

Add this repository as a patch source in [Morphe Manager](https://morphe.software):

```
https://github.com/Pa-kon/morphe-screenshot-patches
```

Or click: [Add to Morphe](https://morphe.software/add-source?github=Pa-kon/morphe-screenshot-patches)

## Building from source

Requires a GitHub PAT with `read:packages` scope to resolve the Morphe patcher dependency.

Add to `~/.gradle/gradle.properties`:
```
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Then:
```bash
./gradlew :patches:build
./gradlew :patches:generatePatchesList
```

## License

Licensed under the [GNU General Public License v3.0](LICENSE).

# Spike: HAProxy support for JetBrains IDEs via the platform LSP API

Goal: check that the language server and TextMate grammar from `gmm-haproxy-vscode`
can run unmodified inside JetBrains IDEs, and find what blocks a Marketplace release.

**Verdict: viable.** Every language feature from the VS Code extension works through
`com.intellij.platform.lsp` with about 150 lines of Kotlin. The plugin passes the
Plugin Verifier on IU 2026.1.3, 2026.1.4 and 2026.2.2 with no internal API usage.

## Architecture

| Piece | Source | How it is wired |
|---|---|---|
| Language server | `../gmm-haproxy-vscode/server/out/server.js` (bundled) | `LspServerSupportProvider` + `ProjectWideLspServerDescriptor`, launched as `node server.js --stdio` |
| Syntax highlighting | `syntaxes/haproxy.tmLanguage.json` + `language-configuration.json` | `TextMateBundleProvider` with a small VS Code-style `package.json` (`src/main/textmate/`) |
| File detection | `HaproxyFiles` (same rules as VS Code 0.6.x) | `haproxy.cfg`, `haproxy.conf`, `haproxy*.cfg`, `*.cfg`/`*.conf` inside a `haproxy/` directory |
| Settings | `HaproxySettings` (placeholder) | Answered through `workspace/configuration`, section `haproxy`; `-PhaproxyVersion=3.3` overrides it in `runIde` |

`prepareSandbox` copies the server and grammar next to the plugin jar. If `server.js`
is missing, `buildLanguageServer` runs `npm run build:server` in the sibling repository.
The plugin finds its own directory through `PluginAware.setPluginDescriptor`.
`PluginManagerCore.getPlugin` and `PluginManager.getPluginByClass` are both `@ApiStatus.Internal` in 2026.2.

## Verified in the sandbox (IU 2026.1.3, `screenshots/`)

| Feature | Result | Screenshot |
|---|---|---|
| TextMate highlighting + diagnostics | OK: unknown directive, port out of range, unitless timeout, undefined backend | `01`, `03` |
| Hover documentation | OK: signature, description, valid sections, "Since" | `02` |
| Completion | OK: keywords and `option` variants | `04` |
| Go to definition (backend reference) | OK | `05` |
| Rename (backend, all references) | OK, renamed inline | `06`, `06a-*` |
| Version awareness (`haproxy.version=3.3`) | OK: `shm-stats-file` is no longer flagged | `08` |
| Node.js missing | Highlighting still works; one warning notification for each project | `09` |
| PyCharm 2026.1.3 (`./gradlew runPyCharm`) | OK after the file type fix below | `10` |

## Findings and fixes

1. **PyCharm maps `*.cfg` to INI.** The INI parser reported errors on `haproxy.cfg` alongside the
   LSP diagnostics. Fixed by registering exact names and patterns on the `textmate` file type
   (`fileNames="haproxy.cfg;haproxy.conf" patterns="haproxy*.cfg"`). Exact names win over extension
   mappings. Files inside `haproxy/*.cfg` with other names still need the header-based detection TODO.
2. **IntelliJ IDEA shows "Plugins supporting *.cfg files found".** This is the Marketplace suggestion
   banner. It does not affect the plugin and goes away once the plugin is on the Marketplace.
3. **Deprecated LSP API in 2026.1.4+ and 2026.2.** `LspServerSupportProvider`, `LspServerDescriptor` and
   `ProjectWideLspServerDescriptor` are deprecated in favour of `LspIntegrationProvider`,
   `LspClientDescriptor`, `ProjectWideLspClientDescriptor` and the EP `com.intellij.platform.lsp.integrationProvider`.
   The new API does not exist in 2026.1.3 (it first appears in 2026.1.4). The verifier reports 7
   deprecated usages, which are not blockers. Choose one of these before 1.0:
   keep the old API with `sinceBuild=261`, or switch to the new API and raise `sinceBuild` to 261.26222 or later.
4. **Internal API.** `PluginManagerCore.getPlugin` was flagged as internal on 2026.2.2, and that fails `verifyPlugin`.
   Replaced with `PluginAware`, and the verifier is now clean.
5. **LSP API availability.** Both sandboxes ran the unified IDE without a license ("Unlock Ultimate/Pro"
   button visible), and the LSP features still worked. The spike has not confirmed which other products/editions
   ship `com.intellij.modules.lsp`. Check this before listing compatible products.

## Disk usage

Each IDE that the verifier downloads is extracted into the Gradle transform cache (about 3 GB per IDE). With the disk at 96%,
the default `pluginVerification` set (IU 2026.1.4, IU 2026.2.2, PY 2026.1.3) could not finish extracting. Use
`-PverifierLocalIdes=/path/a,/path/b` to run against IDEs that are already extracted:

```bash
./gradlew verifyPlugin -PverifierLocalIdes=$HOME/.gradle/caches/<ver>/transforms/<hash>/transformed/idea-2026.2.2,...
```

PyCharm was checked by running it (`runPyCharm`). The verifier has not been run against it.

## Commands

```bash
./gradlew runIde                          # IU 2026.1.3 sandbox opening demo/
./gradlew runIde -PhaproxyVersion=3.3     # version-aware diagnostics
./gradlew runPyCharm                      # PyCharm 2026.1.3 sandbox
./gradlew verifyPlugin [-PverifierLocalIdes=...]
```

## Before a Marketplace release

- Settings page (HAProxy version, validate/completion toggles) to replace `HaproxySettings`.
- Header-based detection for arbitrary `*.cfg` (also update the `fileType` registration to match).
- Decide what to do about the LSP API deprecation (finding 3).
- Run the verifier on PyCharm and a free-tier build once there is disk space; set up CI (`verifyPlugin` + `buildPlugin`).
- Version the bundled server with the VS Code extension release (it is currently taken from the sibling checkout).
- Plugin icon, change notes, and a Node.js requirement note on the Marketplace page.

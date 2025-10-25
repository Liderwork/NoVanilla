# NoVanilla (Paper 1.21.1-133)

Disable all vanilla crafting/smelting/etc. on Paper 1.21.1 (build 133), with player notifications.

## Build locally
```bash
mvn -U clean package
```
JAR: `target/NoVanilla-1.0.2.jar`

## CI
- Every push & PR builds the JAR and uploads it as an artifact.
- Pushing a tag like `v1.0.2` publishes a GitHub Release with the JAR attached.

## Permissions
- `novanilla.bypass` — allow vanilla crafting/cooking for a player (default: OP).

## Config (`config.yml`)
```yaml
notify:
  actionbar: true
  chat: false
  cooldown-ms: 1500
  message: "&cВанильный {action} отключён"
```

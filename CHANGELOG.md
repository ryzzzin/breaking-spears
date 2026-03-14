# Changelog

## 1.2.0

- Elytra collision now breaks a single glass pane earlier in the tick, making fly-through more reliable.
- Added/updated public behavior for spear and trident flight interaction with glass panes.
- Added `Breaking` enchantment for tridents (`I-III`):
- `Breaking I` can break blocks mineable by stone-tier tools.
- `Breaking II` can break blocks mineable by iron-tier tools.
- `Breaking III` can break blocks mineable by diamond-tier tools.
- Drop chance on successful trident block break: `10% / 15% / 25%` by level.
- Trident loses `1` durability when `Breaking` successfully destroys a block.
- One throw can destroy at most one block (prevents chain destruction).

## 1.1.0

- Initial public release of Breaking Spears features.

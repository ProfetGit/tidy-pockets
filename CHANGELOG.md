# Changelog

## 1.2.4 — 2026-10-02

- Tool-break protection no longer fights you: if you pick a tool it moved away back into your hand on purpose, it stays there and you can use it until it breaks.

## 1.2.3 — 2026-10-01

- The animations now run on the game's frame clock instead of the system clock. In play nothing changes; video capture tools that step frames at a fixed rate now record them at their real speed.

## 1.2.2 — 2026-09-30

- Fixed the mod's textures and text missing on Minecraft Java 1.21.1, 1.21.4 and 1.21.8 (its resource pack metadata was in the 1.21.9 format). Nothing changes on 26.2 and 26.3.

## 1.2.1 — 2026-09-30

- Now also runs on Minecraft Java 1.21.1, 1.21.4, 1.21.8 and 1.21.11 (Fabric). Nothing changes on 26.2 and 26.3.

## 1.2.0 — 2026-09-30

- Random palette. Press R over a hotbar block (in the world with the block selected, or over it in an inventory) to add it to your palette; a small die marks it. With a palette block selected, every block you place comes from a random palette slot. Shift + R clears the palette.
- Avoid clumps (on by default): blocks that already sit next to the spot you place at are less likely, so walls and floors look mixed instead of patchy. Turn it off for plain random.
- A block in two palette slots comes up twice as often. Empty slots and slots that don't hold a block are skipped, and so are blocks that can't be placed on the spot you clicked.
- Clicking a chest, door, button or anything else that reacts to a click doesn't roll. Sneak-clicking still places.
- Hotbar refill keeps working: a palette slot that runs out is topped up from your inventory. The hand shows each new block at once and keeps its little placing bounce, and the hotbar selector hops to the block that was picked.
- Palettes are saved per world or server. Settings: Random palette and Avoid clumps, in their own section.

## 1.1.0 — 2026-09-24

- Creative inventory support. The Survival Inventory tab now works like the survival inventory: sorting, slot locks, shift-drag, scroll-wheel moves and drag-to-collect.
- Shift-clicking the creative trash can clears everything except locked slots.
- Creative item tabs: shift-drag across the item grid puts a full stack of each item into your inventory, hotbar first. Shift-drag along the hotbar row deletes each item, with a puff. Locked hotbar items can't be deleted or replaced.
- A shift-drag can start on a locked slot: the locked slot stays put and the drag carries on over the rest.

## 1.0.0 — 2026-09-24

- First release for Fabric, NeoForge and Forge on Minecraft 26.2 and 26.3.
- Instant middle-click sorting, with the poof-and-pop sort animation.
- Slot locking: locked items can't be picked up, shift-clicked, swapped or dropped, and locked hotbar slots remember their item.
- Hotbar and offhand refill.
- Tool-break protection.
- Mouse shortcuts: scroll-wheel moves, shift-drag and drag-to-collect.
- Container buttons (sort, deposit matching, restock) and the Ctrl+F search.
- Animations: inventory pop, background blur, item flight, hotbar glide, refill pop, tool warning shake, count bump, player hop, smooth scrolling.
- Settings screen, and automatic hand-off to Mouse Tweaks, Smooth Swapping, smooth-scrolling mods and Inventory Profiles Next.

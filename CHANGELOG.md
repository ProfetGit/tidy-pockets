# Changelog

## 1.2.30 — 2026-11-28

- **New Blur strength slider** (1 to 10) for the blur behind inventories and menus. On 1.20.5 and newer its default, Auto, keeps following the game's Menu Background Blur.
- **The blur now works on Minecraft 1.20.1** too, behind inventories and the game's menus; before, the setting did nothing there. "Inventory blur" is now called "Background blur".
- **Fixed: no blur behind inventories on Minecraft 1.21.1, 1.21.4 and 1.21.8.**
- **Fixed chest and container screens twitching with Immersive Interfaces** and other resource packs that replace the GUI shaders. On 1.20.1, 1.21.1 and 1.21.4 the screen pop now works with them; on newer versions it switches itself off while such a pack is active.
- Fixed the settings previews being drawn garbled with those packs: they show the game's own chest and hotbar then.

## 1.2.29 — 2026-11-27

- **Ctrl+F opens the search again on Minecraft 26.3.** It did nothing there.
- The right Shift key now works for shift-dragging and Shift + R on Minecraft 26.3.
- Clicking the search button lets you type straight away; before, you had to click the box first.

## 1.2.28 — 2026-11-27

- **Fixed a startup crash on Minecraft 26.1.2** (Fabric, Quilt and NeoForge) when Controlify is installed. Tidy Pockets' controller bindings are for 26.2 and 26.3 only.
- The Profet Mods settings screen has a more compact mod list with a scrollbar.

## 1.2.27 — 2026-11-27

- **Fixed a crash with Axiom**: selecting its builder tool slot closed the game.
- **The new settings screen is on every version now** (26.3, 26.2, 26.1.2, 1.21.11, 1.21.8, 1.21.4, 1.21.1, 1.20.1; Fabric, Quilt, NeoForge and Forge): the same screen, live previews, Quick setup, Report bug and Ko-fi button as on 26.3. The copper golem button in the Game Menu now shows on NeoForge and Forge too.
- **Move everything**: hold Shift and press Deposit to put your whole main inventory into the container, or hold Shift and press Restock to take everything out. Locked slots stay, and the hotbar is never deposited.
- On Minecraft 1.21.4 and older, opening a container no longer shrinks the darkened background with the panel, and EMI's item lists and search box stay put while the panel pops open.
- Minecraft 1.21.11 Forge no longer logs "Couldn't load mod:tidypockets pack metadata".

## 1.2.25 — 2026-11-26

- **New settings screen on Minecraft 26.3**: every Tidy Pockets option in the shared Profet Mods screen with a live preview of what it does (a chest window that sorts, refills and protects in front of you), Quick setup presets, and Cancel to put everything back. Also opened by the copper golem button in the Game Menu on Fabric and Quilt, together with the other Profet mods you have installed.
- **New: Report bug** button in the settings screen (Minecraft 26.3): write what happened and send it from inside the game. It attaches your Minecraft, loader, mod list, changed settings and recent errors (your name, home folder and IPs hidden) and posts it as a public GitHub issue. If sending fails, the report is copied and GitHub opens instead.
- Bug reports have short fields for what happened, the steps and what you expected; a **View report** link shows exactly what gets sent; your draft is kept until it is sent. After a crash caused by a Profet mod, the title screen offers to report it with the crash attached. New **Ko-fi** button in the settings footer.
- Moving items onto a stack no longer makes that slot blink empty: it keeps showing the old stack until the new items land.
- When tool protection takes a worn tool out of your hand, the red shake now shows on that hotbar slot only. Before, it flashed on every empty hotbar slot, or on none if an earlier slot was empty.

## 1.2.11 — 2026-11-25

- Fixed a startup crash on Minecraft 1.21.1 and 1.20.1 when Simply Swords is installed (both mods changed the same call in the held-item renderer).

## 1.2.9 — 2026-11-15

- New versions and loaders: NeoForge and Forge on 1.21.11 and 1.21.1, and Minecraft 1.20.1 on Fabric, Quilt, Forge and NeoForge. Tidy Pockets now runs on 26.3, 26.2, 1.21.11 and 1.21.1 with all four loaders, on 1.20.1 with all four, and on 1.21.8 and 1.21.4 with Fabric and Quilt.
- On 1.20.1 menus have no background blur (that version has none), so there is nothing to fade.

## 1.2.8 — 2026-10-08

- New option "Protection warning" (Tool protection section). Turn it off and tool protection still swaps or stows a nearly broken tool, but without the message, sound and slot animation. When your inventory is full and the action is blocked, the message still shows so you know why.

## 1.2.7 — 2026-10-08

- Controlify no longer lists every Tidy Pockets action twice. It was adding an automatic copy of each key next to the real controller binding, and the copy did nothing useful. Also fixed a crash that could happen at startup when Controlify loaded first.

## 1.2.6 — 2026-10-08

- Controlify's button guide now lists Tidy Pockets' actions (Sort, Lock/Unlock, Add to or Remove from Palette, Clear Palette, Search, Deposit, Restock) next to the buttons they are bound to, and only when the action would do something for the slot under the cursor.

## 1.2.5 — 2026-10-05

- Controlify support (Fabric and NeoForge, 26.2 and 26.3): Sort inventory, Lock slot, Random palette, Search, Deposit and Restock are controller bindings under Tidy Pockets in Controlify's controls. Sort is on a click of the right stick by default; point the cursor at a slot and press. Without Controlify nothing changes.

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

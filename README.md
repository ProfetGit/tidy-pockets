![Tidy Pockets](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/banner.gif)

Instant inventory sorting, hotbar refill, tool-break protection and mouse shortcuts, all in one client-side mod, with quick cartoon animations.

## Why

Most sorting mods send their clicks one by one, so you watch your items shuffle into place. Tidy Pockets works out the finished layout first and sends every click in the same frame, so a sort is done the moment you click. The animation plays on top of the finished inventory, and you can keep clicking while it runs.

![Middle-click sorting a chest and the inventory](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/clips/sort.gif)

## Features

**Sorting**
- Middle-click an inventory to sort it. This works on your own inventory, chests, double chests, barrels, shulker boxes, ender chests, hoppers, dispensers and droppers.
- Stacks merge, full stacks come first, and items follow creative-inventory order. Name and ID order are options.
- Sorting your own inventory leaves the hotbar alone unless you turn that on.
- Items poof out and pop into their new slots in a quick diagonal wave.

**Slot locking**

![Locking slots with Alt-click; a locked item refuses to move](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/clips/locks.gif)

- Hold Left Alt and click a slot in your inventory to lock it. A padlock appears on it.
- A locked item stays where it is: it can't be picked up, shift-clicked, shift-dragged, swapped with number keys, dropped with Q or pulled out by a double-click. The slot shakes to say no. You can still add more of the same item to it.
- Sorting, depositing and the mouse shortcuts leave locked slots alone. Refill can still top up a locked hotbar slot, and tool protection can still move a tool that is about to break.
- When a locked item is used up, the lock goes with it. The exception is a locked hotbar slot, which remembers its item and shows a faded copy of it. When you sort, that item is pulled back into the slot.

**Hotbar refill**

![Building until the stack runs out; the hotbar refills itself](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/clips/refill.gif)

- When the stack in your hand runs out, the best matching stack from your inventory takes its place. That covers placing your last block, eating your last food, and a bucket that just emptied.
- Refill works for the offhand too.

**Tool-break protection** (on by default)

![A nearly broken pickaxe is swapped for a fresh one while mining](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/clips/protect.gif)

- When the next hit, block or use would break the tool in your hand, the action is stopped and:
  1. a spare of the same tool is swapped in, or
  2. the worn tool is moved to your inventory, or
  3. if your inventory is full, the tool stays in your hand and the action is blocked.
- Durability cost is read from the item itself, so swords that take 2 damage per block are protected in time.

**Mouse shortcuts**

![Shift-clicks and scroll-wheel moves](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/clips/moves.gif)

![Shift-dragging a whole row into a chest](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/clips/drag.gif)

- Scroll over a stack to move one item to the other side; scroll the other way to pull one back. Hold Shift to move the whole stack.
- Hold Shift and drag across slots to shift-click all of them.
- With items on the cursor, press on a matching stack and drag to collect matching stacks onto the cursor.

**Container tools**

![Deposit matching, restock, and Ctrl+F search](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/clips/tools.gif)

- Chests and other storage get three small buttons: Sort, Deposit matching and Restock.
- Deposit matching moves every inventory stack whose item is already in the chest into it.
- Restock tops up your partial stacks from the chest.
- Press Ctrl+F for a search box that highlights matching slots and dims the rest.

**Animation**

![Inventory and chest popping open over a blurred world](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/clips/open.gif)

![The hotbar selector gliding between slots](https://raw.githubusercontent.com/ProfetGit/tidy-pockets/main/docs/clips/hotbar.gif)

- Inventories pop open in about 0.1 s. Recipe viewers such as JEI and REI stay still on purpose.
- The world behind an open inventory is blurred. Strength follows the vanilla Menu Background Blur setting.
- Items land in their new slot at once, and a quick ghost streaks over from where they came from. This happens for shift-clicks, the scroll wheel, deposit and restock.
- The hotbar selector glides, a refilled slot pops, and a protected tool shakes red.
- Stack counts bump when they change, and your character hops when you equip armour.
- Lists scroll smoothly, and the creative inventory slides row by row.
- Every animation has its own toggle, and one speed slider covers them all. Setting the slider to 0 turns them all off.

## How to use

| Action | Default |
|---|---|
| Sort | Middle mouse button over an inventory |
| Lock or unlock a slot | Hold Left Alt and left-click |
| Search | Ctrl+F in a container |
| Move one item / pull one back | Scroll wheel over a stack |
| Move a whole stack | Shift + scroll wheel |
| Shift-click many slots | Hold Shift and drag |

All keys can be changed in Controls, under Tidy Pockets. Deposit and Restock have unbound keys you can assign.

In creative mode, middle-clicking an item still copies it as vanilla does. Middle-click an empty slot or the background to sort.

## Settings

Open the settings screen from Mod Menu on Fabric, or from the Mods list on NeoForge and Forge. Settings are saved in `config/tidypockets.json`, and slot locks in `config/tidypockets-locks.json`, per world and per server.

## Compatibility

- **Client-side only.** Works on any server, including vanilla servers and Realms. Nothing needs to be installed on the server.
- **Anti-cheat:** some anti-cheat plugins may object to a whole sort arriving at once. If a server does, turn on **Safe mode**, which sends the clicks a few per tick.
- **Other mods that do the same job:** Tidy Pockets switches its matching feature off when one is installed, so the two never fight:
  - Mouse Tweaks or Mouse Wheelie: mouse shortcuts off
  - Smooth Swapping: item flight off
  - Smooth Scrolling: smooth scrolling off
  - Inventory Profiles Next: sorting and refill off
- **Tested with** JEI, REI, Mouse Tweaks, Smooth Swapping and Mod Menu.

## Installation

Put the jar for your loader and Minecraft version in your `mods` folder.

| Loader | Minecraft | Needs |
|---|---|---|
| Fabric (also Quilt) | 26.2, 26.3 | Fabric API |
| NeoForge | 26.2, 26.3 | - |
| Forge | 26.2, 26.3 | - |

## Good to know

- Sorting doesn't work inside the creative inventory screen itself, because it uses its own item grid. It does work in chests opened in creative.
- A bundle is never clicked onto another stack, because vanilla would put that stack inside the bundle. In a completely full chest, bundles therefore stay where they are.
- The item flight only animates moves that you make. Items moved by hoppers or other players just appear.

## License

All rights reserved, with permissions: you can use it anywhere, include it in modpacks with credit, and show it in videos. See [LICENSE](LICENSE).

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

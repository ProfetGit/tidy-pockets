package io.github.profetgit.tidypockets.ui.settings;

//? if >=26.3 {
import io.github.profetgit.profetsettings.*;
import io.github.profetgit.tidypockets.anim.SlotAnims;
import io.github.profetgit.tidypockets.anim.SortPop;
import io.github.profetgit.tidypockets.input.Keys;
import io.github.profetgit.tidypockets.tools.ContainerTools;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// The tools on a storage screen, where the mod puts them: Ctrl+F opens the search box and a typed word marks the stacks
// ContainerTools.matches (the rest are greyed), then Deposit, Restock and Sort are clicked in turn. A switched-off tool
// has no button and its keys and clicks do nothing, as in vanilla.
public final class ToolsPreview extends ChestStage {
    private static final float OPEN = 450, TYPE = 750, LETTER = 220, CLOSE = 2150, DEPOSIT = 2850, RESTOCK = 3550, TOPSORT = 4250,
        SLIDE_AT = 5200;
    private static final String WORD = "app";
    private static final int CHEST_SLOTS = 9, MAIN = 9, BAR = 18;
    private static final Identifier FIELD = Identifier.withDefaultNamespace("widget/text_field_highlighted");

    private ItemStack[] start;
    private ItemStack[] sorted;
    private int order = -1;

    public ToolsPreview(ModDef mod) {
        super(mod);
    }

    private static ItemStack[] stacks() {
        ItemStack[] s = empty(27);
        s[0] = new ItemStack(Items.TORCH, 40);
        s[2] = new ItemStack(Items.APPLE, 12);
        s[3] = new ItemStack(Items.IRON_INGOT, 17);
        s[5] = new ItemStack(Items.GOLDEN_APPLE, 3);
        s[6] = new ItemStack(Items.BREAD, 30);
        s[8] = new ItemStack(Items.COAL, 22);
        s[MAIN + 1] = new ItemStack(Items.IRON_INGOT, 9);
        s[MAIN + 2] = new ItemStack(Items.BONE, 7);
        s[MAIN + 4] = new ItemStack(Items.APPLE, 5);
        s[MAIN + 5] = new ItemStack(Items.STRING, 11);
        s[MAIN + 7] = new ItemStack(Items.COAL, 14);
        s[BAR] = new ItemStack(Items.IRON_PICKAXE);
        s[BAR + 1] = new ItemStack(Items.TORCH, 9);
        s[BAR + 3] = new ItemStack(Items.BREAD, 4);
        s[BAR + 8] = new ItemStack(Items.WATER_BUCKET);
        return s;
    }

    // Deposit: every main-inventory stack whose item is already in the chest is shift-clicked into it
    private static void deposit(ItemStack[] s) {
        for (int i = MAIN; i < BAR; i++) {
            if (s[i].isEmpty()) continue;
            boolean present = false;
            for (int j = 0; j < CHEST_SLOTS; j++) present |= same(s[j], s[i]);
            if (present) quickMove(s, i, range(0, CHEST_SLOTS));
        }
    }

    // Restock: the partial stacks of the hotbar, then of the inventory, are topped up from the chest
    private static void restock(ItemStack[] s) {
        for (int k = 0; k < 18; k++) {
            int i = k < 9 ? BAR + k : MAIN + k - 9;
            for (int j = 0; j < CHEST_SLOTS && !s[i].isEmpty(); j++) {
                int room = s[i].getMaxStackSize() - s[i].getCount();
                if (room <= 0) break;
                if (!same(s[j], s[i])) continue;
                int n = Math.min(room, s[j].getCount());
                s[i] = s[i].copyWithCount(s[i].getCount() + n);
                s[j] = s[j].getCount() == n ? ItemStack.EMPTY : s[j].copyWithCount(s[j].getCount() - n);
            }
        }
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        boolean buttons = flag("containerButtons", true), search = flag("search", true), fly = flag("animFly", true),
            bump = flag("animCountBump", true), animSort = flag("animSort", true);
        int sortOrder = num("sortOrder", 0);
        double speed = speed();
        int ox = x + (w - W) / 2;
        Panel p = new Panel(ox + WIN_X, y + 24, 17, 1, 1, true);
        backdrop(g, ox, y);
        if (!itemsReady()) {
            p.draw(g);
            noWorld(g, x, y, w, h);
            return;
        }
        if (start == null) start = stacks();
        float time = time(c, SLIDE_AT + SLIDE, buttons, search, fly, bump, animSort, sortOrder, speed);

        Board board = new Board(start, time, speed, fly, bump);
        if (buttons) {
            board.change(DEPOSIT, true, ToolsPreview::deposit);
            board.change(RESTOCK, true, ToolsPreview::restock);
        }
        boolean sorting = buttons && time >= TOPSORT;
        ItemStack[] before = null;
        if (sorting) {
            before = copy(board.item);
            if (sorted == null || order != sortOrder) {
                Board done = new Board(start, SLIDE_AT, 0, false, false);
                done.change(DEPOSIT, true, ToolsPreview::deposit);
                done.change(RESTOCK, true, ToolsPreview::restock);
                sorted = sort(done.item, CHEST_SLOTS).tidy();
                order = sortOrder;
            }
            System.arraycopy(sorted, 0, board.item, 0, CHEST_SLOTS);
        }

        // where the mod puts the buttons: right to left from the title bar's corner
        int right = p.x() + WIN_W - 7 - 11, by = p.y() + 4, searchX = buttons ? right - 36 : right;
        // pointer: rests, then visits the three buttons; without them, the empty title bar left of the search button
        float homeX = ox + 180, homeY = y + 108;
        float bx = (buttons || !search ? right : right - 12) + 6;
        float[] at = path(time, 2250, homeX, homeY, DEPOSIT - 100, bx - 12, by + 6, DEPOSIT + 250, bx - 12, by + 6,
            RESTOCK - 100, bx - 24, by + 6, RESTOCK + 250, bx - 24, by + 6, TOPSORT - 100, bx, by + 6,
            TOPSORT + 450, bx, by + 6, TOPSORT + 900, homeX, homeY);
        boolean open = search && time >= OPEN && time < CLOSE;
        String query = open && time >= TYPE ? WORD.substring(0, Math.min(WORD.length(), 1 + (int) ((time - TYPE) / LETTER))) : "";
        ItemStack[] was = before;
        sliding(g, w, (time - SLIDE_AT) / SLIDE, () -> {
            p.draw(g);
            title(g, p);
            if (sorting) popping(g, p, board, was, (time - TOPSORT) * speed, animSort && speed > 0);
            else board.draw(g, p, -1);
            if (!query.isEmpty()) {
                g.g.nextStratum();
                for (int i = 0; i < p.size(); i++) {
                    if (ContainerTools.matches(board.item[i], query)) g.g.outline(p.sx(i) - 1, p.sy(i) - 1, 18, 18, 0xFFFFD84A);
                    else g.rect(p.sx(i), p.sy(i), 16, 16, 0xB0202020);
                }
            }
            if (open) field(g, p.x() + 2, p.y() - 14, query, (int) (time / 300) % 2 == 0);
            if (buttons) {
                button(g, "sort", right, by, at);
                button(g, "deposit", right - 12, by, at);
                button(g, "restock", right - 24, by, at);
            }
            if (search) button(g, "search", searchX, by, at);
        }, () -> {
            p.draw(g);
            title(g, p);
            new Board(start, 0, speed, fly, bump).draw(g, p, -1);
            if (buttons) {
                button(g, "sort", right, by, null);
                button(g, "deposit", right - 12, by, null);
                button(g, "restock", right - 24, by, null);
            }
            if (search) button(g, "search", searchX, by, null);
        });
        float down = Math.max(press(time, DEPOSIT), Math.max(press(time, RESTOCK), press(time, TOPSORT)));
        pointer(g, at[0], at[1], down);

        if (time < 2400) {
            boolean pressed = within(time, OPEN - 60, 220) || within(time, CLOSE - 60, 220);
            if (Keys.SEARCH.isUnbound()) {
                keys(g, c, x + w / 2f, y + KEY_Y, keyName("tidypockets.ui.key.ctrl"), pressed, "F", pressed);
            } else {
                key(g, c, Keys.SEARCH, x + w / 2f, y + KEY_Y, pressed);
            }
        } else {
            mouse(g, false, x + w / 2f, y + KEY_Y,
                within(time, DEPOSIT, 180) || within(time, RESTOCK, 180) || within(time, TOPSORT, 180));
        }
        off(g, x, y, w, h, !buttons ? search ? "containerButtons" : "tools" : search ? null : "search");
    }

    private static void title(Gfx g, Panel p) {
        g.vanilla(I18n.get("container.chest"), p.x() + 8, p.y() + 6, 0xFF404040, false);
    }

    // the chest row in the mod's sort pop, ms animation ms after the click
    private static void popping(Gfx g, Panel p, Board board, ItemStack[] before, double ms, boolean animated) {
        for (int i = 0; i < p.size(); i++) {
            boolean moved = animated && i < CHEST_SLOTS && !ItemStack.matches(before[i], board.item[i]);
            stack(g, board.item[i], p.sx(i), p.sy(i), moved ? SlotAnims.popScale(ms, SortPop.delay(i, 0)) : 1);
        }
        g.g.nextStratum();
        for (int i = 0; i < CHEST_SLOTS && animated; i++) {
            int frame = ItemStack.matches(before[i], board.item[i]) ? -1 : SlotAnims.puffFrame(ms, SortPop.delay(i, 0));
            if (frame >= 0) g.g.blitSprite(RenderPipelines.GUI_TEXTURED, SlotAnims.puffSprite(frame), p.sx(i), p.sy(i), 16, 16);
        }
    }

    private static void button(Gfx g, String name, int x, int y, float[] pointer) {
        boolean over = pointer != null && pointer[0] >= x && pointer[0] < x + 11 && pointer[1] >= y && pointer[1] < y + 11;
        g.g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("tidypockets", over ? name + "_highlighted" : name),
            x, y, 11, 11);
    }

    // the vanilla text field the search box is, focused, with its caret
    private static void field(Gfx g, int x, int y, String query, boolean caret) {
        g.g.nextStratum();
        g.g.blitSprite(RenderPipelines.GUI_TEXTURED, FIELD, x, y, 110, 12);
        g.vanilla(query, x + 4, y + 2, 0xFFE0E0E0, true);
        if (caret) g.vanilla("_", x + 4 + g.vanillaWidth(query), y + 2, 0xFFE0E0E0, true);
    }
}
//?}

-- Tidy Pockets icon sprites. Run through the aseprite MCP: dofile("<abs>/TidyPockets/dev/icon/draw_sprites.lua")
-- The diamond pickaxe is Veinminer's approved pickaxe_item (same author), moved into a 14x14 slot icon.
-- fx_puff, fx_spark, fx_ring and fx_star are copies of Veinminer's FX sprites.
dofile("/home/emppu/Projects/Minecraft Datapacks/.claude/skills/pack-icon-animation/assets/pixel_art.lua")
local ROOT = "/home/emppu/Projects/Minecraft Datapacks/"
local OUT = ROOT .. "TidyPockets/dev/icon/sprites/"
local key = PA.key

-- GUI panel: vanilla inventory greys. 3x3 slots of 16 px (1 px borders, 14 px inside), 3 px frame.
local GUI = { W = "#FFFFFF", F = "#C6C6C6", S = "#555555", K = "#373737", N = "#8B8B8B" }
local SLOT, COLS, ROWS, EDGE = 16, 3, 3, 3
do
  local w, h = COLS * SLOT + 2 * EDGE, ROWS * SLOT + 2 * EDGE
  local px = {}
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      local c = GUI.F
      if x == 0 or y == 0 then c = GUI.W end
      if x == w - 1 or y == h - 1 then c = GUI.S end
      px[key(x, y)] = c
    end
  end
  for _, p in ipairs({ { 0, 0 }, { w - 1, 0 }, { 0, h - 1 }, { w - 1, h - 1 } }) do px[key(p[1], p[2])] = nil end
  for r = 0, ROWS - 1 do
    for c = 0, COLS - 1 do
      local ox, oy = EDGE + c * SLOT, EDGE + r * SLOT
      for y = 0, SLOT - 1 do
        for x = 0, SLOT - 1 do
          local col = GUI.N
          if x == 0 or y == 0 then col = GUI.K end
          if x == SLOT - 1 or y == SLOT - 1 then col = GUI.W end
          if (x == SLOT - 1 and y == 0) or (x == 0 and y == SLOT - 1) then col = GUI.N end
          px[key(ox + x, oy + y)] = col
        end
      end
    end
  end
  PA.save_pixels(OUT .. "panel", w, h, px)
end

-- Items, 14x14 (the inside of a slot). Light from the upper left, a dark rim of the item's own hue.
PA.sprite_from_grid(OUT .. "apple", {
  "..............",
  "........Gll...",
  ".......vgg....",
  "...rrr.v.rrR..",
  "..rshrrRrrrRk.",
  ".rshHhrrrrrRk.",
  ".rhHhsrrrrrRk.",
  ".rshhsrrrrRRk.",
  ".rssssrrrrRRk.",
  ".rsrrrrrrRRRk.",
  "..rrrrrrRRRk..",
  "..RrrrrRRRkk..",
  "...kRRRRRkk...",
  ".....kkkk.....",
}, { k = "#4A0A22", R = "#8E1530", r = "#C8263A", s = "#EE4A3E", h = "#FF8A5E", H = "#FFD6B0",
     v = "#3B2410", G = "#2E6B24", g = "#4FA83A", l = "#9BE05A" })

PA.sprite_from_grid(OUT .. "grass", {
  "......ab......",
  "....abbcbb....",
  "..abcccbcccb..",
  "abccbccccbccbb",
  "ddcccbccccbcff",
  "dmddccbcccfffn",
  "mmmdddccffnnfn",
  "momdmmdfnfnpnn",
  "mmmnmmmnnpnnnn",
  "mommmnmnpnnnpn",
  "nnmmnmmnnnpnpp",
  "..nnmomnpnpp..",
  "....nnmnpp....",
  "......np......",
}, { a = "#C4F07A", b = "#8BD04E", c = "#5DAA38", d = "#3F8A2C", f = "#2C6424",
     o = "#B8875A", m = "#946440", n = "#6E4730", p = "#4A2E1E" })

PA.sprite_from_grid(OUT .. "gold", {
  "..............",
  "..............",
  ".........HH...",
  ".......HHLKHH.",
  ".....HHLLKKKKH",
  "...HHLLKKKKIIH",
  ".HHLLKKKKIIIIG",
  "HLLKKKKIIIIGG.",
  "JJJKKIIIIGG...",
  "GJJJIIIGG.....",
  ".GGJIGG.......",
  "...GG.........",
  "..............",
  "..............",
}, { G = "#6B3413", H = "#B3561A", I = "#E8891C", J = "#FBB829", K = "#FFE14D", L = "#FFF7BD" })

do
  local src = Sprite{ fromFile = ROOT .. "Veinminer/dev/icon/sprites/pickaxe_item.png" }
  local px, pc = {}, app.pixelColor
  for it in src.cels[1].image:pixels() do
    local v = it()
    if pc.rgbaA(v) > 0 then
      px[key(it.x - 1, it.y - 1)] = string.format("#%02X%02X%02X", pc.rgbaR(v), pc.rgbaG(v), pc.rgbaB(v))
    end
  end
  src:close()
  PA.save_pixels(OUT .. "pickaxe", 14, 14, px)
end

-- Stack-count digits 0-9, 3x5 each, packed side by side (the compositor adds the vanilla-style shadow).
do
  local D = {
    { "###", "#.#", "#.#", "#.#", "###" }, { ".#.", "##.", ".#.", ".#.", "###" }, { "###", "..#", "###", "#..", "###" },
    { "###", "..#", "###", "..#", "###" }, { "#.#", "#.#", "###", "..#", "..#" }, { "###", "#..", "###", "..#", "###" },
    { "###", "#..", "###", "#.#", "###" }, { "###", "..#", "..#", ".#.", ".#." }, { "###", "#.#", "###", "#.#", "###" },
    { "###", "#.#", "###", "..#", "###" },
  }
  local px = {}
  for d = 0, 9 do
    for y = 1, 5 do
      for x = 1, 3 do
        if D[d + 1][y]:sub(x, x) == "#" then px[key(d * 3 + x - 1, y - 1)] = "#FFFFFF" end
      end
    end
  end
  PA.save_pixels(OUT .. "digits", 30, 5, px)
end

-- The mouse (the "tool"): cool white plastic, a cord curling off toward the panel, a 2 px scroll wheel.
local MOUSE = {
  "....cc..........",
  "......c.........",
  ".......c........",
  "....WWWuuwwk....",
  "...WWWWuuwwwk...",
  "..WWWWWXxwwwvk..",
  ".WWWWWWXxwwwwvk.",
  ".WWWWWWXxwwwwvk.",
  ".WWWWWwXxwwwwvk.",
  ".WWWWwwuuwwwvvk.",
  ".WWWwwwuuwwwvvk.",
  ".Wvvvvvvvvvvvvk.",
  ".WWwwwwwwwwwvvk.",
  ".WWwwwwwwwwwvvk.",
  ".WWwwwwwwwwwvvk.",
  ".Wwwwwwwwwwvvvk.",
  ".Wwwwwwwwwwvvvk.",
  ".wwwwwwwwwvvvuk.",
  ".vwwwwwwwvvvuuk.",
  "..vwwwwwvvvuuk..",
  "...uvvvvvuuuk...",
  "....kkkkkkkk....",
}
local mouse = { W = "#FFFFFF", w = "#E4E6F0", v = "#BFC3D9", u = "#8F94B5", k = "#585D82", c = "#3B3F5C",
                X = "#6E7396", x = "#3B3F5C" }
PA.sprite_from_grid(OUT .. "mouse", MOUSE, mouse)
local lit = {}
for k, v in pairs(mouse) do lit[k] = v end
lit.X, lit.x = "#FFF7BD", "#FFE14D"
PA.sprite_from_grid(OUT .. "mouse_click", MOUSE, lit)

-- Background options, 72x72 (the icon canvas). The compositor adds the drop shadow and the dark outline.
local function bg(name, pattern)
  local px = {}
  for y = 0, 71 do
    for x = 0, 71 do px[key(x, y)] = pattern(x, y) end
  end
  PA.save_pixels(OUT .. name, 72, 72, px)
end
local function flat(c) return function() return c end end
bg("bg_leather", flat("#7A4E32"))
bg("bg_denim", function(x, y) return (x + y) % 3 == 0 and "#34548A" or "#3B5E91" end)
bg("bg_green", flat("#3C7A4A"))
bg("bg_terracotta", flat("#B5553C"))
bg("bg_plum", flat("#6B3A6E"))

-- Banner lettering (x8 title, x6 tagline): TIDY in diamond cyan, POCKETS in cream, dark leather outline.
local INK = "#24140C"
local N8 = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { -1, 0 }, { 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } }
local function outline(px, colour)
  local solid = {}
  for k in pairs(px) do solid[k] = true end
  for k in pairs(solid) do
    local x, y = k % 4096, k // 4096
    for _, d in ipairs(N8) do
      local n = key(x + d[1], y + d[2])
      if not solid[n] then px[n] = colour end
    end
  end
end
-- PA.title_sprite with a style per character; each extruded cell takes the colour of its distance to the glyph above
local function title(path, text, style_of)
  local mask, right = PA.layout(text, PA.TITLE, 2, 4, 1, 1)
  local px, ext = {}, {}
  for k, v in pairs(mask) do px[k] = style_of(v[2]).bands[v[1]] end
  for k, v in pairs(mask) do
    local x, y = k % 4096, k // 4096
    local st = style_of(v[2])
    for d = 1, #st.extrude do
      local n = key(x, y + d)
      if mask[n] then break end
      if not ext[n] or ext[n].d > d then ext[n] = { d = d, col = st.extrude[d] } end
    end
  end
  for n, e in pairs(ext) do px[n] = e.col end
  outline(px, INK)
  PA.save_pixels(path, right + 1, 14, px)
end
local CYAN = { bands = { "#B0FFF1", "#62EBD8", "#62EBD8", "#62EBD8", "#27CEC4", "#27CEC4", "#27CEC4", "#1CAFAA", "#1CAFAA", "#1CAFAA" },
               extrude = { "#157A80", "#0D4A52" } }
local CREAM = { bands = { "#FFFBF0", "#FFF1D6", "#FFF1D6", "#FFF1D6", "#F5DDB0", "#F5DDB0", "#F5DDB0", "#E6C48E", "#E6C48E", "#E6C48E" },
                extrude = { "#A8744A", "#6E4730" } }
title(OUT .. "banner_title", "TIDY POCKETS", function(i) return i <= 4 and CYAN or CREAM end)
PA.label_sprite(OUT .. "banner_tagline", "ONE CLICK. ALL SORTED.", function(i) return i > 11 and "#62EBD8" or "#FFFFFF" end, INK)

-- Banner background, 192x64 (x8 = 1536x512): leather with a stitched seam two cells inside the rounded edge.
do
  local W, H = 192, 64
  local px = {}
  for y = 0, H - 1 do
    for x = 0, W - 1 do px[key(x, y)] = "#7A4E32" end
  end
  local thread = "#D2A06C"
  -- each edge runs "on, off, off, on, on, off, off, ..., on" so it is symmetric; the corner cells join the end dashes
  local function edge(cells)
    for i, p in ipairs(cells) do
      if i % 4 < 2 then px[key(p[1], p[2])] = thread end
    end
  end
  local top, bottom, left, right = {}, {}, {}, {}
  for x = 4, W - 5 do top[#top + 1] = { x, 2 }; bottom[#bottom + 1] = { x, H - 3 } end
  for y = 4, H - 5 do left[#left + 1] = { 2, y }; right[#right + 1] = { W - 3, y } end
  for _, e in ipairs({ top, bottom, left, right }) do edge(e) end
  for _, p in ipairs({ { 3, 3 }, { W - 4, 3 }, { 3, H - 4 }, { W - 4, H - 4 } }) do px[key(p[1], p[2])] = thread end
  PA.save_pixels(OUT .. "banner_bg", W, H, px)
end

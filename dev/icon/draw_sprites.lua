-- Tidy Pockets icon v2 sprites ("the heap that sorts itself"). Run through the aseprite MCP:
--   dofile("<abs>/TidyPockets/dev/icon/draw_sprites.lua")
-- apple, grass and gold (the ingot) keep v1's grids, because Description-Kit reads them from this folder.
-- The fx_* sprites are Veinminer's FX (same author).
dofile("/home/emppu/Projects/Minecraft Datapacks/.claude/skills/pack-icon-animation/assets/pixel_art.lua")
local ROOT = "/home/emppu/Projects/Minecraft Datapacks/"
local OUT = ROOT .. "TidyPockets/dev/icon/sprites/"
local key = PA.key
local pc = app.pixelColor

local function read(path, dx, dy, px)
  local src = Sprite{ fromFile = path }
  px = px or {}
  for it in src.cels[1].image:pixels() do
    local v = it()
    if pc.rgbaA(v) > 0 then
      px[key(it.x + (dx or 0), it.y + (dy or 0))] = string.format("#%02X%02X%02X", pc.rgbaR(v), pc.rgbaG(v), pc.rgbaB(v))
    end
  end
  local w, h = src.width, src.height
  src:close()
  return px, w, h
end
local function copy(src, name, dx, dy, w, h)
  local px, sw, sh = read(src, dx, dy)
  PA.save_pixels(OUT .. name, w or sw, h or sh, px)
end

-- Items, 14x14 (the inside of a slot) -------------------------------------------------------------------------------
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

-- FX: Veinminer's set.
for _, n in ipairs({ "fx_puff", "fx_spark", "fx_ring", "fx_star" }) do
  copy(ROOT .. "Veinminer/dev/icon/sprites/" .. n .. ".png", n)
end

-- Blocks, 16x16: each is an index grid (0 darkest .. 5 lightest) painted through three ramps: top, left (one step
-- darker) and right (about 1.5 steps darker). The render uses shading:false, so the faces carry their own light.
local RAMPS = {
  gold = {
    top   = { "#6B3413", "#B3561A", "#E8891C", "#FBB829", "#FFE14D", "#FFF7BD" },
    left  = { "#4A200A", "#6B3413", "#B3561A", "#E8891C", "#FBB829", "#FFE14D" },
    right = { "#3A1808", "#5A2A0E", "#A94E17", "#D9781A", "#F0A020", "#FBB829" },
  },
  diamond = {
    top   = { "#0D3F4A", "#198F94", "#27CEC4", "#62EBD8", "#B0FFF1", "#FFFFFF" },
    left  = { "#082A33", "#0D3F4A", "#198F94", "#27CEC4", "#62EBD8", "#B0FFF1" },
    right = { "#061E26", "#0B3440", "#147478", "#1FA8A6", "#3FD6C6", "#8AF2E2" },
  },
  redstone = {
    top   = { "#4A0A12", "#8E1520", "#C8262A", "#EE4A3A", "#FF8A66", "#FFD0B8" },
    left  = { "#33060C", "#4A0A12", "#8E1520", "#C8262A", "#EE4A3A", "#FF8A66" },
    right = { "#26040A", "#3E0810", "#74111C", "#A81C24", "#D83A32", "#F46A52" },
  },
}
local function bevel(x, y)   -- 1 px frame: lit top/left, dark bottom/right
  if x == 15 or y == 15 then return 1 end
  if x == 0 or y == 0 then return 5 end
end
local MOTIF = {
  -- engraved inner square (dark top/left groove, lit bottom/right) and a diagonal shine
  gold = function(x, y)
    local r = math.min(x, y, 15 - x, 15 - y)
    if r == 2 then return (x == 2 or y == 2) and (x < 13 and y < 13) and 2 or 4 end
    if r >= 3 then
      if x + y == 10 or x + y == 11 then return 5 end
      if x + y == 14 and r >= 4 then return 4 end
      if x + y >= 20 then return 2 end
    end
    return 3
  end,
  -- four cut gems: lit upper-left facets, dark lower-right, a white glint in each
  diamond = function(x, y)
    local cx, cy = (x < 8) and 4 or 11, (y < 8) and 4 or 11
    local dx, dy = x - cx, y - cy
    local d = math.abs(dx) + math.abs(dy)
    if d == 0 then return 5 end
    if d <= 2 then return (dx + dy < 0) and 4 or ((dx + dy > 0) and 2 or 3) end
    if d == 3 then return (dx + dy <= 0) and 2 or 1 end
    return 3
  end,
  -- dust studs in a 3x3 grid, joined by dark traces
  redstone = function(x, y)
    local sx, sy = (x - 2) % 5, (y - 2) % 5
    local ix, iy = (x - 2) // 5, (y - 2) // 5
    if x >= 2 and y >= 2 and ix <= 2 and iy <= 2 and sx <= 1 and sy <= 1 then
      if sx == 0 and sy == 0 then return 5 end
      if sx == 1 and sy == 1 then return 2 end
      return 4
    end
    if x >= 2 and y >= 2 and x <= 13 and y <= 13 and (sx == 3 and sy <= 1 or sy == 3 and sx <= 1) then return 1 end
    return 3
  end,
}
for name, ramp in pairs(RAMPS) do
  for _, side in ipairs({ "top", "left", "right" }) do
    local px = {}
    for y = 0, 15 do
      for x = 0, 15 do px[key(x, y)] = ramp[side][(bevel(x, y) or MOTIF[name](x, y)) + 1] end
    end
    PA.save_pixels(OUT .. name .. "_" .. side, 16, 16, px)
  end
end

-- Backgrounds, 64x64 (x8 = 512). The icon compositor adds the ground shadow and the dark outline.
local function bg(name, pattern)
  local px = {}
  for y = 0, 63 do
    for x = 0, 63 do px[key(x, y)] = pattern(x, y) end
  end
  PA.save_pixels(OUT .. name, 64, 64, px)
end
bg("bg_leather", function() return "#7A4E32" end)

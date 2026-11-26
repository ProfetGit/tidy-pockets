package io.github.profetgit.tidypockets.config;

import com.mojang.serialization.Codec;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21 {
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
*///?}
import net.minecraft.network.chat.Component;

/** The vanilla-style options page (kept for the 26.2 and older targets until the Profet Settings screen is ported, and for the smooth-scroll scene). */
public final class LegacyConfigScreen extends OptionsSubScreen {
    public LegacyConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("tidypockets.config.title"));
    }

    //? if <1.21 {
    /*private OptionsList list;

    @Override
    protected void init() {
        list = new OptionsList(minecraft, width, height, 32, height - 32, 25);
        addOptions();
        addWidget(list);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> minecraft.setScreen(lastScreen))
            .bounds(width / 2 - 100, height - 27, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        basicListRender(g, list, mx, my, pt);
    }
    *///?}

    //? if >=1.21 {
    @Override
    //?}
    protected void addOptions() {
        header("sorting");
        small(bool("sortEnabled"), bool("sortHotbar"), choice("sortOrder"), bool("sortSound"),
            bool("safeMode"), range("safeModeClicksPerTick", 1, 64));
        header("refill");
        small(bool("refillEnabled"), bool("refillExactMatch"), bool("refillOffhand"));
        header("random");
        small(bool("randomEnabled"), bool("randomSpread"));
        header("protect");
        small(choice("protect"), range("protectMargin", 0, 20), bool("protectWarn"));
        header("mouse");
        small(bool("wheelMove"), bool("wheelInvert"), bool("shiftDrag"), bool("collectDrag"));
        header("tools");
        small(bool("containerButtons"), bool("search"));
        header("animation");
        small(speed(), bool("animSort"), bool("animFly"), bool("animScreenPop"), bool("animAllScreens"),
            bool("animHotbar"), bool("animCountBump"), bool("animPlayerHop"), bool("smoothScroll"), bool("blur"));
    }

    @Override
    public void removed() {
        TidyConfig.save();
        super.removed();
    }

    private void header(String group) {
        //? if >=1.21.11 {
        list.addHeader(Component.translatable("tidypockets.config." + group));
        //?}
    }

    private void small(OptionInstance<?>... options) {
        list.addSmall(options);
    }

    private static Field field(String name) {
        try {
            return TidyConfig.class.getField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(name, e);
        }
    }

    private static Object read(Field f) {
        try {
            return f.get(TidyConfig.get());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(Field f, Object v) {
        try {
            f.set(TidyConfig.get(), v);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String key(String name) {
        return "tidypockets.option." + name;
    }

    private static OptionInstance<Boolean> bool(String name) {
        Field f = field(name);
        return OptionInstance.createBoolean(key(name), OptionInstance.cachedConstantTooltip(Component.translatable(key(name) + ".tooltip")),
            (Boolean) read(f), v -> write(f, v));
    }

    private static OptionInstance<Integer> range(String name, int min, int max) {
        Field f = field(name);
        return new OptionInstance<>(key(name), OptionInstance.cachedConstantTooltip(Component.translatable(key(name) + ".tooltip")),
            (caption, v) -> Component.translatable("options.generic_value", caption, v),
            new OptionInstance.IntRange(min, max), (Integer) read(f), v -> write(f, v));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionInstance<?> choice(String name) {
        Field f = field(name);
        Class<? extends Enum> type = (Class<? extends Enum>) f.getType();
        List<Enum> values = Arrays.asList(type.getEnumConstants());
        Codec<Enum> codec = Codec.STRING.xmap(s -> Enum.valueOf(type, s), Enum::name);
        return new OptionInstance<Enum>(key(name), OptionInstance.cachedConstantTooltip(Component.translatable(key(name) + ".tooltip")),
            (caption, v) -> Component.translatable(key(name) + "." + v.name().toLowerCase()),
            new OptionInstance.Enum<>(values, codec), (Enum) read(f), v -> write(f, v));
    }

    private static OptionInstance<Integer> speed() {
        return new OptionInstance<>(key("animSpeed"), OptionInstance.cachedConstantTooltip(Component.translatable(key("animSpeed") + ".tooltip")),
            (caption, v) -> v == 0 ? Component.translatable("tidypockets.option.animSpeed.off", caption)
                : Component.translatable("options.generic_value", caption, String.format("%.1f×", v / 10.0)),
            new OptionInstance.IntRange(0, 30), (int) Math.round(TidyConfig.get().animSpeed * 10), v -> TidyConfig.get().animSpeed = v / 10.0);
    }
}

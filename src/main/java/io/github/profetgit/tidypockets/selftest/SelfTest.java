package io.github.profetgit.tidypockets.selftest;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.profetgit.tidypockets.TidyPockets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * In-game test runner, inert unless -Dtidypockets.selftest=&lt;out dir&gt; is set. Mode "boot" only checks that the
 * title screen is reached; "full" creates a flat world and runs {@link Scenarios}. Results go to results.json,
 * captured frames to frames/&lt;name&gt;/, then the game quits.
 */
public final class SelfTest {
    private static final String OUT = System.getProperty("tidypockets.selftest");
    private static final String MODE = System.getProperty("tidypockets.selftest.mode", "full");
    private static final List<String> RESULTS = new ArrayList<>();
    private static final ExecutorService WRITER = Executors.newFixedThreadPool(4);
    public static int contentPackets;

    private static boolean finished;
    private static int titleTicks, ticks;
    private static List<Script.Step> steps;
    private static int index;
    private static String captureName;
    private static int captureLeft, captureFrame;
    private static long captureStart;

    private SelfTest() {}

    public static boolean active() {
        return OUT != null;
    }

    public static void onClientTick(Minecraft mc) {
        if (finished) return;
        if (++ticks % 400 == 0) {
            TidyPockets.LOG.info("[selftest] step {}/{} screen={}", index, steps == null ? 0 : steps.size(),
                mc.gui.screen() == null ? null : mc.gui.screen().getClass().getSimpleName());
        }
        if (steps == null && MODE.equals("remote")) {
            if (mc.player == null || mc.level == null || mc.gui.screen() != null || ++titleTicks < 40) return;
            record("joined server", true, "connected over localhost");
            steps = Scenarios.buildRemote().steps;
        }
        if (steps == null) {
            if (!mc.isGameLoadFinished() || !(mc.gui.screen() instanceof TitleScreen) || ++titleTicks < 20) return;
            record("boot", true, "title screen reached");
            if (MODE.equals("boot")) {
                finish(mc);
                return;
            }
            steps = switch (MODE) {
                case "compat" -> Scenarios.buildCompat().steps;
                case "showcase" -> Showcase.build().steps;
                default -> Scenarios.build().steps;
            };
        }
        try {
            while (index < steps.size() && steps.get(index).tick(mc)) index++;
        } catch (Throwable t) {
            TidyPockets.LOG.error("[selftest] step {} failed", index, t);
            record("step " + index, false, t.toString());
            index = steps.size();
        }
        if (index >= steps.size()) finish(mc);
    }

    private static String regionName;
    private static int[] region;
    private static int regionStride, regionFrame, regionSeen;
    private static long regionStart;

    /** Showcase recording: every {@code stride}-th frame, the pixel rect (null = whole frame) as raw RGBA. */
    static void startRegionCapture(String name, int[] px, int stride) {
        regionName = name;
        region = px;
        regionStride = Math.max(1, stride);
        regionFrame = regionSeen = 0;
        regionStart = System.nanoTime();
    }

    static void stopRegionCapture() {
        regionName = null;
    }

    private static void captureRegion(Minecraft mc) {
        if (regionName == null || regionSeen++ % regionStride != 0) return;
        long ms = (System.nanoTime() - regionStart) / 1_000_000;
        Path out = Path.of(OUT, "showcase", regionName, String.format("f%04d_%05dms.raw", regionFrame++, ms));
        int[] r = region;
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), (NativeImage img) -> WRITER.execute(() -> {
            try (img) {
                int x0 = r == null ? 0 : Math.clamp(r[0], 0, img.getWidth() - 1), y0 = r == null ? 0 : Math.clamp(r[1], 0, img.getHeight() - 1);
                int w = r == null ? img.getWidth() : Math.min(r[2], img.getWidth() - x0), h = r == null ? img.getHeight() : Math.min(r[3], img.getHeight() - y0);
                java.nio.ByteBuffer src = img.getPixelBytes();
                byte[] buf = new byte[16 + w * h * 4];
                java.nio.ByteBuffer dst = java.nio.ByteBuffer.wrap(buf);
                dst.putInt(w).putInt(h).putInt((int) ms).putInt(img.getWidth());
                for (int y = 0; y < h; y++) {
                    int off = ((y0 + y) * img.getWidth() + x0) * 4;
                    src.get(off, buf, 16 + y * w * 4, w * 4);
                }
                Files.createDirectories(out.getParent());
                Files.write(out, buf);
            } catch (IOException e) {
                TidyPockets.LOG.warn("[showcase] frame write failed: {}", e.toString());
            }
        }));
    }

    public static void onFrame(Minecraft mc) {
        Director.frame(mc);
        captureRegion(mc);
        if (captureLeft <= 0) return;
        long ms = (System.nanoTime() - captureStart) / 1_000_000;
        Path out = Path.of(OUT, "frames", captureName, String.format("f%03d_%04dms.png", captureFrame++, ms));
        captureLeft--;
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), (NativeImage img) -> WRITER.execute(() -> {
            try (img) {
                Files.createDirectories(out.getParent());
                img.writeToFile(out);
            } catch (IOException e) {
                TidyPockets.LOG.warn("[selftest] frame write failed: {}", e.toString());
            }
        }));
    }

    static void startCapture(String name, int frames) {
        captureName = name;
        captureLeft = frames;
        captureFrame = 0;
        captureStart = System.nanoTime();
    }

    public static void record(String name, boolean pass, String detail) {
        RESULTS.add(String.format("{\"name\":\"%s\",\"pass\":%s,\"detail\":\"%s\"}",
            name, pass, detail.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")));
        TidyPockets.LOG.info("[selftest] {} {} {}", pass ? "PASS" : "FAIL", name, detail);
    }

    private static void finish(Minecraft mc) {
        finished = true;
        WRITER.shutdown();
        try {
            WRITER.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        String json = String.format("{\"loader\":\"%s\",\"mc\":\"%s\",\"results\":[%s]}%n",
            TidyPockets.platform().loader(), TidyPockets.platform().minecraftVersion(), String.join(",", RESULTS));
        try {
            Files.createDirectories(Path.of(OUT));
            Files.writeString(Path.of(OUT, "results.json"), json);
        } catch (IOException e) {
            TidyPockets.LOG.error("[selftest] could not write results", e);
        }
        mc.stop();
    }
}

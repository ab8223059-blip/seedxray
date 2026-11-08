package dev.seedxray;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class XrayScreen extends Screen {
    private final Screen parent;

    public XrayScreen(Screen parent) {
        super(Text.literal("Seed X-Ray"));
        this.parent = parent;
    }

    private static Text onOff(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        XrayConfig c = XrayConfig.INSTANCE;
        int cx = this.width / 2;
        int y = 30;

        addDrawableChild(ButtonWidget.builder(onOff("X-Ray", c.enabled), b -> {
            c.enabled = !c.enabled;
            b.setMessage(onOff("X-Ray", c.enabled));
            XrayManager.markDirty();
        }).dimensions(cx - 100, y, 200, 20).build());
        y += 22;

        addDrawableChild(ButtonWidget.builder(Text.literal("Scan radius: " + c.radius + " chunks"), b -> {
            c.radius = c.radius >= 12 ? 2 : c.radius + 2;
            b.setMessage(Text.literal("Scan radius: " + c.radius + " chunks"));
            XrayManager.markDirty();
        }).dimensions(cx - 100, y, 200, 20).build());
        y += 26;

        OreType[] types = OreType.values();
        for (int i = 0; i < types.length; i++) {
            OreType t = types[i];
            int col = i % 2, row = i / 2;
            addDrawableChild(ButtonWidget.builder(onOff(t.label, c.isOn(t)), b -> {
                c.set(t, !c.isOn(t));
                b.setMessage(onOff(t.label, c.isOn(t)));
                XrayManager.markDirty();
            }).dimensions(cx - 100 + col * 102, y + row * 22, 98, 20).build());
        }
        y += ((types.length + 1) / 2) * 22 + 4;

        addDrawableChild(ButtonWidget.builder(Text.literal("All ores ON"), b -> setAll(true))
                .dimensions(cx - 100, y, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("All ores OFF"), b -> setAll(false))
                .dimensions(cx + 2, y, 98, 20).build());
        y += 22;

        addDrawableChild(ButtonWidget.builder(Text.literal("Max entity distance"), b -> {
            this.client.options.getEntityDistanceScaling().setValue(5.0);
            this.client.options.write();
        }).dimensions(cx - 100, y, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx + 2, y, 98, 20).build());
    }

    private void setAll(boolean v) {
        for (OreType t : OreType.values()) XrayConfig.INSTANCE.set(t, v);
        XrayManager.markDirty();
        this.clearAndInit();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        XrayConfig.INSTANCE.save();
        this.client.setScreen(parent);
    }
}

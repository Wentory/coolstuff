package com.wentory.coolstuff.client;

import com.wentory.coolstuff.config.CoolstuffClientConfig;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CoolstuffListConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 23;
    private static final int CONTENT_ROWS = 24;
    private final Screen parent;
    private final List<LabelRow> labels = new ArrayList<>();
    private final List<RestartWarning> restartWarnings = new ArrayList<>();
    private int scrollRow;
    private int visibleRows;

    public CoolstuffListConfigScreen(Screen parent) {
        super(Component.translatable("coolstuff.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        visibleRows = Math.max(4, (height - 67) / ROW_HEIGHT);
        scrollRow = Math.min(scrollRow, maxScroll());
        buildList();
    }

    private void buildList() {
        clearWidgets();
        labels.clear();
        restartWarnings.clear();
        int listWidth = Math.min(360, width - 32);
        int x = (width - listWidth) / 2;
        int row = 0;

        row = addToggle(row, x, listWidth, "coolstuff.config.enable_parry", CoolstuffConfig.ENABLE_PARRY);
        row = addSlider(row, x, listWidth, "coolstuff.config.parry_sound", CoolstuffClientConfig.PARRY_SOUND_VOLUME);
        row = addSlider(row, x, listWidth, "coolstuff.config.parry_visuals", CoolstuffClientConfig.PARRY_VISUAL_INTENSITY);
        row = addToggle(row, x, listWidth, "coolstuff.config.enable_ghast_parry", CoolstuffConfig.ENABLE_GHAST_PARRY);
        row = addPercent(row, x, listWidth, "coolstuff.config.ghast_parry_chance", CoolstuffConfig.GHAST_PARRY_CHANCE);

        row = addRestartToggle(row, x, listWidth, "coolstuff.config.enable_ultra_ghast",
                CoolstuffConfig.ENABLE_ULTRA_GHAST, RestartRequiredConfig.ultraGhast());
        row = addPercent(row, x, listWidth, "coolstuff.config.ultra_ghast_chance", CoolstuffConfig.ULTRA_GHAST_SPAWN_CHANCE);

        row = addRestartToggle(row, x, listWidth, "coolstuff.config.enable_spore_creeper",
                CoolstuffConfig.ENABLE_SPORE_CREEPER, RestartRequiredConfig.sporeCreeper());
        row = addPercent(row, x, listWidth, "coolstuff.config.spore_creeper_chance", CoolstuffConfig.SPORE_CREEPER_SPAWN_CHANCE);
        row = addPercent(row, x, listWidth, "coolstuff.config.spore_fart_chance", CoolstuffConfig.SPORE_FART_CHANCE);
        row = addToggle(row, x, listWidth, "coolstuff.config.enable_sugar", CoolstuffConfig.ENABLE_SUGAR_TRANSFORMATION);

        row = addPercent(row, x, listWidth, "coolstuff.config.shield_skeleton_chance", CoolstuffConfig.SHIELD_SKELETON_SPAWN_CHANCE);
        row = addPercent(row, x, listWidth, "coolstuff.config.shield_skeleton_parry_chance", CoolstuffConfig.SHIELD_SKELETON_PARRY_CHANCE);

        row = addToggle(row, x, listWidth, "coolstuff.config.enable_cakes", CoolstuffConfig.ENABLE_THROWABLE_CAKES);
        row = addRestartToggle(row, x, listWidth, "coolstuff.config.enable_fillings",
                CoolstuffConfig.ENABLE_CAKE_FILLINGS, RestartRequiredConfig.cakeFillings());

        row = addToggle(row, x, listWidth, "coolstuff.config.enable_black_holes", CoolstuffConfig.ENABLE_BLACK_HOLES);
        row = addToggle(row, x, listWidth, "coolstuff.config.enable_cake_pacification", CoolstuffConfig.ENABLE_CAKE_BLACK_HOLE_PACIFICATION);

        row = addToggle(row, x, listWidth, "coolstuff.config.enable_launcher", CoolstuffConfig.ENABLE_FIREBALL_LAUNCHER);
        row = addToggle(row, x, listWidth, "coolstuff.config.enable_emissive_trims", CoolstuffConfig.ENABLE_EMISSIVE_TRIMS);

        row = addRestartToggle(row, x, listWidth, "coolstuff.config.enable_frostling",
                CoolstuffConfig.ENABLE_FROSTLING, RestartRequiredConfig.frostling());
        row = addPercent(row, x, listWidth, "coolstuff.config.frostling_chance", CoolstuffConfig.FROSTLING_SPAWN_CHANCE);

        row = addRestartToggle(row, x, listWidth, "coolstuff.config.enable_zombie_wolf",
                CoolstuffConfig.ENABLE_ZOMBIE_WOLF, RestartRequiredConfig.zombieWolf());
        row = addPercent(row, x, listWidth, "coolstuff.config.zombie_wolf_chance", CoolstuffConfig.ZOMBIE_WOLF_SPAWN_CHANCE);
        row = addPercent(row, x, listWidth, "coolstuff.config.wolf_jockey_chance", CoolstuffConfig.WOLF_JOCKEY_REPLACEMENT_CHANCE);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height - 27, 200, 20).build());
    }

    private int addToggle(int index, int x, int width, String key, ModConfigSpec.BooleanValue value) {
        if (isVisible(index)) {
            int y = rowY(index);
            labels.add(new LabelRow(Component.translatable(key), x + 4, y + 6));
            addRenderableWidget(CycleButton.onOffBuilder(value.get()).displayOnlyValue()
                    .create(x + width - 68, y, 68, 20, Component.translatable(key),
                            (button, enabled) -> value.set(enabled)));
        }
        return index + 1;
    }

    private int addRestartToggle(int index, int x, int width, String key, ModConfigSpec.BooleanValue value,
                                 boolean valueAtStartup) {
        if (isVisible(index)) {
            int y = rowY(index);
            labels.add(new LabelRow(Component.translatable(key), x + 4, y + 6));
            restartWarnings.add(new RestartWarning(value, valueAtStartup, x + width - 84, y + 6));
            addRenderableWidget(CycleButton.onOffBuilder(value.get()).displayOnlyValue()
                    .create(x + width - 68, y, 68, 20, Component.translatable(key),
                            (button, enabled) -> value.set(enabled)));
        }
        return index + 1;
    }

    private int addPercent(int index, int x, int width, String key, ModConfigSpec.DoubleValue value) {
        if (isVisible(index)) {
            int y = rowY(index);
            labels.add(new LabelRow(Component.translatable(key + ".label").append(" (0-100)"), x + 4, y + 6));
            EditBox field = new EditBox(font, x + width - 68, y, 68, 20, Component.translatable(key));
            field.setMaxLength(6);
            field.setFilter(text -> text.matches("\\d{0,3}([.,]\\d{0,2})?"));
            field.setValue(formatPercent(value.get()));
            field.setResponder(text -> applyPercent(text, value));
            addRenderableWidget(field);
        }
        return index + 1;
    }

    private int addSlider(int index, int x, int width, String key, ModConfigSpec.DoubleValue value) {
        if (isVisible(index)) {
            int y = rowY(index);
            labels.add(new LabelRow(Component.translatable(key + ".label"), x + 4, y + 6));
            addRenderableWidget(new PercentSlider(x + width - 140, y, 140, key, value));
        }
        return index + 1;
    }

    private static String formatPercent(double value) {
        double percent = value * 100.0;
        if (percent == Math.rint(percent)) return Integer.toString((int) percent);
        return String.format(Locale.ROOT, "%.2f", percent).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static void applyPercent(String text, ModConfigSpec.DoubleValue value) {
        if (text.isBlank()) return;
        try {
            double percent = Double.parseDouble(text.replace(',', '.'));
            value.set(Math.max(0.0, Math.min(100.0, percent)) / 100.0);
        } catch (NumberFormatException ignored) {
        }
    }

    private boolean isVisible(int index) {
        return index >= scrollRow && index < scrollRow + visibleRows;
    }

    private int rowY(int index) {
        return 27 + (index - scrollRow) * ROW_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, CONTENT_ROWS - visibleRows);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int old = scrollRow;
        scrollRow = Math.max(0, Math.min(maxScroll(), scrollRow - (int) Math.signum(scrollY)));
        if (old != scrollRow) buildList();
        return old != scrollRow || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        for (LabelRow label : labels) graphics.drawString(font, label.text(), label.x(), label.y(), 0xFFFFFF, false);
        for (RestartWarning warning : restartWarnings) {
            if (warning.value().get() == warning.valueAtStartup()) continue;
            graphics.drawString(font, "!", warning.x(), warning.y(), 0xFFFFFF00, false);
            if (mouseX >= warning.x() - 4 && mouseX <= warning.x() + 10
                    && mouseY >= warning.y() - 4 && mouseY <= warning.y() + 13) {
                graphics.renderTooltip(font, Component.translatable("coolstuff.config.restart_required"), mouseX, mouseY);
            }
        }
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        if (maxScroll() > 0) {
            int top = 27;
            int bottom = height - 34;
            int trackX = width / 2 + Math.min(360, width - 32) / 2 + 7;
            int trackHeight = bottom - top;
            int thumbHeight = Math.max(18, trackHeight * visibleRows / CONTENT_ROWS);
            int thumbY = top + (trackHeight - thumbHeight) * scrollRow / maxScroll();
            graphics.fill(trackX, top, trackX + 3, bottom, 0x55333333);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        if (CoolstuffClientConfig.SPEC.isLoaded()) CoolstuffClientConfig.SPEC.save();
        if (CoolstuffConfig.SPEC.isLoaded()) CoolstuffConfig.SPEC.save();
        minecraft.setScreen(parent);
    }

    private record LabelRow(Component text, int x, int y) {
    }

    private record RestartWarning(ModConfigSpec.BooleanValue value, boolean valueAtStartup, int x, int y) {
    }

    private static final class PercentSlider extends AbstractSliderButton {
        private final String translationKey;
        private final ModConfigSpec.DoubleValue configValue;

        private PercentSlider(int x, int y, int width, String translationKey,
                              ModConfigSpec.DoubleValue configValue) {
            super(x, y, width, 20, Component.empty(), configValue.get());
            this.translationKey = translationKey;
            this.configValue = configValue;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(translationKey, Math.round(value * 100.0)));
        }

        @Override
        protected void applyValue() {
            configValue.set(value);
        }
    }
}

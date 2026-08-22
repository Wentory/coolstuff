package com.wentory.coolstuff.client;

import com.wentory.coolstuff.config.CoolstuffClientConfig;
import com.wentory.coolstuff.config.CoolstuffConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class CoolstuffConfigScreen extends Screen {
    private final Screen parent;

    public CoolstuffConfigScreen(Screen parent) {
        super(Component.translatable("coolstuff.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int columnWidth = Math.min(200, (width - 24) / 2);
        int left = width / 2 - columnWidth - 4;
        int right = width / 2 + 4;
        int top = 20;
        int row = 21;
        addToggle(left, top, columnWidth, "coolstuff.config.enable_parry", CoolstuffConfig.ENABLE_PARRY);
        addToggle(left, top + row, columnWidth, "coolstuff.config.enable_ghast_parry", CoolstuffConfig.ENABLE_GHAST_PARRY);
        addToggle(left, top + row * 2, columnWidth, "coolstuff.config.enable_ultra_ghast", CoolstuffConfig.ENABLE_ULTRA_GHAST);
        addToggle(left, top + row * 3, columnWidth, "coolstuff.config.enable_spore_creeper", CoolstuffConfig.ENABLE_SPORE_CREEPER);
        addToggle(left, top + row * 4, columnWidth, "coolstuff.config.enable_sugar", CoolstuffConfig.ENABLE_SUGAR_TRANSFORMATION);
        addToggle(right, top, columnWidth, "coolstuff.config.enable_cakes", CoolstuffConfig.ENABLE_THROWABLE_CAKES);
        addToggle(right, top + row, columnWidth, "coolstuff.config.enable_fillings", CoolstuffConfig.ENABLE_CAKE_FILLINGS);
        addToggle(right, top + row * 2, columnWidth, "coolstuff.config.enable_black_holes", CoolstuffConfig.ENABLE_BLACK_HOLES);
        addToggle(right, top + row * 3, columnWidth, "coolstuff.config.enable_cake_pacification", CoolstuffConfig.ENABLE_CAKE_BLACK_HOLE_PACIFICATION);
        addToggle(right, top + row * 4, columnWidth, "coolstuff.config.enable_launcher", CoolstuffConfig.ENABLE_FIREBALL_LAUNCHER);
        addToggle(left, top + row * 5, columnWidth, "coolstuff.config.enable_zombie_wolf", CoolstuffConfig.ENABLE_ZOMBIE_WOLF);

        int sliderTop = top + row * 6 + 5;
        addRenderableWidget(new ConfigSlider(left, sliderTop, columnWidth,
                "coolstuff.config.ghast_parry_chance", CoolstuffConfig.GHAST_PARRY_CHANCE.get(),
                CoolstuffConfig.GHAST_PARRY_CHANCE));
        addRenderableWidget(new ConfigSlider(right, sliderTop, columnWidth,
                "coolstuff.config.ultra_ghast_chance", CoolstuffConfig.ULTRA_GHAST_SPAWN_CHANCE.get(),
                CoolstuffConfig.ULTRA_GHAST_SPAWN_CHANCE));
        addRenderableWidget(new ConfigSlider(left, sliderTop + row, columnWidth,
                "coolstuff.config.spore_creeper_chance", CoolstuffConfig.SPORE_CREEPER_SPAWN_CHANCE.get(),
                CoolstuffConfig.SPORE_CREEPER_SPAWN_CHANCE));
        addRenderableWidget(new ConfigSlider(right, sliderTop + row, columnWidth,
                "coolstuff.config.spore_fart_chance", CoolstuffConfig.SPORE_FART_CHANCE.get(),
                CoolstuffConfig.SPORE_FART_CHANCE));
        addRenderableWidget(new ConfigSlider(left, sliderTop + row * 2, columnWidth,
                "coolstuff.config.shield_skeleton_chance", CoolstuffConfig.SHIELD_SKELETON_SPAWN_CHANCE.get(),
                CoolstuffConfig.SHIELD_SKELETON_SPAWN_CHANCE));
        addRenderableWidget(new ConfigSlider(right, sliderTop + row * 2, columnWidth,
                "coolstuff.config.shield_skeleton_parry_chance", CoolstuffConfig.SHIELD_SKELETON_PARRY_CHANCE.get(),
                CoolstuffConfig.SHIELD_SKELETON_PARRY_CHANCE));
        addRenderableWidget(new ConfigSlider(left, sliderTop + row * 3, columnWidth,
                "coolstuff.config.parry_sound", CoolstuffClientConfig.PARRY_SOUND_VOLUME.get(),
                CoolstuffClientConfig.PARRY_SOUND_VOLUME));
        addRenderableWidget(new ConfigSlider(right, sliderTop + row * 3, columnWidth,
                "coolstuff.config.parry_visuals", CoolstuffClientConfig.PARRY_VISUAL_INTENSITY.get(),
                CoolstuffClientConfig.PARRY_VISUAL_INTENSITY));
        addRenderableWidget(new ConfigSlider(left, sliderTop + row * 4, columnWidth,
                "coolstuff.config.zombie_wolf_chance", CoolstuffConfig.ZOMBIE_WOLF_SPAWN_CHANCE.get(),
                CoolstuffConfig.ZOMBIE_WOLF_SPAWN_CHANCE));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, sliderTop + row * 5 + 3, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 7, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (CoolstuffClientConfig.SPEC.isLoaded()) CoolstuffClientConfig.SPEC.save();
        if (CoolstuffConfig.SPEC.isLoaded()) CoolstuffConfig.SPEC.save();
        minecraft.setScreen(parent);
    }

    private void addToggle(int x, int y, int buttonWidth, String translationKey,
                           net.neoforged.neoforge.common.ModConfigSpec.BooleanValue configValue) {
        addRenderableWidget(CycleButton.onOffBuilder(configValue.get())
                .create(x, y, buttonWidth, 20, Component.translatable(translationKey),
                        (button, enabled) -> configValue.set(enabled)));
    }

    private static final class ConfigSlider extends AbstractSliderButton {
        private final String translationKey;
        private final net.neoforged.neoforge.common.ModConfigSpec.DoubleValue configValue;

        private ConfigSlider(int x, int y, int width, String translationKey, double value,
                             net.neoforged.neoforge.common.ModConfigSpec.DoubleValue configValue) {
            super(x, y, width, 20, Component.empty(), value);
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

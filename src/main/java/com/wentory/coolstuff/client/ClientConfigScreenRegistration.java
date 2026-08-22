package com.wentory.coolstuff.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ClientConfigScreenRegistration {
    private ClientConfigScreenRegistration() {
    }

    public static void register(ModContainer container) {
        IConfigScreenFactory factory = (modContainer, parent) -> new CoolstuffListConfigScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}

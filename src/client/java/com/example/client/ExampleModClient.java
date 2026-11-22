package com.example;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {
    private static KeyMapping toggle;

    @Override
    public void onInitializeClient() {
        toggle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.crystalcheck.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggle.consumeClick()) {
                ExampleMod.enabled = !ExampleMod.enabled;
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal(
                        "CrystalCheck " + (ExampleMod.enabled ? "ON" : "OFF")), true);
                }
            }
        });
    }
}

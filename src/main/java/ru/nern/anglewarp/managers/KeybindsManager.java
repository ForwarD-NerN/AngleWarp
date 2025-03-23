package ru.nern.anglewarp.managers;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindsManager {
    public static KeyBinding showWarpsKey;

    public static void init() {
        showWarpsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.anglewarp.show_warps", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_R, // The keycode of the key
                KeyBinding.MISC_CATEGORY
        ));
    }
}

package ru.nern.anglewarp.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.nern.anglewarp.AngleWarp;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void anglewarp$blockMouseMovements(double timeDelta, CallbackInfo ci) {
        if(AngleWarp.mouseBlocked) ci.cancel();
    }
}

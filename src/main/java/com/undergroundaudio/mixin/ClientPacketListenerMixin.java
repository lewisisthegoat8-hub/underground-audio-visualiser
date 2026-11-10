package com.undergroundaudio.mixin;

import com.undergroundaudio.UndergroundAudioVisualizer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void uav$onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        UndergroundAudioVisualizer.onSound(packet);
    }
}

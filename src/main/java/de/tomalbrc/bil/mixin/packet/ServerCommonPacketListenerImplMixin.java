package de.tomalbrc.bil.mixin.packet;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.tomalbrc.bil.util.AsyncTickThread;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
    // Vanilla holds a packet for the end-of-tick flush only when sent on the server thread. Async
    // ticks are joined before flushing resumes (MinecraftServerMixin), so hold theirs as well.
    @ModifyExpressionValue(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isSameThread()Z")
    )
    private boolean bil$holdFlushOnAsyncTick(boolean isSameThread) {
        return isSameThread || AsyncTickThread.isCurrent();
    }
}

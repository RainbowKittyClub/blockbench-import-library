package de.tomalbrc.bil.core.holder.base;

import de.tomalbrc.bil.util.AsyncTickThread;
import de.tomalbrc.bil.util.IChunkMap;
import de.tomalbrc.bil.util.Utils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Base class for all holder that handles Polymer's ElementHolder specific logic.
 * <p>
 * This class mainly exists to split off ElementHolder logic from the element logic.
 */
public abstract class AbstractElementHolder extends ElementHolder {
    // Rebuilt on the server thread at every watch change; async ticks read it instead of Polymer's
    // live list, which the server thread may change under them.
    protected volatile ServerGamePacketListenerImpl[] watchingPlayers;
    protected boolean isDataLoaded;

    @Deprecated(forRemoval = true)
    protected AbstractElementHolder(ServerLevel unused) {
        this();
    }

    protected AbstractElementHolder() {
        super();
        this.watchingPlayers = Utils.EMPTY_CONNECTION_ARRAY;
    }

    abstract protected void onAsyncTick();

    abstract protected void onDataLoaded();

    abstract protected boolean shouldSkipTick();

    @Override
    public boolean startWatching(ServerGamePacketListenerImpl player) {
        if (!this.isDataLoaded) {
            this.isDataLoaded = true;
            this.onDataLoaded();
        }

        var result = super.startWatching(player);
        this.watchingPlayers = this.getWatchingPlayers().toArray(Utils.EMPTY_CONNECTION_ARRAY);
        return result;
    }

    @Override
    public boolean stopWatching(ServerGamePacketListenerImpl player) {
        boolean result = super.stopWatching(player);
        this.watchingPlayers = this.getWatchingPlayers().toArray(Utils.EMPTY_CONNECTION_ARRAY);
        return result;
    }

    @Override
    public final void tick() {
        if (this.getAttachment() == null || this.shouldSkipTick()) {
            return;
        }

        this.onTick();

        this.updatePosition();

        // Schedule an async tick for this holder.
        IChunkMap.scheduleAsyncTick(this);
    }

    public ServerGamePacketListenerImpl[] getWatchingPlayerSnapshot() {
        return this.watchingPlayers;
    }

    @Override
    public void sendPacket(Packet<? extends ClientGamePacketListener> packet) {
        if (!AsyncTickThread.isCurrent()) {
            super.sendPacket(packet);
            return;
        }

        for (ServerGamePacketListenerImpl connection : this.watchingPlayers) {
            connection.send(packet);
        }
    }

    @Override
    public void sendPacket(Packet<? extends ClientGamePacketListener> packet, Predicate<ServerPlayer> predicate) {
        if (!AsyncTickThread.isCurrent()) {
            super.sendPacket(packet, predicate);
            return;
        }

        for (ServerGamePacketListenerImpl connection : this.watchingPlayers) {
            if (predicate.test(connection.getPlayer())) {
                connection.send(packet);
            }
        }
    }

    public final void asyncTick() {
        this.onAsyncTick();

        for (VirtualElement element : this.getElements()) {
            element.tick();
        }
    }

    public @Nullable ServerLevel getLevel() {
        return this.getAttachment() != null ? this.getAttachment().getWorld() : null;
    }
}

package de.tomalbrc.bil.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import eu.pb4.polymer.virtualentity.api.data.EntityData;
import eu.pb4.polymer.virtualentity.api.data.InteractionEntityData;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

public class Utils {
    public static final ServerGamePacketListenerImpl[] EMPTY_CONNECTION_ARRAY = new ServerGamePacketListenerImpl[0];
    public static final Vector3fc ZERO_VEC3F = new Vector3f();

    public static CommandSyntaxException buildCommandException(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }

    public static int toSlimeSize(float size) {
        return Mth.floor(size / 2.04F / 0.255F);
    }

    public static boolean getSharedFlag(byte value, int flag) {
        return (value & 1 << flag) != 0;
    }

    public static List<Packet<? super @NotNull ClientGamePacketListener>> updateClientInteraction(InteractionElement interaction, EntityDimensions dimensions) {
        return updateClientInteraction(interaction, dimensions, dimensions.height());
    }

    public static List<Packet<? super ClientGamePacketListener>> updateClientInteraction(InteractionElement interaction, EntityDimensions dimensions, float height) {
        // Updates the dimensions and bounding box of the interaction on the client. Note that the interactions dimensions and bounding box are two different things.
        // - The bounding box is primarily used for detecting player attacks, interactions and rendering the hitbox.
        // - The dimensions are used for certain other properties, such as the passenger riding height or the fire animation.
        return List.of(
                // We update the POSE in this packet, which makes the client refresh the interactions dimensions.
                // We use this to move the passenger riding height of the interaction upwards.
                new ClientboundSetEntityDataPacket(interaction.getEntityId(), List.of(
                        SynchedEntityData.DataValue.create(InteractionEntityData.HEIGHT, height),
                        SynchedEntityData.DataValue.create(InteractionEntityData.WIDTH, dimensions.width()),
                        SynchedEntityData.DataValue.create(EntityData.POSE, Pose.STANDING)
                )),
                // Afterward, we send another packet that only updates the bounding box height back to its original value, without updating its dimensions.
                // This lets us turn the bounding box back into the correct size whilst keeping the raised passenger riding height.
                new ClientboundSetEntityDataPacket(interaction.getEntityId(), List.of(
                        SynchedEntityData.DataValue.create(InteractionEntityData.HEIGHT, dimensions.height())
                ))
        );
    }

    /**
     * Creates a Quaternionf from euler angles
     * @param eulerAngles
     * @return
     */
    public static Quaternionf createQuaternion(Vector3fc eulerAngles) {
        return new Quaternionf()
                .rotateZ(Mth.DEG_TO_RAD * eulerAngles.z())
                .rotateY(Mth.DEG_TO_RAD * eulerAngles.y())
                .rotateX(Mth.DEG_TO_RAD * eulerAngles.x());
    }

    public static Quaternionf createQuaternion(Vector3f eulerAngles) {
        return new Quaternionf()
                .rotateZ(Mth.DEG_TO_RAD * eulerAngles.z())
                .rotateY(Mth.DEG_TO_RAD * eulerAngles.y())
                .rotateX(Mth.DEG_TO_RAD * eulerAngles.x());
    }
}

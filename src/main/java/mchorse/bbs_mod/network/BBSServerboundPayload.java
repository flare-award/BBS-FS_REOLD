package mchorse.bbs_mod.network;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The client to server half of the mod's packets. See {@link BBSClientboundPayload} for why the
 * mod's channels are folded into one payload type instead of being registered one by one.
 */
public record BBSServerboundPayload(Identifier channel, byte[] data) implements CustomPayload
{
    public static final CustomPayload.Id<BBSServerboundPayload> ID =
        new CustomPayload.Id<>(Identifier.of(BBSMod.MOD_ID, "payload_c2s"));

    public static final PacketCodec<RegistryByteBuf, BBSServerboundPayload> CODEC = PacketCodec.ofStatic(
        (buf, payload) ->
        {
            buf.writeIdentifier(payload.channel());
            buf.writeBytes(payload.data());
        },
        buf ->
        {
            Identifier channel = buf.readIdentifier();
            byte[] data = new byte[buf.readableBytes()];

            buf.readBytes(data);

            return new BBSServerboundPayload(channel, data);
        }
    );

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }
}

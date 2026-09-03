package mchorse.bbs_mod.network;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The server to client half of the mod's packets.
 *
 * <p>Since 1.20.5 Fabric's networking only carries typed {@link CustomPayload} records, so the old
 * "an {@link Identifier} plus a raw buffer" scheme rides on top of a single payload type per
 * direction: the real channel id goes first and everything left in the buffer is the original
 * bytes verbatim. Handlers downstream still see an identifier and a {@code PacketByteBuf}, which
 * is what kept the migration to a couple of files.</p>
 *
 * <p>The length is never written out because the channel id is self-delimiting — reading "whatever
 * is left" sidesteps {@code PacketByteBuf}'s one megabyte array cap, and a film sync packet can
 * carry considerably more than that.</p>
 */
public record BBSClientboundPayload(Identifier channel, byte[] data) implements CustomPayload
{
    public static final CustomPayload.Id<BBSClientboundPayload> ID =
        new CustomPayload.Id<>(Identifier.of(BBSMod.MOD_ID, "payload_s2c"));

    public static final PacketCodec<RegistryByteBuf, BBSClientboundPayload> CODEC = PacketCodec.ofStatic(
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

            return new BBSClientboundPayload(channel, data);
        }
    );

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }
}

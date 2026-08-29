package mchorse.bbs_mod.actions;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class SuperFakePlayerNetworkHandler extends ServerPlayNetworkHandler
{
    private static final ClientConnection FAKE_CONNECTION = new ClientConnection(NetworkSide.CLIENTBOUND);

    public SuperFakePlayerNetworkHandler(ServerPlayerEntity player)
    {
        super(player.getServer(), FAKE_CONNECTION, player, ConnectedClientData.createDefault(player.getGameProfile(), false));
    }

    /* A fake player has no real socket behind it, so everything it would send is dropped. */
    @Override
    public void sendPacket(Packet<?> packet)
    {}
}
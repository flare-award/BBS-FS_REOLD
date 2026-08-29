package mchorse.bbs_mod.network;

import mchorse.bbs_mod.utils.ItemNbtUtils;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.ActionManager;
import mchorse.bbs_mod.actions.ActionPlayer;
import mchorse.bbs_mod.actions.ActionRecorder;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.actions.PlayerType;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ByteType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.entity.IEntityFormProvider;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.EnumUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PermissionUtils;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.repos.RepositoryOperation;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import io.netty.buffer.Unpooled;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerNetwork
{
    public static final int STATE_TRIGGER_MORPH = 0;
    public static final int STATE_TRIGGER_MAIN_HAND_ITEM = 1;
    public static final int STATE_TRIGGER_OFF_HAND_ITEM = 2;

    public static final Identifier CLIENT_CLICKED_MODEL_BLOCK_PACKET = Identifier.of(BBSMod.MOD_ID, "c1");
    public static final Identifier CLIENT_PLAYER_FORM_PACKET = Identifier.of(BBSMod.MOD_ID, "c2");
    public static final Identifier CLIENT_PLAY_FILM_PACKET = Identifier.of(BBSMod.MOD_ID, "c3");
    public static final Identifier CLIENT_MANAGER_DATA_PACKET = Identifier.of(BBSMod.MOD_ID, "c4");
    public static final Identifier CLIENT_STOP_FILM_PACKET = Identifier.of(BBSMod.MOD_ID, "c5");
    public static final Identifier CLIENT_HANDSHAKE = Identifier.of(BBSMod.MOD_ID, "c6");
    public static final Identifier CLIENT_RECORDED_ACTIONS = Identifier.of(BBSMod.MOD_ID, "c7");
    public static final Identifier CLIENT_ANIMATION_STATE_TRIGGER = Identifier.of(BBSMod.MOD_ID, "c8");
    public static final Identifier CLIENT_CHEATS_PERMISSION = Identifier.of(BBSMod.MOD_ID, "c9");
    public static final Identifier CLIENT_SHARED_FORM = Identifier.of(BBSMod.MOD_ID, "c10");
    public static final Identifier CLIENT_ENTITY_FORM = Identifier.of(BBSMod.MOD_ID, "c11");
    public static final Identifier CLIENT_ACTORS = Identifier.of(BBSMod.MOD_ID, "c12");
    public static final Identifier CLIENT_GUN_PROPERTIES = Identifier.of(BBSMod.MOD_ID, "c13");
    public static final Identifier CLIENT_PAUSE_FILM = Identifier.of(BBSMod.MOD_ID, "c14");
    public static final Identifier CLIENT_SELECTED_SLOT = Identifier.of(BBSMod.MOD_ID, "c15");
    public static final Identifier CLIENT_ANIMATION_STATE_MODEL_BLOCK_TRIGGER = Identifier.of(BBSMod.MOD_ID, "c16");
    public static final Identifier CLIENT_REFRESH_MODEL_BLOCKS = Identifier.of(BBSMod.MOD_ID, "c17");
    public static final Identifier CLIENT_REQUEST_FILM_RESYNC = Identifier.of(BBSMod.MOD_ID, "c18");

    public static final Identifier SERVER_MODEL_BLOCK_FORM_PACKET = Identifier.of(BBSMod.MOD_ID, "s1");
    public static final Identifier SERVER_MODEL_BLOCK_TRANSFORMS_PACKET = Identifier.of(BBSMod.MOD_ID, "s2");
    public static final Identifier SERVER_PLAYER_FORM_PACKET = Identifier.of(BBSMod.MOD_ID, "s3");
    public static final Identifier SERVER_MANAGER_DATA_PACKET = Identifier.of(BBSMod.MOD_ID, "s4");
    public static final Identifier SERVER_ACTION_RECORDING = Identifier.of(BBSMod.MOD_ID, "s5");
    public static final Identifier SERVER_TOGGLE_FILM = Identifier.of(BBSMod.MOD_ID, "s6");
    public static final Identifier SERVER_ACTION_CONTROL = Identifier.of(BBSMod.MOD_ID, "s7");
    public static final Identifier SERVER_FILM_DATA_SYNC = Identifier.of(BBSMod.MOD_ID, "s8");
    public static final Identifier SERVER_PLAYER_TP = Identifier.of(BBSMod.MOD_ID, "s9");
    public static final Identifier SERVER_ANIMATION_STATE_TRIGGER = Identifier.of(BBSMod.MOD_ID, "s10");
    public static final Identifier SERVER_SHARED_FORM = Identifier.of(BBSMod.MOD_ID, "s11");
    public static final Identifier SERVER_ZOOM = Identifier.of(BBSMod.MOD_ID, "s12");
    public static final Identifier SERVER_PAUSE_FILM = Identifier.of(BBSMod.MOD_ID, "s13");
    public static final Identifier SERVER_APPLY_FILM_PLAYER_SETTINGS = Identifier.of(BBSMod.MOD_ID, "s14");

    private static ServerPacketCrusher crusher = new ServerPacketCrusher();

    public static void reset()
    {
        crusher.reset();
    }

    public static void setup()
    {
        PayloadTypeRegistry.playC2S().register(BBSServerboundPayload.ID, BBSServerboundPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BBSClientboundPayload.ID, BBSClientboundPayload.CODEC);

        /* Every channel arrives inside the one payload type, so it is unwrapped here and handed to
         * the same handlers the mod always had. */
        ServerPlayNetworking.registerGlobalReceiver(BBSServerboundPayload.ID, (payload, context) ->
        {
            MinecraftServer server = context.server();
            ServerPlayerEntity player = context.player();
            PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()));
            Identifier channel = payload.channel();

            if (channel.equals(SERVER_MODEL_BLOCK_FORM_PACKET))
            {
                handleModelBlockFormPacket(server, player, buf);
            }
            else if (channel.equals(SERVER_MODEL_BLOCK_TRANSFORMS_PACKET))
            {
                handleModelBlockTransformsPacket(server, player, buf);
            }
            else if (channel.equals(SERVER_PLAYER_FORM_PACKET))
            {
                handlePlayerFormPacket(server, player, buf);
            }
            else if (channel.equals(SERVER_MANAGER_DATA_PACKET))
            {
                handleManagerDataPacket(server, player, buf);
            }
            else if (channel.equals(SERVER_ACTION_RECORDING))
            {
                handleActionRecording(server, player, buf);
            }
            else if (channel.equals(SERVER_TOGGLE_FILM))
            {
                handleToggleFilm(server, player, buf);
            }
            else if (channel.equals(SERVER_ACTION_CONTROL))
            {
                handleActionControl(server, player, buf);
            }
            else if (channel.equals(SERVER_FILM_DATA_SYNC))
            {
                handleSyncData(server, player, buf);
            }
            else if (channel.equals(SERVER_PLAYER_TP))
            {
                handleTeleportPlayer(server, player, buf);
            }
            else if (channel.equals(SERVER_ANIMATION_STATE_TRIGGER))
            {
                handleAnimationStateTriggerPacket(server, player, buf);
            }
            else if (channel.equals(SERVER_SHARED_FORM))
            {
                handleSharedFormPacket(server, player, buf);
            }
            else if (channel.equals(SERVER_ZOOM))
            {
                handleZoomPacket(server, player, buf);
            }
            else if (channel.equals(SERVER_PAUSE_FILM))
            {
                handlePauseFilmPacket(server, player, buf);
            }
            else if (channel.equals(SERVER_APPLY_FILM_PLAYER_SETTINGS))
            {
                handleApplyFilmPlayerSettings(server, player, buf);
            }
        });
    }

    /** Wraps a raw buffer in the mod's payload type - the one shape 1.20.5+ networking accepts. */
    public static void send(ServerPlayerEntity player, Identifier channel, PacketByteBuf buf)
    {
        byte[] data = new byte[buf.readableBytes()];

        buf.getBytes(buf.readerIndex(), data);

        send(player, new BBSClientboundPayload(channel, data));
    }

    /* Handlers */

    private static void handleModelBlockFormPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            BlockPos pos = buf.readBlockPos();

            try
            {
                MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

                server.execute(() ->
                {
                    World world = player.getWorld();
                    BlockEntity be = world.getBlockEntity(pos);

                    if (be instanceof ModelBlockEntity modelBlock)
                    {
                        modelBlock.updateForm(data, world);
                    }
                });
            }
            catch (Exception e)
            {}
        });
    }

    private static void handleModelBlockTransformsPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            try
            {
                MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

                server.execute(() ->
                {
                    ItemStack stack = player.getEquippedStack(EquipmentSlot.MAINHAND).copy();

                    if (stack.getItem() == BBSMod.MODEL_BLOCK_ITEM) ItemNbtUtils.putBlockEntityData(stack, "Properties", DataStorageUtils.toNbt(data));
                    else if (stack.getItem() == BBSMod.GUN_ITEM) ItemNbtUtils.putCustomData(stack, "GunData", DataStorageUtils.toNbt(data));

                    player.equipStack(EquipmentSlot.MAINHAND, stack);
                });
            }
            catch (Exception e)
            {}
        });
    }

    private static void handlePlayerFormPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            Form form = null;

            try
            {
                if (DataStorageUtils.readFromBytes(bytes) instanceof MapType data)
                {
                    form = BBSMod.getForms().fromData(data);
                }
            }
            catch (Exception e)
            {}

            final Form finalForm = form;

            server.execute(() ->
            {
                Morph.getMorph(player).setForm(FormUtils.copy(finalForm));

                sendMorphToTracked(player, finalForm);
            });
        });
    }

    private static void handleManagerDataPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);
            int callbackId = packetByteBuf.readInt();
            RepositoryOperation op = RepositoryOperation.values()[packetByteBuf.readInt()];
            FilmManager films = BBSMod.getFilms();

            if (op == RepositoryOperation.LOAD)
            {
                String id = data.getString("id");
                Film film = films.load(id);

                sendManagerData(player, callbackId, op, film.toData());
            }
            else if (op == RepositoryOperation.SAVE)
            {
                films.save(data.getString("id"), data.getMap("data"));
            }
            else if (op == RepositoryOperation.RENAME)
            {
                films.rename(data.getString("from"), data.getString("to"));
            }
            else if (op == RepositoryOperation.DELETE)
            {
                films.delete(data.getString("id"));
            }
            else if (op == RepositoryOperation.KEYS)
            {
                ListType list = DataStorageUtils.stringListToData(films.getKeys());

                sendManagerData(player, callbackId, op, list);
            }
            else if (op == RepositoryOperation.ADD_FOLDER)
            {
                sendManagerData(player, callbackId, op, new ByteType(films.addFolder(data.getString("folder"))));
            }
            else if (op == RepositoryOperation.RENAME_FOLDER)
            {
                sendManagerData(player, callbackId, op, new ByteType(films.renameFolder(data.getString("from"), data.getString("to"))));
            }
            else if (op == RepositoryOperation.DELETE_FOLDER)
            {
                sendManagerData(player, callbackId, op, new ByteType(films.deleteFolder(data.getString("folder"))));
            }
        });
    }

    private static void handleActionRecording(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        String filmId = buf.readString();
        int replayId = buf.readInt();
        int tick = buf.readInt();
        int countdown = buf.readInt();
        boolean recording = buf.readBoolean();

        server.execute(() ->
        {
            if (recording)
            {
                Film film = BBSMod.getFilms().load(filmId);

                if (film != null)
                {
                    BBSMod.getActions().startRecording(film, player, 0, countdown, replayId);
                }
            }
            else
            {
                ActionRecorder recorder = BBSMod.getActions().stopRecording(player);

                /* Nothing was being recorded - a stop that arrived twice, or after the player
                 * was dropped from the take on disconnect. */
                if (recorder != null)
                {
                    Clips clips = recorder.composeClips();

                    /* Send recorded clips to the client */
                    sendRecordedActions(player, filmId, replayId, tick, clips);
                }
            }
        });
    }

    private static void handleToggleFilm(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        String filmId = buf.readString();
        boolean withCamera = buf.readBoolean();

        server.execute(() ->
        {
            ActionPlayer actionPlayer = BBSMod.getActions().getPlayer(filmId);

            if (actionPlayer != null)
            {
                BBSMod.getActions().stop(filmId);

                for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList())
                {
                    sendStopFilm(otherPlayer, filmId);
                }
            }
            else
            {
                sendPlayFilm(player, player.getServerWorld(), filmId, withCamera);
            }
        });
    }

    private static void handleActionControl(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        ActionManager actions = BBSMod.getActions();
        String filmId = buf.readString();
        ActionState state = EnumUtils.getValue(buf.readByte(), ActionState.values(), ActionState.STOP);
        int tick = buf.readInt();

        server.execute(() ->
        {
            if (state == ActionState.SEEK)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer != null)
                {
                    actionPlayer.goTo(tick);
                }
            }
            else if (state == ActionState.PLAY)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer != null)
                {
                    actionPlayer.goTo(tick);
                    actionPlayer.playing = true;
                }
            }
            else if (state == ActionState.PAUSE)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer != null)
                {
                    actionPlayer.goTo(tick);
                    actionPlayer.playing = false;
                }
            }
            else if (state == ActionState.RESTART)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer == null)
                {
                    Film film = BBSMod.getFilms().load(filmId);

                    if (film != null)
                    {
                        actionPlayer = actions.play(player, player.getServerWorld(), film, tick, PlayerType.FILM_EDITOR);
                    }
                }
                else
                {
                    actions.stop(filmId);

                    actionPlayer = actions.play(player, player.getServerWorld(), actionPlayer.film, tick, PlayerType.FILM_EDITOR);
                }

                if (actionPlayer != null)
                {
                    actionPlayer.syncing = true;
                    actionPlayer.playing = false;

                    if (tick != 0)
                    {
                        actionPlayer.goTo(0, tick);
                    }
                }

                sendStopFilm(player, filmId);
            }
            else if (state == ActionState.STOP)
            {
                actions.stop(filmId);
            }
        });
    }

    private static void handleSyncData(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            String filmId = packetByteBuf.readString();
            List<String> path = new ArrayList<>();

            for (int i = 0, c = buf.readInt(); i < c; i++)
            {
                path.add(buf.readString());
            }

            BaseType data = DataStorageUtils.readFromBytes(bytes);

            server.execute(() ->
            {
                BBSMod.getActions().syncData(filmId, new DataPath(path), data);
            });
        });
    }

    private static void handleTeleportPlayer(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float yaw = buf.readFloat();
        float bodyYaw = buf.readFloat();
        float pitch = buf.readFloat();

        server.execute(() ->
        {
            player.requestTeleport(x, y, z);

            player.setYaw(yaw);
            player.setHeadYaw(yaw);
            player.setBodyYaw(bodyYaw);
            player.setPitch(pitch);
        });
    }

    private static void handleAnimationStateTriggerPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        String string = buf.readString();
        int type = buf.readInt();
        PacketByteBuf newBuf = PacketByteBufs.create();

        newBuf.writeInt(player.getId());
        newBuf.writeString(string);
        newBuf.writeInt(type);

        for (ServerPlayerEntity otherPlayer : PlayerLookup.tracking(player))
        {
            send(otherPlayer, CLIENT_ANIMATION_STATE_TRIGGER, newBuf);
        }

        server.execute(() ->
        {
            /* TODO: State Triggers */
        });
    }

    private static void handleSharedFormPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            UUID playerUuid = packetByteBuf.readUuid();
            MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

            server.execute(() ->
            {
                ServerPlayerEntity otherPlayer = server.getPlayerManager().getPlayer(playerUuid);

                if (otherPlayer != null)
                {
                    sendSharedForm(otherPlayer, data);
                }
            });
        });
    }

    private static void handleZoomPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        boolean zoom = buf.readBoolean();
        ItemStack main = player.getMainHandStack();

        if (main.getItem() == BBSMod.GUN_ITEM)
        {
            GunProperties properties = GunProperties.get(main);
            String command = zoom ? properties.cmdZoomOn : properties.cmdZoomOff;

            if (!command.isEmpty())
            {
                server.getCommandManager().executeWithPrefix(player.getCommandSource(), command);
            }
        }
    }

    private static void handlePauseFilmPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        String filmId = buf.readString();

        ActionPlayer actionPlayer = BBSMod.getActions().getPlayer(filmId);

        if (actionPlayer != null)
        {
            actionPlayer.toggle();
        }

        for (ServerPlayerEntity playerEntity : server.getPlayerManager().getPlayerList())
        {
            sendPauseFilm(playerEntity, filmId);
        }
    }

    private static void handleApplyFilmPlayerSettings(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        float hp = buf.readFloat();
        float hunger = buf.readFloat();
        int xpLevel = buf.readInt();
        float xpProgress = buf.readFloat();
        int selectedSlot = buf.readInt();
        int dressSize = buf.readInt();
        byte[] dressBytes = dressSize > 0 ? new byte[dressSize] : null;

        if (dressBytes != null)
        {
            buf.readBytes(dressBytes);
        }

        final byte[] dressBytesFinal = dressBytes;

        server.execute(() ->
        {
            ActionPlayer.applyFilmPlayerSettingsTo(player, hp, hunger, xpLevel, xpProgress);

            if (dressBytesFinal != null)
            {
                BaseType dressData = DataStorageUtils.readFromBytes(dressBytesFinal);

                if (dressData.isList())
                {
                    ReplayKeyframes.applyPackedEquipment(new MCEntity(player), dressData.asList());

                    sendSelectedSlot(player, MathUtils.clamp(selectedSlot, 0, ReplayKeyframes.HOTBAR_SIZE - 1));
                }
            }
        });
    }

    /* API */

    public static void sendMorph(ServerPlayerEntity player, int playerId, Form form)
    {
        crusher.send(player, CLIENT_PLAYER_FORM_PACKET, FormUtils.toData(form), (packetByteBuf) ->
        {
            packetByteBuf.writeInt(playerId);
        });
    }

    public static void sendMorphToTracked(ServerPlayerEntity player, Form form)
    {
        sendMorph(player, player.getId(), form);

        for (ServerPlayerEntity otherPlayer : PlayerLookup.tracking(player))
        {
            sendMorph(otherPlayer, player.getId(), form);
        }
    }

    public static void sendClickedModelBlock(ServerPlayerEntity player, BlockPos pos)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeBlockPos(pos);

        send(player, CLIENT_CLICKED_MODEL_BLOCK_PACKET, buf);
    }

    public static void sendPlayFilm(ServerPlayerEntity player, ServerWorld world, String filmId, boolean withCamera)
    {
        try
        {
            Film film = BBSMod.getFilms().load(filmId);

            if (film != null)
            {
                BBSMod.getActions().play(player, world, film, 0);

                BaseType data = film.toData();

                crusher.send(world.getPlayers().stream().map((p) -> (PlayerEntity) p).toList(), CLIENT_PLAY_FILM_PACKET, data, (packetByteBuf) ->
                {
                    packetByteBuf.writeString(filmId);
                    packetByteBuf.writeBoolean(withCamera);
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendPlayFilm(ServerPlayerEntity player, String filmId, boolean withCamera)
    {
        try
        {
            Film film = BBSMod.getFilms().load(filmId);

            if (film != null)
            {
                BBSMod.getActions().play(player, player.getServerWorld(), film, 0);

                crusher.send(player, CLIENT_PLAY_FILM_PACKET, film.toData(), (packetByteBuf) ->
                {
                    packetByteBuf.writeString(filmId);
                    packetByteBuf.writeBoolean(withCamera);
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendStopFilm(ServerPlayerEntity player, String filmId)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(filmId);

        send(player, CLIENT_STOP_FILM_PACKET, buf);
    }

    /**
     * Ask the editing client to re-send the whole film, used when a per-property
     * sync targets a path the server doesn't have (client/server desync).
     */
    public static void requestFilmResync(ServerPlayerEntity player, String filmId)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(filmId);

        send(player, CLIENT_REQUEST_FILM_RESYNC, buf);
    }

    public static void sendManagerData(ServerPlayerEntity player, int callbackId, RepositoryOperation op, BaseType data)
    {
        crusher.send(player, CLIENT_MANAGER_DATA_PACKET, data, (packetByteBuf) ->
        {
            packetByteBuf.writeInt(callbackId);
            packetByteBuf.writeInt(op.ordinal());
        });
    }

    public static void sendRecordedActions(ServerPlayerEntity player, String filmId, int replayId, int tick, Clips clips)
    {
        crusher.send(player, CLIENT_RECORDED_ACTIONS, clips.toData(), (packetByteBuf) ->
        {
            packetByteBuf.writeString(filmId);
            packetByteBuf.writeInt(replayId);
            packetByteBuf.writeInt(tick);
        });
    }

    public static void sendHandshake(MinecraftServer server, ServerPlayerEntity player)
    {
        send(player, ServerNetwork.CLIENT_HANDSHAKE, createHandshakeBuf(server));
    }

    private static PacketByteBuf createHandshakeBuf(MinecraftServer server)
    {
        PacketByteBuf buf = PacketByteBufs.create();
        String id = "";

        /* No need to do that in singleplayer */
        if (server.isSingleplayer())
        {
            id = "";
        }

        buf.writeString(id);

        return buf;
    }

    public static void sendCheatsPermission(ServerPlayerEntity player, boolean cheats)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeBoolean(cheats);

        send(player, ServerNetwork.CLIENT_CHEATS_PERMISSION, buf);
    }

    public static void sendSharedForm(ServerPlayerEntity player, MapType data)
    {
        crusher.send(player, CLIENT_SHARED_FORM, data, (packetByteBuf) ->
        {});
    }

    public static void sendEntityForm(ServerPlayerEntity player, IEntityFormProvider actor)
    {
        crusher.send(player, CLIENT_ENTITY_FORM, FormUtils.toData(actor.getForm()), (packetByteBuf) ->
        {
            packetByteBuf.writeInt(actor.getEntityId());
        });
    }

    public static void sendActors(ServerPlayerEntity player, String filmId, Map<String, LivingEntity> actors)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(filmId);
        buf.writeInt(actors.size());

        for (Map.Entry<String, LivingEntity> entry : actors.entrySet())
        {
            buf.writeString(entry.getKey());
            buf.writeInt(entry.getValue().getId());
        }

        send(player, CLIENT_ACTORS, buf);
    }

    public static void sendGunProperties(ServerPlayerEntity player, GunProjectileEntity projectile)
    {
        PacketByteBuf buf = PacketByteBufs.create();
        GunProperties properties = projectile.getProperties();

        buf.writeInt(projectile.getEntityId());
        properties.toNetwork(buf);

        send(player, CLIENT_GUN_PROPERTIES, buf);
    }

    public static void sendPauseFilm(ServerPlayerEntity player, String filmId)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(filmId);

        send(player, CLIENT_PAUSE_FILM, buf);
    }

    public static void sendSelectedSlot(ServerPlayerEntity player, int slot)
    {
        player.getInventory().selectedSlot = slot;

        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeInt(slot);

        send(player, CLIENT_SELECTED_SLOT, buf);
    }

    public static void sendModelBlockState(ServerPlayerEntity player, BlockPos pos, String trigger)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeBlockPos(pos);
        buf.writeString(trigger);

        send(player, CLIENT_ANIMATION_STATE_MODEL_BLOCK_TRIGGER, buf);
    }

    public static void sendReloadModelBlocks(ServerPlayerEntity player, int tickRandom)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeInt(tickRandom);

        send(player, CLIENT_REFRESH_MODEL_BLOCKS, buf);
    }
}
package gcsv;

import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public class GotoManager implements Closeable {
    private static class GotoRequest {
        public UUID from;
        public UUID to;
        public long atTime;
    }

    GotoPlayerConfig playerConfigs;
    ArrayList<GotoRequest> pendingGotos;

    GotoManager() {
        pendingGotos = new ArrayList<>();
        playerConfigs = new GotoPlayerConfig();

        GCSVMod.LOGGER.info("Reading relations from disk..");
        playerConfigs.readFromDisk();
    }

    @Override
    public void close() {
        GCSVMod.LOGGER.info("Writing relations to disk..");
        playerConfigs.writeToDisk();
    }

    private static Text serverTranslatable(String key, Object... args)
    {
        return Text.literal(Text.translatable(key, args).getString());
    }

    private void doGoto(ServerPlayerEntity from, ServerPlayerEntity to) {
        from.teleport(to.getServerWorld(), to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());
        from.sendMessage(serverTranslatable("commands.gcsv.goto.success", to.getDisplayName()));
        to.sendMessage(serverTranslatable("commands.gcsv.goto.success.3p", from.getDisplayName()));
    }

    private void doGotoRequest(ServerPlayerEntity from, ServerPlayerEntity to) {
        from.sendMessage(serverTranslatable("commands.gcsv.goto.success.reqsent", to.getDisplayName()));
        to.sendMessage(serverTranslatable("commands.gcsv.accept.request", from.getDisplayName()));
    }

    public boolean requestGoto(ServerCommandSource commandSource, ServerPlayerEntity from, ServerPlayerEntity to) {
        if (from.getUuid() == to.getUuid()) {
            commandSource.sendError(serverTranslatable("commands.gcsv.goto.self"));
            return false;
        }

        GotoMode existingRelation = playerConfigs.GetRelation(to.getUuid(), from.getUuid());
        if (existingRelation == GotoMode.NEVER) {
            commandSource.sendFeedback(() -> serverTranslatable("commands.gcsv.goto.success.denied", to.getDisplayName()), false);
            return true;
        }

        if (existingRelation == GotoMode.ALWAYS) {
            doGoto(from, to);
            return true;
        }

        final long now = Instant.now().getEpochSecond();
        for (GotoRequest req : pendingGotos) {
            if (req.from.equals(from.getUuid())) {
                if (req.to.equals(to.getUuid()) && now - req.atTime < GCSVMod.INSTANCE.properties.gotoTimeoutSecs) {
                    commandSource.sendFeedback(() -> serverTranslatable("commands.gcsv.goto.success.reqpending", to.getDisplayName()), false);
                    return true;
                }

                req.to = to.getUuid();
                req.atTime = now;
                doGotoRequest(from, to);
                return true;
            }
        }

        GotoRequest req = new GotoRequest();
        req.from = from.getUuid();
        req.to = to.getUuid();
        req.atTime = now;
        pendingGotos.add(req);

        doGotoRequest(from, to);
        return true;
    }

    public int setDefaultBehaviour(ServerPlayerEntity player, GotoMode response) {
        playerConfigs.setDefault(player.getUuid(), response);

        switch (response)
        {
            case ASK -> player.sendMessage(serverTranslatable("commands.gcsv.accept.default.ask"));
            case ALWAYS -> player.sendMessage(serverTranslatable("commands.gcsv.accept.default.always"));
            case NEVER -> player.sendMessage(serverTranslatable("commands.gcsv.accept.default.never"));
        }

        return 1;
    }

    public int doRevert(ServerPlayerEntity player, ServerPlayerEntity requester) {
        playerConfigs.SetRelation(player.getUuid(), requester.getUuid(), GotoMode.NONE);
        return 1;
    }

    public int doAccept(ServerPlayerEntity player, @Nullable ServerPlayerEntity requester, GotoMode response) {
        if (requester != null && response != GotoMode.NONE)
            playerConfigs.SetRelation(player.getUuid(), requester.getUuid(), response);

        for (int i = 0; i < pendingGotos.size(); ++i) {
            if ((requester == null || pendingGotos.get(i).from.equals(requester.getUuid())) && pendingGotos.get(i).to.equals(player.getUuid())) {
                Entity fromPlayer = requester;
                if (requester == null) {
                    if (response != GotoMode.NONE)
                        playerConfigs.SetRelation(player.getUuid(), pendingGotos.get(i).from, response);

                    for (ServerWorld world : player.getServer().getWorlds()) {
                        Entity recipient = world.getEntity(pendingGotos.get(i).from);
                        if (recipient != null) {
                            fromPlayer = recipient;
                            break;
                        }
                    }
                }

                if (fromPlayer == null) {
                    player.sendMessage(Text.translatable("commands.gcsv.goto.noplayer").formatted(Formatting.RED));
                } else {
                    assert fromPlayer instanceof ServerPlayerEntity;

                    if (response != GotoMode.NEVER)
                        doGoto((ServerPlayerEntity) fromPlayer, player);
                }

                if (requester != null) {
                    pendingGotos.remove(i);
                    return 1;
                }
            }
        }

        return 1;
    }

    void printInfo(ServerCommandSource commandSource, ServerPlayerEntity player) {
        playerConfigs.printInfo(commandSource, player.getUuid());
    }
}

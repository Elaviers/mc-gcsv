package gcsv;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GotoCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> gotoCommand = dispatcher.register(CommandManager.literal("goto")
                .requires(source -> (source.getPlayer() != null))
                .then(
                        CommandManager.argument("player", EntityArgumentType.player()).executes(
                                context -> {
                                    ServerPlayerEntity us = context.getSource().getPlayerOrThrow();
                                    ServerPlayerEntity them = EntityArgumentType.getPlayer(context, "player");
                                    return GCSVMod.INSTANCE.gotoManager.requestGoto(context.getSource(), us, them) ? 1 : 0;
                                }
                        )
                )
        );

        dispatcher.register(CommandManager.literal("g").redirect(gotoCommand));
    }
}

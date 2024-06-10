package gcsv;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class GotoAcceptCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> acceptCommand = dispatcher.register(
                CommandManager.literal("accept")
                        .requires(source -> (source.getPlayer() != null))
                        .then( CommandManager.literal("info").executes(context -> {
                            GCSVMod.INSTANCE.gotoManager.printInfo(context.getSource(), context.getSource().getPlayerOrThrow());
                            return 1;
                        }))
                        .then(
                                CommandManager.literal("default")
                                        .then(CommandManager.literal("always").executes(context -> GCSVMod.INSTANCE.gotoManager.setDefaultBehaviour(context.getSource().getPlayerOrThrow(), GotoMode.ALWAYS)))
                                        .then(CommandManager.literal("never").executes(context -> GCSVMod.INSTANCE.gotoManager.setDefaultBehaviour(context.getSource().getPlayerOrThrow(), GotoMode.NEVER)))
                                        .then(CommandManager.literal("ask").executes(context -> GCSVMod.INSTANCE.gotoManager.setDefaultBehaviour(context.getSource().getPlayerOrThrow(), GotoMode.ASK)))
                        )
                        .then(
                                CommandManager.literal("all").executes(context -> GCSVMod.INSTANCE.gotoManager.doAccept(context.getSource().getPlayerOrThrow(), null, GotoMode.NONE))
                                        .then(CommandManager.literal("always").executes(context -> GCSVMod.INSTANCE.gotoManager.doAccept(context.getSource().getPlayerOrThrow(), null, GotoMode.ALWAYS)))
                                        .then(CommandManager.literal("never").executes(context -> GCSVMod.INSTANCE.gotoManager.doAccept(context.getSource().getPlayerOrThrow(), null, GotoMode.NEVER)))
                        )
                        .then(
                                CommandManager.argument("player", EntityArgumentType.player()).executes(
                                                context -> GCSVMod.INSTANCE.gotoManager.doAccept(context.getSource().getPlayerOrThrow(), EntityArgumentType.getPlayer(context, "player"), GotoMode.NONE)
                                        )
                                        .then(CommandManager.literal("always").executes(
                                                context -> GCSVMod.INSTANCE.gotoManager.doAccept(context.getSource().getPlayerOrThrow(), EntityArgumentType.getPlayer(context, "player"), GotoMode.ALWAYS)
                                        ))
                                        .then(CommandManager.literal("never").executes(
                                                context -> GCSVMod.INSTANCE.gotoManager.doAccept(context.getSource().getPlayerOrThrow(), EntityArgumentType.getPlayer(context, "player"), GotoMode.NEVER)
                                        ))
                                        .then(CommandManager.literal("ask").executes(
                                                context -> GCSVMod.INSTANCE.gotoManager.doAccept(context.getSource().getPlayerOrThrow(), EntityArgumentType.getPlayer(context, "player"), GotoMode.ASK)
                                        ))
                                        .then(CommandManager.literal("revert").executes(
                                                context -> GCSVMod.INSTANCE.gotoManager.doRevert(context.getSource().getPlayerOrThrow(), EntityArgumentType.getPlayer(context, "player") )
                                        ))
                        )
        );

        dispatcher.register(CommandManager.literal("a").redirect(acceptCommand));
    }

}

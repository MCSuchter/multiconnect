package net.earthcomputer.multiconnect.protocols.v1_12_2.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.server.command.CommandSource;

import java.util.Set;

public class Commands_1_12_2 {

    public static void register(CommandDispatcher<CommandSource> dispatcher, Set<String> serverCommands) {
        if (serverCommands != null) {
            for (String command : serverCommands) {
                if (dispatcher.getRoot().getChild(command) == null) {
                    dispatcher.register(literal(command)
                            .executes(ctx -> 0)
                            .then(argument("args", StringArgumentType.greedyString())
                                .suggests(SuggestionProviders.ASK_SERVER)
                                .executes(ctx -> 0)));
                }
            }
        }
    }

    public static LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<CommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

}

package com.rickandmorty.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.PortalGunItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;

/**
 * {@code /rickrooms gun [players]}: a fully upgraded Portal Gun (Targeting, Blender, DNA Lock,
 * a full tank) with every level already on the dial. Needs operator rights, like goto.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class GunCommand {
    private GunCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(RickAndMortyMod.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("gun")
                        .executes(context -> give(context, List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> give(context, EntityArgument.getPlayers(context, "players"))))));
    }

    private static int give(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            ItemStack gun = PortalGunItem.fullyUpgraded(player.server);
            if (!player.getInventory().add(gun)) player.drop(gun, false);
        }
        int count = players.size();
        context.getSource().sendSuccess(() -> Component.translatable("commands.rickrooms.gun.done", count), true);
        return count;
    }
}

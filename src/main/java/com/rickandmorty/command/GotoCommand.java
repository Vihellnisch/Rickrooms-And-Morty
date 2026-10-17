package com.rickandmorty.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.PortalTeleporter;
import com.rickandmorty.worldgen.ModDimensions;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelTravel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * For testing: {@code /rickrooms goto <place> [players]} sends you (or them) to a random spot in
 * any Backrooms level, Cronenberg World, or home to the Overworld spawn. Needs operator rights.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class GotoCommand {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            name -> Component.translatable("commands.rickrooms.goto.unknown", name));

    private GotoCommand() {
    }

    /** Every place the command knows: the levels by name, plus cronenberg and overworld. */
    public static List<String> places() {
        List<String> names = new ArrayList<>();
        for (BackroomsLevel level : BackroomsLevel.values()) names.add(level.getSerializedName());
        names.add("cronenberg");
        names.add("overworld");
        return names;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(RickAndMortyMod.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("goto")
                        .then(Commands.argument("place", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(places(), builder))
                                .executes(context -> send(context, List.of(context.getSource().getPlayerOrException())))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> send(context, EntityArgument.getPlayers(context, "players")))))));
    }

    private static int send(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) throws CommandSyntaxException {
        String place = StringArgumentType.getString(context, "place");
        int sent = 0;
        for (ServerPlayer player : players) {
            if (go(player, place)) sent++;
            else throw UNKNOWN.create(place);
        }
        int count = sent;
        context.getSource().sendSuccess(() -> Component.translatable("commands.rickrooms.goto.done", count, place), true);
        return sent;
    }

    /** Sends a player to {@code place}. False if there's no such place (or it doesn't exist on this server). */
    public static boolean go(ServerPlayer player, String place) {
        if (place.equals("overworld")) {
            ServerLevel overworld = player.server.overworld();
            var spawn = overworld.getSharedSpawnPos();
            int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
            PortalTeleporter.teleport(player, player.serverLevel(), overworld, new Vec3(spawn.getX() + 0.5, y, spawn.getZ() + 0.5), player.getYRot());
            return true;
        }
        if (place.equals("cronenberg")) {
            ServerLevel target = player.server.getLevel(ModDimensions.CRONENBERG);
            var destination = ModDimensions.randomDestination(player.server, ModDimensions.CRONENBERG, player.getRandom());
            if (target == null || destination.isEmpty()) return false;
            PortalTeleporter.teleport(player, player.serverLevel(), target, destination.get().position(), destination.get().yaw());
            return true;
        }
        for (BackroomsLevel level : BackroomsLevel.values()) {
            if (level.getSerializedName().equals(place) || level.id().equals(place)) return LevelTravel.sendTo(player, level);
        }
        return false;
    }
}

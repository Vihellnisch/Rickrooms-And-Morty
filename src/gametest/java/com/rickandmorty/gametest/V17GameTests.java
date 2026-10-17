package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.StillLifeEntity;
import com.rickandmorty.entity.StillLifeForm;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests for the Still Lifes of people. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V17GameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void everyFormHasItsShape(GameTestHelper helper) {
        for (StillLifeForm form : StillLifeForm.values()) {
            StillLifeEntity e = helper.spawn(ModEntities.STILL_LIVES.get(form).get(), new BlockPos(1 + form.ordinal(), 2, 1));
            helper.assertTrue(e.form() == form && !e.isAwake(), form + " came out wrong");
        }
        helper.assertTrue(StillLifeForm.CAPTAIN.height > 2.8f && StillLifeForm.FOLDED.height < 1.5f, "the Captain should tower, the Folded one crouch");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void itWakesAndWearsYourFace(GameTestHelper helper) {
        StillLifeEntity e = helper.spawn(ModEntities.STILL_LIVES.get(StillLifeForm.THREE_FACES).get(), new BlockPos(2, 2, 2));
        ServerPlayer player = survivalPlayer(helper, "test-original");
        Vec3 at = helper.absoluteVec(new Vec3(5.5, 2, 5.5));
        player.teleportTo(helper.getLevel(), at.x, at.y, at.z, 0.0f, 0.0f);
        helper.succeedWhen(() -> {
            helper.assertTrue(e.isAwake(), "it didn't wake with someone right next to it");
            helper.assertTrue(e.profile() != null, "it isn't wearing anyone's face yet");
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    @GameTest(template = EMPTY)
    public static void theCaptainMovesInFits(GameTestHelper helper) {
        StillLifeEntity captain = helper.spawn(ModEntities.STILL_LIVES.get(StillLifeForm.CAPTAIN).get(), new BlockPos(2, 2, 2));
        StillLifeEntity other = helper.spawn(ModEntities.STILL_LIVES.get(StillLifeForm.TWICE).get(), new BlockPos(5, 2, 2));
        int frozen = 0;
        for (int t = 0; t < 28; t++) {
            captain.tickCount = t;
            other.tickCount = t;
            if (captain.frozenFrame()) frozen++;
            helper.assertFalse(other.frozenFrame(), "only the Captain jerks about");
        }
        helper.assertTrue(frozen > 4 && frozen < 20, "frozen " + frozen + " frames of 28");
        helper.succeed();
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper, String name) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }
}

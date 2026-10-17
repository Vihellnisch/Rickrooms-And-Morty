package com.rickandmorty.gametest;

import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.GatewayStructure;
import com.rickandmorty.block.WarpGatewayBlock;
import com.rickandmorty.block.entity.EvilGateway;
import com.rickandmorty.block.entity.WarpGatewayBlockEntity;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Evil Warp Gateways: armed by a gun in attack mode; they eat mobs (and spit out bones) and melt down on players. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class EvilGatewayGameTests {
    private static final String EMPTY = "empty";

    private static BlockPos armedGateway(GameTestHelper helper, Player owner) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        var state = ModBlocks.WARP_GATEWAY.get().defaultBlockState().setValue(WarpGatewayBlock.FACING, Direction.SOUTH);
        level.setBlock(pos, state, 3);
        state.getBlock().setPlacedBy(level, pos, state, null, new ItemStack(ModItems.WARP_GATEWAY.get()));
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.BLENDER.get(), true);
        gun.set(ModDataComponents.ATTACK_MODE.get(), true);
        owner.setItemInHand(InteractionHand.MAIN_HAND, gun);
        BlockPos top = GatewayStructure.partPos(pos, Direction.SOUTH, GatewayStructure.TOP_PART);   // any piece of it works
        gun.useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(top), Direction.SOUTH, top, false)));
        return pos;
    }

    private static WarpGatewayBlockEntity gateway(GameTestHelper helper, BlockPos pos) {
        return (WarpGatewayBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void evilGatewayEatsMobsAndSpitsBones(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setGameMode(GameType.SURVIVAL);
        BlockPos pos = armedGateway(helper, owner);
        helper.assertTrue(gateway(helper, pos).isEvil(), "the gun didn't arm the gateway");
        helper.assertFalse(level.getBlockState(pos).getValue(WarpGatewayBlock.RED), "armed gateway already shows red");
        Vec3 center = EvilGateway.center(pos, Direction.SOUTH);
        owner.moveTo(center.x - 2.5, pos.getY(), center.z + 1.5, 0, 0);    // right beside it: the owner is safe
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 6));
        zombie.setNoAi(true);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        zombie.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 2.0f);
        boolean[] sawRed = {false};
        helper.onEachTick(() -> sawRed[0] |= level.getBlockState(pos).getValue(WarpGatewayBlock.RED));
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.isRemoved(), "zombie not eaten, phase " + gateway(helper, pos).evilPhase());
            helper.assertTrue(sawRed[0], "never turned red");
            helper.assertTrue(gateway(helper, pos).evilPhase() == EvilGateway.IDLE, "not back to waiting");
            helper.assertFalse(level.getBlockState(pos).getValue(WarpGatewayBlock.RED), "still red");
            var items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8));
            helper.assertTrue(items.stream().anyMatch(i -> i.getItem().is(Items.BONE)), "no bones spat out");
            helper.assertTrue(items.stream().anyMatch(i -> i.getItem().is(Items.IRON_SWORD)), "the zombie's loot wasn't spat out: " + items);
            helper.assertTrue(owner.isAlive() && owner.getHealth() == owner.getMaxHealth(), "it ate its owner");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void evilGatewayMeltsDownOnPlayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        double power = ModConfig.GATEWAY_EXPLOSION.get();
        ModConfig.GATEWAY_EXPLOSION.set(0.0);                        // don't blow up the other tests
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = armedGateway(helper, owner);
        Vec3 center = EvilGateway.center(pos, Direction.SOUTH);
        ServerPlayer victim = survivalPlayer(helper);
        victim.moveTo(center.x, pos.getY(), center.z + 1.8, 180, 0);
        boolean[] meltdown = {false};
        helper.onEachTick(() -> {
            if (level.getBlockEntity(pos) instanceof WarpGatewayBlockEntity g && g.evilPhase() == EvilGateway.MELTDOWN) meltdown[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(victim.isDeadOrDying(), "the player wasn't killed: health " + victim.getHealth() + " at " + victim.position()
                    + ", phase " + (level.getBlockEntity(pos) instanceof WarpGatewayBlockEntity g ? g.evilPhase() + " victim " + g.evilVictim() : "gone")
                    + ", invulnerable " + victim.isInvulnerable() + " creative " + victim.isCreative());
            helper.assertTrue(meltdown[0], "no meltdown");
            helper.assertTrue(level.getBlockState(pos).isAir(), "the gateway didn't destroy itself");
            helper.assertTrue(level.getBlockState(GatewayStructure.partPos(pos, Direction.SOUTH, GatewayStructure.TOP_PART)).isAir(), "frame left behind");
            ModConfig.GATEWAY_EXPLOSION.set(power);
        });
    }

    /** Like GameTestHelper's mock server player, but one that doesn't pretend to be in creative mode. */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-victim"), false);
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

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void councilMembersCountAsPlayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        double power = ModConfig.GATEWAY_EXPLOSION.get();
        ModConfig.GATEWAY_EXPLOSION.set(0.0);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = armedGateway(helper, owner);
        var member = helper.spawn(com.rickandmorty.registry.ModEntities.COUNCIL_MEMBER.get(), new BlockPos(4, 1, 6));
        member.setNoAi(true);
        boolean[] meltdown = {false};
        helper.onEachTick(() -> {
            if (level.getBlockEntity(pos) instanceof WarpGatewayBlockEntity g && g.evilPhase() == EvilGateway.MELTDOWN) meltdown[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(meltdown[0], "a Council member didn't set off a meltdown");
            helper.assertTrue(level.getBlockState(pos).isAir(), "the gateway didn't destroy itself");
            ModConfig.GATEWAY_EXPLOSION.set(power);
        });
    }

    @GameTest(template = EMPTY)
    public static void attackModeGunDisarmsItAgain(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = armedGateway(helper, owner);
        helper.assertTrue(gateway(helper, pos).isEvil(), "not armed");
        BlockPos top = GatewayStructure.partPos(pos, Direction.SOUTH, GatewayStructure.TOP_PART);
        owner.getMainHandItem().useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(top), Direction.SOUTH, top, false)));
        helper.assertFalse(gateway(helper, pos).isEvil(), "not disarmed");
        helper.succeed();
    }
}

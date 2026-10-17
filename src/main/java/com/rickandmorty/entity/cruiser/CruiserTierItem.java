package com.rickandmorty.entity.cruiser;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** One mark of a {@link CruiserTier} upgrade (a Microverse Booster, Speed Thrusters, a Laser Drill). Use it on the cruiser. */
public class CruiserTierItem extends Item {
    private final CruiserTier line;
    private final int tier;

    public CruiserTierItem(CruiserTier line, int tier, Properties properties) {
        super(properties);
        this.line = line;
        this.tier = tier;
    }

    public CruiserTier line() {
        return line;
    }

    public int tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (Component line : describe(this.line, tier)) tooltip.add(line.copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rickrooms.cruiser_upgrade").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** What a mark does, for tooltips. */
    public static List<Component> describe(CruiserTier line, int tier) {
        return switch (line) {
            case BOOSTER -> List.of(
                    Component.translatable("tooltip.rickrooms.cruiser_booster.battery", CruiserTier.capacity(tier) / 60),
                    Component.translatable("tooltip.rickrooms.cruiser_booster.flight", Math.round((1 - CruiserTier.flightDrain(tier)) * 100)),
                    Component.translatable("tooltip.rickrooms.cruiser_booster.weapons", Math.round((1 - CruiserTier.weaponCost(tier)) * 100)),
                    Component.translatable("tooltip.rickrooms.cruiser_booster.recharge", CruiserTier.recharge(tier)));
            case THRUSTERS -> List.of(
                    Component.translatable("tooltip.rickrooms.cruiser_thrusters.speed", Math.round(CruiserTier.topSpeed(tier) / CruiserTier.topSpeed(0) * 100)),
                    Component.translatable("tooltip.rickrooms.cruiser_thrusters.handling"));
            case DRILL -> tier >= 3
                    ? List.of(Component.translatable("tooltip.rickrooms.laser_drill.size", CruiserTier.drillSize(tier)),
                    Component.translatable("tooltip.rickrooms.laser_drill.obsidian"))
                    : List.of(Component.translatable("tooltip.rickrooms.laser_drill.size", CruiserTier.drillSize(tier)));
        };
    }
}

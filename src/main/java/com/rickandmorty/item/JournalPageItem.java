package com.rickandmorty.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * One of three torn pages of research hidden in loot chests. Each set hints at somewhere the
 * portal gun can go: the journal pages at the Backrooms, Rick's notes at Cronenberg World.
 * Together (plus Portal Fluid) they decipher its coordinates.
 */
public class JournalPageItem extends Item {
    public static final int PAGE_COUNT = 3;
    private static final int LORE_LINES = 3;
    private final String name;
    private final int page;

    /** {@code name} is the item family, e.g. "journal_page" or "rick_notes". */
    public JournalPageItem(Properties properties, String name, int page) {
        super(properties);
        this.name = name;
        this.page = page;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int line = 1; line <= LORE_LINES; line++) {
            tooltip.add(Component.translatable("tooltip.rickrooms." + name + "_" + page + "." + line)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        tooltip.add(Component.translatable("tooltip.rickrooms.journal_page.number", page, PAGE_COUNT).withStyle(ChatFormatting.DARK_GRAY));
    }
}

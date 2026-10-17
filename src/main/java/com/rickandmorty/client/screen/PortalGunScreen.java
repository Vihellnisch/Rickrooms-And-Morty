package com.rickandmorty.client.screen;

import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.network.DialActionPayload;
import com.rickandmorty.network.GunModePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The portal gun's dial: pick where the next portal goes, save your current
 * position under a name, or delete old locations. The list of places scrolls when it's
 * longer than the screen (every Backrooms level you find adds a row); the name box and the
 * buttons below it always stay on screen.
 */
public class PortalGunScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int ROW_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 36;
    /** The name box and save button, then the mode/done row. */
    private static final int FOOTER_HEIGHT = 8 + 20 + 6 + 20;
    private static final int MARGIN = 6;
    private static final int PORTAL_GREEN = 0x5CFF3B;

    private final InteractionHand hand;
    private PortalDestinations shown = PortalDestinations.EMPTY;
    private EditBox nameBox;
    private String typedName = "";
    private int top;
    private boolean shownAttack;
    /** The first row of the list that's shown, how many fit, and how many there are. */
    private int scroll = -1;
    private int visibleRows;
    private int rowCount;
    private int listTop;

    public PortalGunScreen(InteractionHand hand) {
        super(Component.translatable("screen.rickrooms.portal_gun"));
        this.hand = hand;
    }

    private ItemStack gun() {
        return minecraft.player.getItemInHand(hand);
    }

    @Override
    protected void init() {
        shown = PortalGunItem.getDestinations(gun());
        int left = (width - PANEL_WIDTH) / 2;
        List<ResourceKey<Level>> dimensions = shown.knownDimensions();
        List<PortalDestinations.Destination> destinations = shown.destinations();
        // rows: the learned dimensions, then the saved places (or one line saying there are none)
        rowCount = dimensions.size() + Math.max(1, destinations.size());
        int fits = Math.max(2, (height - 2 * MARGIN - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
        visibleRows = Math.min(rowCount, fits);
        int totalHeight = HEADER_HEIGHT + visibleRows * ROW_HEIGHT + FOOTER_HEIGHT;
        top = Math.max(MARGIN, (height - totalHeight) / 2);
        listTop = top + HEADER_HEIGHT;
        if (scroll < 0) scroll = Math.max(0, selectedRow() - visibleRows + 1);   // first open: show what's selected
        scroll = Math.max(0, Math.min(scroll, rowCount - visibleRows));

        // Learned dimensions first. They can't be deleted and land somewhere random each trip.
        for (int i = 0; i < dimensions.size(); i++) {
            int y = rowY(i);
            if (y < 0) continue;
            int index = i;
            ResourceKey<Level> dimension = dimensions.get(i);
            boolean selected = shown.selectedDimension().filter(dimension::equals).isPresent();
            MutableComponent name = PortalDestinations.dimensionName(dimension).copy();
            Component label = selected
                    ? Component.literal("» ").append(name).append(" «").withStyle(ChatFormatting.GOLD)
                    : name.withStyle(ChatFormatting.YELLOW);
            addRenderableWidget(Button.builder(label, b -> send(DialActionPayload.selectDimension(hand, index)))
                    .bounds(left, y, PANEL_WIDTH, 18)
                    .tooltip(Tooltip.create(Component.translatable("screen.rickrooms.portal_gun.random_landing")))
                    .build());
        }

        for (int i = 0; i < destinations.size(); i++) {
            int y = rowY(dimensions.size() + i);
            if (y < 0) continue;
            int index = i;
            PortalDestinations.Destination dest = destinations.get(i);
            Component label = shown.isSavedSelected(i)
                    ? Component.literal("» " + dest.name() + " «").withStyle(ChatFormatting.GREEN)
                    : Component.literal(dest.name());
            addRenderableWidget(Button.builder(label, b -> send(DialActionPayload.select(hand, index)))
                    .bounds(left, y, PANEL_WIDTH - 24, 18)
                    .tooltip(Tooltip.create(dest.describe()))
                    .build());
            addRenderableWidget(Button.builder(Component.literal("X").withStyle(ChatFormatting.RED),
                            b -> send(DialActionPayload.remove(hand, index)))
                    .bounds(left + PANEL_WIDTH - 20, y, 20, 18)
                    .tooltip(Tooltip.create(Component.translatable("screen.rickrooms.portal_gun.delete", dest.name())))
                    .build());
        }

        int bottom = listTop + visibleRows * ROW_HEIGHT + 8;
        nameBox = new EditBox(font, left, bottom, PANEL_WIDTH - 84, 20, Component.translatable("screen.rickrooms.portal_gun.name"));
        nameBox.setMaxLength(PortalDestinations.MAX_NAME_LENGTH);
        nameBox.setValue(typedName);
        nameBox.setResponder(text -> typedName = text);
        nameBox.setHint(Component.translatable("screen.rickrooms.portal_gun.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(nameBox);

        Button save = addRenderableWidget(Button.builder(Component.translatable("screen.rickrooms.portal_gun.save"), b -> saveCurrentPosition())
                .bounds(left + PANEL_WIDTH - 80, bottom, 80, 20)
                .tooltip(Tooltip.create(Component.translatable(shown.isFull()
                        ? "screen.rickrooms.portal_gun.full"
                        : "screen.rickrooms.portal_gun.save_tooltip")))
                .build());
        save.active = !shown.isFull();

        if (PortalGunItem.hasBlender(gun())) {
            // with the Blender Upgrade: a switch between travel and attack mode next to Done
            boolean attack = PortalGunItem.isAttackMode(gun());
            addRenderableWidget(Button.builder(Component.translatable(attack ? "screen.rickrooms.portal_gun.mode_attack" : "screen.rickrooms.portal_gun.mode_travel")
                            .withStyle(attack ? ChatFormatting.RED : ChatFormatting.GREEN), b -> PacketDistributor.sendToServer(new GunModePayload(hand)))
                    .bounds(left, bottom + 26, PANEL_WIDTH / 2 - 4, 20)
                    .tooltip(Tooltip.create(Component.translatable("screen.rickrooms.portal_gun.mode_tooltip")))
                    .build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(left + PANEL_WIDTH / 2 + 4, bottom + 26, PANEL_WIDTH / 2 - 4, 20)
                    .build());
        } else {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(left + PANEL_WIDTH / 2 - 50, bottom + 26, 100, 20)
                    .build());
        }
        shownAttack = PortalGunItem.isAttackMode(gun());
    }

    /** Screen y of a list row, or -1 if it's scrolled out of view. */
    private int rowY(int row) {
        int shownRow = row - scroll;
        return shownRow >= 0 && shownRow < visibleRows ? listTop + shownRow * ROW_HEIGHT : -1;
    }

    /** The row of whatever the gun is set to, so the list opens on it. */
    private int selectedRow() {
        List<ResourceKey<Level>> dimensions = shown.knownDimensions();
        for (int i = 0; i < dimensions.size(); i++) {
            if (shown.selectedDimension().filter(dimensions.get(i)::equals).isPresent()) return i;
        }
        for (int i = 0; i < shown.destinations().size(); i++) {
            if (shown.isSavedSelected(i)) return dimensions.size() + i;
        }
        return 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (rowCount > visibleRows && scrollY != 0) {
            int next = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), rowCount - visibleRows));
            if (next != scroll) {
                scroll = next;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void saveCurrentPosition() {
        if (shown.isFull()) return;
        send(DialActionPayload.add(hand, typedName));
        typedName = "";
        nameBox.setValue("");
    }

    private static void send(DialActionPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public void tick() {
        super.tick();
        ItemStack gun = gun();
        if (!(gun.getItem() instanceof PortalGunItem)) {
            onClose();
            return;
        }
        // The server changed the gun (we selected/added/removed something): refresh the list.
        if (!PortalGunItem.getDestinations(gun).equals(shown) || PortalGunItem.isAttackMode(gun) != shownAttack) {
            rebuildWidgets();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            saveCurrentPosition();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = width / 2;
        graphics.drawCenteredString(font, title, centerX, top, PORTAL_GREEN);
        graphics.drawCenteredString(font,
                Component.translatable("screen.rickrooms.portal_gun.charges", PortalGunItem.getCharges(gun()), PortalGunItem.MAX_CHARGES),
                centerX, top + 12, 0xAAAAAA);
        Component selected = shown.selectedDimension()
                .<Component>map(dim -> Component.translatable("screen.rickrooms.portal_gun.selected",
                        PortalDestinations.dimensionName(dim), Component.translatable("screen.rickrooms.portal_gun.random_spot")))
                .or(() -> shown.selectedDestination()
                        .map(dest -> Component.translatable("screen.rickrooms.portal_gun.selected", dest.name(), dest.describe())))
                .orElse(Component.translatable("screen.rickrooms.portal_gun.none_selected"));
        graphics.drawCenteredString(font, selected, centerX, top + 23, 0xFFFFFF);

        if (shown.destinations().isEmpty()) {
            int y = rowY(shown.knownDimensions().size());
            if (y >= 0) graphics.drawCenteredString(font, Component.translatable("screen.rickrooms.portal_gun.empty"), centerX, y + 5, 0x888888);
        }
        // more above / below: little arrows beside the list, and the wheel scrolls it
        int arrowX = (width + PANEL_WIDTH) / 2 + 6;
        if (scroll > 0) graphics.drawString(font, "\u25B2", arrowX, listTop + 5, 0xFFFFFF);
        if (scroll + visibleRows < rowCount) graphics.drawString(font, "\u25BC", arrowX, listTop + (visibleRows - 1) * ROW_HEIGHT + 5, 0xFFFFFF);
    }

    /** Don't pause singleplayer, otherwise the server can't apply dial changes while the screen is open. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

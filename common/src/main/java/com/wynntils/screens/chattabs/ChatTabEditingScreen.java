/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.chattabs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.screens.base.WynntilsGridLayoutScreen;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.base.widgets.TextWidget;
import com.wynntils.screens.base.widgets.WynntilsCheckbox;
import com.wynntils.screens.chattabs.widgets.ChatTabsWidget;
import com.wynntils.services.chat.ChatTab;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ChatTabEditingScreen extends WynntilsGridLayoutScreen {
    private static final int HEADER_ROW_Y = 6;
    private static final int FIRST_ROW_Y = 11;
    private static final int SECOND_ROW_Y = 22;
    private static final int THIRD_ROW_Y = 40;
    private static final int FOURTH_ROW_Y = 47;
    private static final int FIFTH_ROW_Y = 54;

    private List<AbstractWidget> chatTabsWidgets = new ArrayList<>();

    private TextInputBoxWidget nameInput;
    private TextInputBoxWidget autoCommandInput;
    private TextInputBoxWidget orderInput;
    private final List<WynntilsCheckbox> recipientTypeBoxes = new ArrayList<>();
    private TextInputBoxWidget filterRegexInput;
    private TextWidget regexErrorMsg;
    private Checkbox consumingCheckbox;

    private Button saveButton;
    private Button saveAndCloseButton;

    private boolean firstSetup;
    private final ChatTab edited;

    private ChatTabEditingScreen() {
        this(null);
    }

    private ChatTabEditingScreen(ChatTab tab) {
        super(Component.literal("Chat Tab Editing Screen"));

        this.edited = tab;
        this.firstSetup = true;
    }

    public static Screen create() {
        return new ChatTabEditingScreen();
    }

    public static Screen create(ChatTab chatTab) {
        return new ChatTabEditingScreen(chatTab);
    }

    @Override
    protected void doInit() {
        super.doInit();
        reloadChatTabsWidgets();

        // region Name
        nameInput = new TextInputBoxWidget(
                (int) (dividedWidth * 35),
                (int) ((dividedHeight * FIRST_ROW_Y)),
                (int) (dividedWidth * 10),
                BUTTON_WIDTH,
                (s) -> updateSaveButtonActive(),
                this,
                nameInput);
        this.addRenderableWidget(nameInput);

        if (firstSetup) {
            if (edited != null) {
                nameInput.setTextBoxInput(edited.getName());
            }
            setFocusedTextInput(nameInput);
        }
        // endregion

        // region Auto Command
        autoCommandInput = new TextInputBoxWidget(
                (int) (dividedWidth * 47),
                (int) (dividedHeight * FIRST_ROW_Y),
                (int) (dividedWidth * 10),
                BUTTON_WIDTH,
                null,
                this,
                autoCommandInput);
        this.addRenderableWidget(autoCommandInput);
        if (firstSetup && edited != null && edited.getAutoCommand() != null) {
            autoCommandInput.setTextBoxInput(edited.getAutoCommand());
        }
        // endregion

        // region Order
        orderInput = new TextInputBoxWidget(
                (int) (dividedWidth * 59),
                (int) (dividedHeight * FIRST_ROW_Y),
                (int) (dividedWidth * 2),
                BUTTON_WIDTH,
                (s) -> updateSaveButtonActive(),
                this,
                orderInput);
        this.addRenderableWidget(orderInput);
        if (firstSetup && edited != null) {
            orderInput.setTextBoxInput(Integer.toString(Services.ChatTab.getTabIndex(edited)));
        }
        // endregion

        // region Recipient Types

        // Display all recipient types in two rows of 4 checkboxes each
        List<Checkbox> oldBoxes = new ArrayList<>(recipientTypeBoxes);
        recipientTypeBoxes.clear();

        int x = (int) (dividedWidth * 35);
        int y = (int) (dividedHeight * SECOND_ROW_Y);
        for (int i = 0; i < RecipientType.values().length; i++) {
            if (i == 4 || i == 8) {
                y += (int) (dividedHeight * 5);
                x = (int) (dividedWidth * 35);
            }

            RecipientType type = RecipientType.values()[i];
            Checkbox oldCheckbox = oldBoxes.stream()
                    .filter(checkbox -> checkbox.getMessage().getString().equals(type.getName()))
                    .findFirst()
                    .orElse(null);

            boolean oldCheckboxSelected = oldCheckbox != null && oldCheckbox.selected();
            boolean editedFirstSetupSelected = firstSetup
                    && edited != null
                    && (edited.getFilteredTypes() == null
                            || edited.getFilteredTypes().contains(type));
            boolean ticked = oldCheckboxSelected || editedFirstSetupSelected;

            WynntilsCheckbox newBox = new WynntilsCheckbox(
                    x, y, BUTTON_WIDTH, Component.literal(type.getName()), ticked, (int) (dividedWidth * 7) - 24);

            this.addRenderableWidget(newBox);
            recipientTypeBoxes.add(newBox);

            x += (int) (dividedWidth * 7);
        }

        // endregion

        // region Filter Regex
        filterRegexInput = new TextInputBoxWidget(
                (int) (dividedWidth * 35),
                (int) (dividedHeight * THIRD_ROW_Y),
                (int) (dividedWidth * 26),
                BUTTON_WIDTH,
                (s) -> updateSaveButtonActive(),
                this,
                filterRegexInput);
        this.addRenderableWidget(filterRegexInput);
        if (firstSetup && edited != null && edited.getCustomRegexString() != null) {
            filterRegexInput.setTextBoxInput(edited.getCustomRegexString());
        }

        regexErrorMsg = new TextWidget(
                this.width / 2 - 160 + 100, this.height / 2 + 75 + 7, 200, BUTTON_WIDTH, Component.empty());
        this.addRenderableWidget(regexErrorMsg);
        // endregion

        // region Consuming
        consumingCheckbox = Checkbox.builder(
                        Component.translatable("screens.wynntils.chatTabsGui.consuming"),
                        FontRenderer.getInstance().getFont())
                .pos((int) (dividedWidth * 35), (int) (dividedHeight * FOURTH_ROW_Y))
                .maxWidth(BUTTON_WIDTH)
                .selected(consumingCheckbox != null && consumingCheckbox.selected())
                .build();
        this.addRenderableWidget(consumingCheckbox);
        if (firstSetup && edited != null) {
            consumingCheckbox.selected = edited.isConsuming();
        }
        // endregion

        // region Screen Interactions
        String saveButtonKey =
                edited == null ? "screens.wynntils.chatTabsGui.add" : "screens.wynntils.chatTabsGui.save";
        saveButton = new Button.Builder(
                        Component.translatable(saveButtonKey).withStyle(ChatFormatting.GREEN), (button) -> {
                            saveChatTab();
                            reloadChatTabsWidgets();
                        })
                .pos((int) (dividedWidth * 35), (int) (dividedHeight * FIFTH_ROW_Y))
                .size((int) (dividedWidth * 8), BUTTON_WIDTH)
                .build();
        this.addRenderableWidget(saveButton);

        String saveAndCloseButtonKey = edited == null
                ? "screens.wynntils.chatTabsGui.addAndClose"
                : "screens.wynntils.chatTabsGui.saveAndClose";
        saveAndCloseButton = new Button.Builder(
                        Component.translatable(saveAndCloseButtonKey).withStyle(ChatFormatting.GREEN), (button) -> {
                            saveChatTab();
                            this.onClose();
                        })
                .pos((int) (dividedWidth * 44), (int) (dividedHeight * FIFTH_ROW_Y))
                .size((int) (dividedWidth * 8), BUTTON_WIDTH)
                .build();
        this.addRenderableWidget(saveAndCloseButton);

        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.chatTabsGui.cancel"), (button) -> this.onClose())
                .pos((int) (dividedWidth * 53), (int) (dividedHeight * FIFTH_ROW_Y))
                .size((int) (dividedWidth * 8), BUTTON_WIDTH)
                .build());
        // endregion

        firstSetup = false;
        updateSaveButtonActive();
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.doRender(guiGraphics, mouseX, mouseY, partialTick);
        PoseStack poseStack = guiGraphics.pose();

        // Chat Tabs List
        chatTabsWidgets.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));

        if (edited == null) {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(I18n.get("screens.wynntils.chatTabsGui.create")),
                            dividedWidth * 48,
                            dividedHeight * HEADER_ROW_Y,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.BOTTOM,
                            TextShadow.NORMAL);
        } else {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(I18n.get("screens.wynntils.chatTabsGui.edit", edited.getName())),
                            dividedWidth * 48,
                            dividedHeight * HEADER_ROW_Y,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.BOTTOM,
                            TextShadow.NORMAL);
        }

        // Name
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(
                                I18n.get("screens.wynntils.chatTabsGui.name") + ChatFormatting.DARK_RED + " *"),
                        (int) (dividedWidth * 35),
                        (int) (dividedHeight * FIRST_ROW_Y),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.BOTTOM,
                        TextShadow.NORMAL);

        // Auto Command
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.chatTabsGui.autoCommand")),
                        (int) (dividedWidth * 47),
                        (int) (dividedHeight * FIRST_ROW_Y),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.BOTTOM,
                        TextShadow.NORMAL);

        // Order
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.chatTabsGui.order")),
                        (int) (dividedWidth * 59),
                        (int) (dividedHeight * FIRST_ROW_Y),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.BOTTOM,
                        TextShadow.NORMAL);

        // Recipient Types
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(
                                I18n.get("screens.wynntils.chatTabsGui.types") + ChatFormatting.DARK_RED + " *"),
                        (int) (dividedWidth * 35),
                        (int) (dividedHeight * (SECOND_ROW_Y - 1)),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.BOTTOM,
                        TextShadow.NORMAL);

        // Filter Pattern
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.chatTabsGui.filter")),
                        (int) (dividedWidth * 35),
                        (int) (dividedHeight * THIRD_ROW_Y),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.BOTTOM,
                        TextShadow.NORMAL);
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        // Order of these matter!
        // First, check if we need to load a new widget. If yes, it could have a different save button
        // state. Then, we need to determine if a checkbox was clicked.
        // If yes, update the save/add button states and move on.
        // If not, deal with clicks on the save/add buttons, then super method if needed.
        for (AbstractWidget widget : chatTabsWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                return widget.mouseClicked(mouseX, mouseY, button);
            }
        }

        boolean mouseClicked = super.doMouseClicked(mouseX, mouseY, button);
        updateSaveButtonActive();

        if (saveButton.isMouseOver(mouseX, mouseY)) {
            return saveButton.mouseClicked(mouseX, mouseY, button);
        }
        if (saveAndCloseButton.isMouseOver(mouseX, mouseY)) {
            return saveAndCloseButton.mouseClicked(mouseX, mouseY, button);
        }

        return mouseClicked;
    }

    @Override
    public void onClose() {
        McUtils.mc().setScreen(new ChatScreen(""));
    }

    private void saveChatTab() {
        if (edited != null) {
            Services.ChatTab.removeTab(edited);
        }

        int insertIndex = orderInput.getTextBoxInput().isEmpty()
                ? Services.ChatTab.getTabCount()
                : Math.min(Services.ChatTab.getTabCount(), Integer.parseInt(orderInput.getTextBoxInput()));

        ChatTab chatTab = new ChatTab(
                nameInput.getTextBoxInput(),
                consumingCheckbox.selected(),
                autoCommandInput.getTextBoxInput(),
                recipientTypeBoxes.stream()
                        .filter(Checkbox::selected)
                        .map(box -> RecipientType.fromName(box.getMessage().getString()))
                        .collect(Collectors.toSet()),
                filterRegexInput.getTextBoxInput().isBlank() ? null : filterRegexInput.getTextBoxInput());
        Services.ChatTab.addTab(insertIndex, chatTab);
        McUtils.mc().setScreen(ChatTabEditingScreen.create(chatTab));
    }

    private void updateSaveButtonActive() {
        if (orderInput != null && !orderInput.getTextBoxInput().isBlank()) {
            try {
                Integer.parseInt(orderInput.getTextBoxInput());
                orderInput.setRenderColor(CommonColors.GREEN);
            } catch (NumberFormatException ignored) {
                orderInput.setRenderColor(CommonColors.RED);
                saveButton.active = false;
                saveAndCloseButton.active = false;
            }
        }

        if (saveButton == null || saveAndCloseButton == null) return;

        saveButton.active = !nameInput.getTextBoxInput().isEmpty()
                && validatePattern()
                && recipientTypeBoxes.stream().anyMatch(Checkbox::selected);
        saveAndCloseButton.active = saveButton.active;
    }

    private boolean validatePattern() {
        try {
            Pattern.compile(filterRegexInput.getTextBoxInput());
            regexErrorMsg.setMessage(Component.empty());
        } catch (PatternSyntaxException e) {
            MutableComponent errorMessage = Component.literal(e.getDescription())
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(" (at pos " + e.getIndex() + ")").withStyle(ChatFormatting.DARK_RED));
            regexErrorMsg.setMessage(errorMessage);
            return false;
        }
        return true;
    }

    public boolean isActiveChatTab(ChatTab chatTab) {
        return edited != null && edited.equals(chatTab);
    }

    public void reloadChatTabsWidgets() {
        chatTabsWidgets = new ArrayList<>();
        List<ChatTab> chatTabs = Services.ChatTab.getChatTabs();

        int initialVerticalOffset =
                (int) (dividedHeight * 32) - (int) ((dividedHeight * (chatTabs.size() * 5 + 1) + BUTTON_WIDTH) / 2);

        for (int i = 0; i < chatTabs.size(); i++) {
            chatTabsWidgets.add(new ChatTabsWidget(
                    dividedWidth * 3,
                    initialVerticalOffset + dividedHeight * (i * 5),
                    (int) (dividedWidth * 29) - (int) (dividedWidth * 3),
                    (int) (dividedHeight * 4),
                    chatTabs.get(i),
                    29 - 3,
                    this));
        }

        ChatFormatting color = edited == null ? ChatFormatting.GREEN : ChatFormatting.WHITE;

        chatTabsWidgets.add(new Button.Builder(
                        Component.translatable("screens.wynntils.chatTabsGui.new")
                                .withStyle(color),
                        (button) -> McUtils.mc().setScreen(ChatTabEditingScreen.create()))
                .pos(
                        (int) (dividedWidth * 13),
                        initialVerticalOffset + (int) (dividedHeight * (chatTabs.size() * 5 + 1)))
                .size((int) (dividedWidth * 6), BUTTON_WIDTH)
                .build());
    }
}

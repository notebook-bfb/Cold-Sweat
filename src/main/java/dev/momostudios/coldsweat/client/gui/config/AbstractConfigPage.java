package dev.momostudios.coldsweat.client.gui.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(Dist.CLIENT)
public abstract class AbstractConfigPage extends Screen
{
    // Count how many ticks the mouse has been still for
    static int MOUSE_STILL_TIMER = 0;
    static int TOOLTIP_DELAY = 5;
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        MOUSE_STILL_TIMER++;
    }
    @Override
    public void afterMouseMove()
    {
        MOUSE_STILL_TIMER = 0;
        super.afterMouseMove();
    }

    private final Screen parentScreen;
    private final ConfigSettings configSettings;

    public Map<String, List<Widget>> elementBatches = new HashMap<>();
    public Map<String, List<FormattedText>> tooltips = new HashMap<>();

    protected int rightSideLength = 0;
    protected int leftSideLength = 0;

    private static final int TITLE_HEIGHT = ConfigScreen.TITLE_HEIGHT;
    private static final int BOTTOM_BUTTON_HEIGHT_OFFSET = ConfigScreen.BOTTOM_BUTTON_HEIGHT_OFFSET;
    private static final int BOTTOM_BUTTON_WIDTH = ConfigScreen.BOTTOM_BUTTON_WIDTH;
    public static Minecraft mc = Minecraft.getInstance();

    static ResourceLocation TEXTURE = new ResourceLocation("cold_sweat:textures/gui/screen/config_gui.png");

    ImageButton nextNavButton;
    ImageButton prevNavButton;

    public abstract BaseComponent sectionOneTitle();

    @Nullable
    public abstract BaseComponent sectionTwoTitle();

    public AbstractConfigPage(Screen parentScreen, ConfigSettings configSettings)
    {
        super(new TranslatableComponent("cold_sweat.config.title"));
        this.parentScreen = parentScreen;
        this.configSettings = configSettings;
    }

    public int index()
    {
        return 0;
    }

    protected void addEmptySpace(Side side, double units)
    {
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * units;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * units;
    }

    protected void addLabel(String id, Side side, String text)
    {
        this.addLabel(id, side, text, 16777215);
    }

    protected void addLabel(String id, Side side, String text, int color)
    {
        int labelX = side == Side.LEFT ? this.width / 2 - 185 : this.width / 2 + 51;
        int labelY = this.height / 4 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        ConfigLabel label = new ConfigLabel(id, text, labelX, labelY, color);

        this.addRenderableWidget(label);

        this.addElementBatch(id, List.of(label));

        if (side == Side.LEFT)
            this.leftSideLength += font.lineHeight + 4;
        else
            this.rightSideLength += font.lineHeight + 4;
    }

    protected void addButton(String id, Side side, Supplier<String> dynamicLabel, Consumer<Button> onClick,
                             boolean requireOP, boolean setsCustomDifficulty, boolean clientside, String... tooltip)
    {
        String label = dynamicLabel.get();

        boolean shouldBeActive = !requireOP || mc.player == null || mc.player.hasPermissions(2);
        int buttonX = this.width / 2;
        int xOffset = side == Side.LEFT ? -179 : 56;
        int buttonY = this.height / 4 - 8 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        int buttonWidth = 152 + Math.max(0, font.width(label) - 140);

        Button button = new ConfigButton(buttonX + xOffset, buttonY, buttonWidth, 20, new TextComponent(label), button1 ->
        {
            onClick.accept(button1);
            button1.setMessage(new TextComponent(dynamicLabel.get()));
        })
        {
            @Override
            public boolean setsCustomDifficulty()
            {
                return setsCustomDifficulty;
            }
        };
        button.active = shouldBeActive;
        elementBatches.put(id, List.of(button));

        // Add button
        this.addRenderableWidget(button);
        // Add the clientside indicator
        if (clientside) this.addRenderableOnly(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 18, buttonY + 3, 16, 15, 0, 144));

        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE;

        this.setTooltip(id, tooltip);
    }

    protected void addDecimalInput(String id, Side side, Component label, Consumer<Double> writeValue, Consumer<EditBox> readValue,
                                   boolean requireOP, boolean setsCustomDifficulty, boolean clientside, String... tooltip)
    {
        boolean shouldBeActive = !requireOP || mc.player == null || mc.player.hasPermissions(2);
        int xOffset = side == Side.LEFT ? -82 : 151;
        int yOffset = (side == Side.LEFT ? this.leftSideLength : this.rightSideLength) - 2;
        int labelOffset = font.width(label.getString()) > 90 ?
                          font.width(label.getString()) - 84 : 0;

        EditBox textBox = new EditBox(this.font, this.width / 2 + xOffset + labelOffset, this.height / 4 - 6 + yOffset, 51, 22, new TextComponent(""))
        {
            @Override
            public void insertText(String text)
            {
                super.insertText(text);
                CSMath.tryCatch(() ->
                {
                    if (setsCustomDifficulty)
                        configSettings.difficulty = 4;
                    writeValue.accept(Double.parseDouble(this.getValue()));
                });
            }
            @Override
            public void deleteWords(int i)
            {
                super.deleteWords(i);
                CSMath.tryCatch(() ->
                {
                    if (setsCustomDifficulty)
                        configSettings.difficulty = 4;
                    writeValue.accept(Double.parseDouble(this.getValue()));
                });
            }
            @Override
            public void deleteChars(int i)
            {
                super.deleteChars(i);
                CSMath.tryCatch(() ->
                {
                    if (setsCustomDifficulty)
                        configSettings.difficulty = 4;
                    writeValue.accept(Double.parseDouble(this.getValue()));
                });
            }
        };
        textBox.setEditable(shouldBeActive);
        readValue.accept(textBox);
        textBox.setValue(ConfigScreen.TWO_PLACES.format(Double.parseDouble(textBox.getValue())));

        // Add the text box
        this.addRenderableWidget(textBox);
        // Add the label
        this.addRenderableWidget(new ConfigLabel(id, label.getString(), this.width / 2 + xOffset - 95, this.height / 4 + yOffset, shouldBeActive ? 16777215 : 8421504));
        // Add the clientside indicator
        if (clientside) this.addRenderableOnly(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 115, this.height / 4 - 4 + yOffset, 16, 15, 0, 144));

        this.setTooltip(id, tooltip);
        this.addElementBatch(id, List.of(textBox));

        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * 1.2;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * 1.2;
    }

    protected void addDirectionPanel(String id, Side side, TranslatableComponent label, Consumer<Integer> addX, Consumer<Integer> addY, Runnable reset,
                                     boolean requireOP, boolean setsCustomDifficulty, boolean clientside, String... tooltip)
    {
        int xOffset = side == Side.LEFT ? -81 : 152;
        int yOffset = side == Side.LEFT ? this.leftSideLength : this.rightSideLength;

        boolean shouldBeActive = !requireOP || mc.player == null || mc.player.hasPermissions(2);

        int labelOffset = font.width(label.getString()) > 84 ?
                font.width(label.getString()) - 84 : 0;


        // Left button
        ImageButton leftButton = new ImageButton(this.width / 2 + xOffset + labelOffset, this.height / 4 - 8 + yOffset, 14, 20, 0, 0, 20, TEXTURE, button ->
        {
            addX.accept(-1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        leftButton.active = shouldBeActive;
        this.addRenderableWidget(leftButton);

        // Up button
        ImageButton upButton = new ImageButton(this.width / 2 + xOffset + 14 + labelOffset, this.height / 4 - 8 + yOffset, 20, 10, 14, 0, 20, TEXTURE, button ->
        {
            addY.accept(-1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        upButton.active = shouldBeActive;
        this.addRenderableWidget(upButton);

        // Down button
        ImageButton downButton = new ImageButton(this.width / 2 + xOffset + 14 + labelOffset, this.height / 4 + 2 + yOffset, 20, 10, 14, 10, 20, TEXTURE, button ->
        {
            addY.accept(1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        downButton.active = shouldBeActive;
        this.addRenderableWidget(downButton);

        // Right button
        ImageButton rightButton = new ImageButton(this.width / 2 + xOffset + 34 + labelOffset, this.height / 4 - 8 + yOffset, 14, 20, 34, 0, 20, TEXTURE, button ->
        {
            addX.accept(1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        rightButton.active = shouldBeActive;
        this.addRenderableWidget(rightButton);

        // Reset button
        ImageButton resetButton = new ImageButton(this.width / 2 + xOffset + 52 + labelOffset, this.height / 4 - 8 + yOffset, 20, 20, 48, 0, 20, TEXTURE, button ->
        {
            reset.run();

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        resetButton.active = shouldBeActive;
        this.addRenderableWidget(resetButton);

        // Add the option text
        this.addRenderableWidget(new ConfigLabel(id, label.getString(), this.width / 2 + xOffset - 95, this.height / 4 + yOffset, shouldBeActive ? 16777215 : 8421504));
        // Add the clientside indicator
        if (clientside) this.addRenderableOnly(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 114, this.height / 4 - 8 + yOffset + 5, 16, 15, 0, 144));

        this.setTooltip(id, tooltip);
        this.addElementBatch(id, List.of(upButton, downButton, leftButton, rightButton, resetButton));

        // Move down
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * 1.5;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * 1.5;
    }

    @Override
    protected void init()
    {
        this.leftSideLength = 0;
        this.rightSideLength = 0;

        this.addRenderableWidget(new Button(
            this.width / 2 - BOTTOM_BUTTON_WIDTH / 2,
            this.height - BOTTOM_BUTTON_HEIGHT_OFFSET,
            BOTTOM_BUTTON_WIDTH, 20,
            new TranslatableComponent("gui.done"),
            button -> this.close())
        );

        // Navigation
        nextNavButton = new ImageButton(this.width - 32, 12, 20, 20, 0, 88, 20, TEXTURE,
                button -> mc.setScreen(ConfigScreen.getPage(this.index() + 1, parentScreen, configSettings)));
        if (this.index() < ConfigScreen.LAST_PAGE)
            this.addRenderableWidget(nextNavButton);

        prevNavButton = new ImageButton(this.width - 76, 12, 20, 20, 20, 88, 20, TEXTURE,
                button -> mc.setScreen(ConfigScreen.getPage(this.index() - 1, parentScreen, configSettings)));
        if (this.index() > ConfigScreen.FIRST_PAGE)
            this.addRenderableWidget(prevNavButton);
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);

        // Page Title
        drawCenteredString(poseStack, this.font, this.title.getString(), this.width / 2, TITLE_HEIGHT, 0xFFFFFF);

        // Page Number
        drawString(poseStack, this.font, new TextComponent(this.index() + 1 + "/" + (ConfigScreen.LAST_PAGE + 1)), this.width - 53, 18, 16777215);

        // Section 1 Title
        drawString(poseStack, this.font, this.sectionOneTitle(), this.width / 2 - 204, this.height / 4 - 28, 16777215);

        // Section 1 Divider
        RenderSystem.setShaderTexture(0, TEXTURE);
        this.blit(poseStack, this.width / 2 - 202, this.height / 4 - 16, 255, 0, 1, 154);

        if (this.sectionTwoTitle() != null)
        {
            // Section 2 Title
            drawString(poseStack, this.font, this.sectionTwoTitle(), this.width / 2 + 32, this.height / 4 - 28, 16777215);

            // Section 2 Divider
            RenderSystem.setShaderTexture(0, TEXTURE);
            this.blit(poseStack, this.width / 2 + 34, this.height / 4 - 16, 255, 0, 1, 154);
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Render tooltip
        if (MOUSE_STILL_TIMER >= TOOLTIP_DELAY)
        {
            for (Map.Entry<String, List<Widget>> widget : this.elementBatches.entrySet())
            {
                int x;
                int y;
                int maxX;
                int maxY;
                ConfigLabel label = null;
                if (widget.getValue().size() == 1 && widget.getValue().get(0) instanceof Button button)
                {
                    x = button.x;
                    y = button.y;
                    maxX = x + button.getWidth();
                    maxY = y + button.getHeight();
                    String id = widget.getKey();

                    if (mouseX >= x && mouseX <= maxX - 1
                    && mouseY >= y && mouseY <= maxY - 1)
                    {
                        List<FormattedText> tooltipList = this.tooltips.get(id);
                        if (tooltipList != null && !tooltipList.isEmpty())
                        {
                            List<Component> tooltip = this.tooltips.get(id).stream().map(text -> new TextComponent(text.getString())).collect(Collectors.toList());
                            this.renderComponentTooltip(poseStack, tooltip, mouseX, mouseY);
                            break;
                        }
                    }
                }
                else
                {
                    for (GuiEventListener eventListener : this.children())
                    {
                        if (eventListener instanceof ConfigLabel label1 && label1.id.equals(widget.getKey()))
                        {
                            label = label1;
                            break;
                        }
                    }
                    if (label == null) continue;

                    x = label.x;
                    y = label.y;
                    maxX = label.x + font.width(label.text);
                    maxY = label.y + font.lineHeight;

                    if (mouseX >= x - 2 && mouseX <= maxX + 2
                            && mouseY >= y - 5 && mouseY <= maxY + 5)
                    {
                        List<FormattedText> tooltipList = this.tooltips.get(label.id);
                        if (tooltipList != null && !tooltipList.isEmpty())
                        {
                            List<Component> tooltip = this.tooltips.get(label.id).stream().map(text -> new TextComponent(text.getString())).collect(Collectors.toList());
                            this.renderComponentTooltip(poseStack, tooltip, mouseX, mouseY);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void tick()
    {
        super.tick();
    }

    @Override
    public boolean isPauseScreen()
    {
        return true;
    }

    public void close()
    {
        this.onClose();
        Minecraft.getInstance().setScreen(this.parentScreen);
    }

    public enum Side
    {
        LEFT,
        RIGHT
    }

    protected void addElementBatch(String id, List<Widget> elements)
    {
        this.elementBatches.put(id, elements);
    }

    public List<Widget> getElementBatch(String id)
    {
        return this.elementBatches.get(id);
    }

    protected void setTooltip(String id, String[] tooltip)
    {
        List<FormattedText> tooltipList = new ArrayList<>();
        for (String string : tooltip)
        {
            tooltipList.addAll(font.getSplitter().splitLines(string, 300, Style.EMPTY));
        }
        this.tooltips.put(id, tooltipList);
    }
}

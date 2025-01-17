package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import dev.momostudios.coldsweat.util.entity.PlayerHelper;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class RenderLampHand
{
    static Method renderItem = ObfuscationReflectionHelper.findMethod(ItemInHandRenderer.class, "m_109371_",
                                                               AbstractClientPlayer.class, float.class, float.class,
                                                               InteractionHand.class, float.class, ItemStack.class,
                                                               float.class, PoseStack.class, MultiBufferSource.class, int.class);
    static
    {
        renderItem.setAccessible(true);
    }

    @SubscribeEvent
    public static void onHandRender(RenderHandEvent event)
    {

        if (event.getItemStack().getItem() == ModItems.SOULSPRING_LAMP)
        {
            LocalPlayer player = Minecraft.getInstance().player;
            PoseStack ms = event.getPoseStack();
            boolean isRightHand = PlayerHelper.getArmFromHand(event.getHand(), player) == HumanoidArm.RIGHT;

            event.setCanceled(true);

            ms.pushPose();
            ms.mulPose(Vector3f.YP.rotationDegrees(-((float) Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2) * 5 - 5)));
            ms.mulPose(Vector3f.ZP.rotationDegrees(-((float) Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2) * 10 - 10)));

            ms.translate
            (
                0.0d,
                Math.cos(Math.min(event.getSwingProgress() * 1.1, 1) * Math.PI * 2 - Math.PI * 0.5) * 0.1
                    + (event.getEquipProgress() == 0 ? (Math.cos(event.getSwingProgress() * Math.PI * 2) - 1) * 0.2 : 0),
                Math.cos(Math.min(event.getSwingProgress() * 1.1, 1) * Math.PI * 2) * -0.0 - 0
            );

            ms.pushPose();

            if (isRightHand)
            {
                ms.translate(0.75, -0.3, -0.36);
            }
            else
            {
                ms.translate(-0.75, -0.3, -0.36);
            }

            ms.scale(0.75f, 0.8f, 0.72f);

            PlayerRenderer handRenderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (isRightHand)
            {
                ms.mulPose(Vector3f.ZP.rotationDegrees(98));
                ms.mulPose(Vector3f.YP.rotationDegrees(170.0F));
                ms.mulPose(Vector3f.XP.rotationDegrees(90.0F));
                ms.translate(event.getEquipProgress() * 1, -event.getEquipProgress() * 0.2, -event.getEquipProgress() * 0.2);
                handRenderer.renderRightHand(ms, event.getMultiBufferSource(), event.getPackedLight(), player);
            }
            else
            {
                ms.mulPose(Vector3f.ZP.rotationDegrees(-98));
                ms.mulPose(Vector3f.YP.rotationDegrees(190.0F));
                ms.mulPose(Vector3f.XP.rotationDegrees(90.0F));
                ms.translate(-event.getEquipProgress() * 1, -event.getEquipProgress() * 0.2, -event.getEquipProgress() * 0.2);
                handRenderer.renderLeftHand(ms, event.getMultiBufferSource(), event.getPackedLight(), player);
            }
            ms.popPose();

            ms.pushPose();
            ms.translate(-event.getEquipProgress() * 0.05, -event.getEquipProgress() * 0.15, -0.05);
            try
            {
                renderItem.invoke(Minecraft.getInstance().getItemInHandRenderer(),
                                  player,
                                  event.getInterpolatedPitch(),
                                  event.getPartialTicks(),
                                  event.getHand(),
                                  0,
                                  event.getItemStack(),
                                  event.getEquipProgress(),
                                  ms,
                                  event.getMultiBufferSource(),
                                  event.getPackedLight());
            }
            catch (Exception e) {}

            ms.popPose();
            ms.popPose();
        }
    }
}

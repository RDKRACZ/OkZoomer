package io.github.ennuil.okzoomer.zoom;

import com.mojang.blaze3d.systems.RenderSystem;

import io.github.ennuil.libzoomer.api.ZoomOverlay;
import io.github.ennuil.okzoomer.config.OkZoomerConfigPojo;
import io.github.ennuil.okzoomer.config.OkZoomerConfigPojo.FeaturesGroup.ZoomTransitionOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class ZoomerZoomOverlay implements ZoomOverlay {
    private Identifier OVERLAY_ID = new Identifier("okzoomer:zoom_overlay");
    private Identifier OVERLAY_TEXTURE_ID = new Identifier("okzoomer:textures/misc/zoom_overlay.png");
    private boolean active;
    private boolean zoomActive;
    private double divisor;
    private MinecraftClient client;

    public float zoomOverlayAlpha = 0.0F;
    public float lastZoomOverlayAlpha = 0.0F;

    public ZoomerZoomOverlay() {
        this.active = false;
        this.client = MinecraftClient.getInstance();
    }

    @Override
    public Identifier getIdentifier() {
        return OVERLAY_ID;
    }

    @Override
    public boolean getActive() {
        return this.active;
    }

    @Override
    public MinecraftClient setClient(MinecraftClient newClient) {
        return this.client;
    }

    @Override
    public void renderOverlay() {
        if (!this.active) return;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.zoomOverlayAlpha);
        RenderSystem.setShaderTexture(0, OVERLAY_TEXTURE_ID);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(0.0D, (double)this.client.getWindow().getScaledHeight(), -90.0D).texture(0.0F, 1.0F).next();
        bufferBuilder.vertex((double)this.client.getWindow().getScaledWidth(), (double)this.client.getWindow().getScaledHeight(), -90.0D).texture(1.0F, 1.0F).next();
        bufferBuilder.vertex((double)this.client.getWindow().getScaledWidth(), 0.0D, -90.0D).texture(1.0F, 0.0F).next();
        bufferBuilder.vertex(0.0D, 0.0D, -90.0D).texture(0.0F, 0.0F).next();
        tessellator.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void tick(boolean active, double divisor, double transitionMultiplier) {
        this.divisor = divisor;
        this.zoomActive = active;
        if ((!active && zoomOverlayAlpha == 0.0f) || active) {
            this.active = active;
        }

        /*
        Due to how LibZoomer is implemented, it's always going to disappear when the HUD's hidden,
        this is not good for cinematic purposes...
        // TODO - Restore this feature
        if (this.client.options.hudHidden) {
            if (OkZoomerConfigPojo.tweaks.hideZoomOverlay) {
                return;
            }
        }
        */

        float zoomMultiplier = this.zoomActive ? 1.0F : 0.0F;

        lastZoomOverlayAlpha = zoomOverlayAlpha;
        
        if (OkZoomerConfigPojo.features.zoomTransition.equals(ZoomTransitionOptions.SMOOTH)) {
            zoomOverlayAlpha += (zoomMultiplier - zoomOverlayAlpha) * OkZoomerConfigPojo.values.smoothMultiplier;
        } else if (OkZoomerConfigPojo.features.zoomTransition.equals(ZoomTransitionOptions.LINEAR)) {
            double linearStep = 1.0F / this.divisor;
            if (linearStep < OkZoomerConfigPojo.values.minimumLinearStep) {
                linearStep = OkZoomerConfigPojo.values.minimumLinearStep;
            }
            if (linearStep > OkZoomerConfigPojo.values.maximumLinearStep) {
                linearStep = OkZoomerConfigPojo.values.maximumLinearStep;
            }
            zoomOverlayAlpha = MathHelper.stepTowards(zoomOverlayAlpha, zoomMultiplier, (float)linearStep);
        }
    }

    @Override
    public void tickBeforeRender() {
        
    }
}

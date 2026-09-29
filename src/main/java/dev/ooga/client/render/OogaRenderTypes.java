package dev.ooga.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * World overlay render types. Everything Ooga draws in the world (ESP boxes, outlines,
 * tracers) is coloured triangles in one pipeline, drawn with the depth test off so it shows
 * through terrain.
 */
public final class OogaRenderTypes {
	private static final RenderPipeline OVERLAY_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath("ooga", "pipeline/world_overlay"))
			.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.withCull(false)
			.build();

	public static final RenderType OVERLAY = RenderType.create("ooga_world_overlay",
			RenderSetup.builder(OVERLAY_PIPELINE).createRenderSetup());

	private OogaRenderTypes() {
	}
}

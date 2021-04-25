package io.github.joaoh1.okzoomer.packets;

import io.github.joaoh1.okzoomer.config.OkZoomerConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

/* 	Manages the zoom packets and their signals.
	These packets are intended to be used by the future "Zoomer Boomer" server-side mod,
	although developers are welcome to independently transmit them for other loaders */
public class ZoomPackets {
	//The IDs for packets that allows the server to have some control on the zoom.
	public static final Identifier DISABLE_ZOOM_PACKET_ID = new Identifier("okzoomer", "disable_zoom");
	public static final Identifier DISABLE_ZOOM_SCROLLING_PACKET_ID = new Identifier("okzoomer", "disable_zoom_scrolling");
	public static final Identifier FORCE_CLASSIC_MODE_PACKET_ID = new Identifier("okzoomer", "force_classic_mode");
	public static final Identifier FORCE_ZOOM_DIVISOR_PACKET_ID = new Identifier("okzoomer", "force_zoom_divisor");
	public static final Identifier ACKNOWLEDGE_MOD_PACKET_ID = new Identifier("okzoomer", "acknowledge_mod");

	//The signals used by other parts of the zoom in order to enforce the packets.
	private static boolean disableZoom = false;
	private static boolean disableZoomScrolling = false;
	private static boolean forceClassicMode = false;
	private static boolean forceZoomDivisors = false;
	public static double maximumZoomDivisor = 0.0D;
	public static double minimumZoomDivisor = 0.0D;

	private static TranslatableText toastTitle = new TranslatableText("toast.okzoomer.title");

	private static void sendToast(MinecraftClient client, TranslatableText description) {
		SystemToast.add(client.getToastManager(), SystemToast.Type.TUTORIAL_HINT, toastTitle, description);
	}

	//Registers all the packets
	public static void registerPackets() {
		/*	The "Disable Zoom" packet,
			If this packet is received, Ok Zoomer's zoom will be disabled completely while in the server
			Supported since Ok Zoomer 4.0.0 (1.16)
			Arguments: None */
		ClientPlayNetworking.registerGlobalReceiver(DISABLE_ZOOM_PACKET_ID, (client, handler, buf, sender) -> {
			client.execute(() -> {
				sendToast(client, new TranslatableText("toast.okzoomer.disable_zoom"));
				disableZoom = true;
			});
		});

		/*	The "Disable Zoom Scrolling" packet,
			If this packet is received, zoom scrolling will be disabled while in the server
			Supported since Ok Zoomer 4.0.0 (1.16)
			Arguments: None */
		ClientPlayNetworking.registerGlobalReceiver(DISABLE_ZOOM_SCROLLING_PACKET_ID, (client, handler, buf, sender) -> {
			client.execute(() -> {
				sendToast(client, new TranslatableText("toast.okzoomer.disable_zoom_scrolling"));
				disableZoomScrolling = true;
			});
		});

		/*	The "Force Classic Mode" packet,
			If this packet is received, the Classic Mode will be activated while connected to the server,
			under the Classic mode, the Classic preset will be forced on all non-cosmetic options
			Supported since Ok Zoomer 5.0.0-beta.1 (1.17)
			Arguments: None */
		ClientPlayNetworking.registerGlobalReceiver(FORCE_CLASSIC_MODE_PACKET_ID, (client, handler, buf, sender) -> {
			client.execute(() -> {
				sendToast(client, new TranslatableText("toast.okzoomer.force_classic_mode"));
				disableZoomScrolling = true;
				forceClassicMode = true;
				OkZoomerConfig.configureZoomInstance();
			});
		});

		/*	The "Force Zoom Divisor" packet,
			If this packet is received, the minimum and maximum zoom divisor values will be overriden
			with the provided arguments
			Will be supported by Ok Zoomer 5.0.0-beta.2 (1.17)
			Arguments: One double (max & min) or two doubles (first is max, second is min) */
		ClientPlayNetworking.registerGlobalReceiver(FORCE_ZOOM_DIVISOR_PACKET_ID, (client, handler, buf, sender) -> {
			client.execute(() -> {
				sendToast(client, new TranslatableText("toast.okzoomer.force_zoom_divisor"));
				// TODO - Find out why this is not working
				if (buf.readableBytes() == 8) {
					double divisor = buf.readDouble();
					maximumZoomDivisor = divisor;
					minimumZoomDivisor = divisor;
					forceZoomDivisors = true;
				} else if (buf.readableBytes() == 16) {
					double maxDivisor = buf.readDouble();
					double minDivisor = buf.readDouble();
					maximumZoomDivisor = maxDivisor;
					minimumZoomDivisor = minDivisor;
					forceZoomDivisors = true;
				}
				OkZoomerConfig.configureZoomInstance();
			});
		});

		/*	TODO - The "Acknowledge Mod" packet,
			If received, a toast will appear, the toast will either state that
			the server won't restrict the mod or say that the server controls will be activated
			Will have a boolean argument, false for restricting, true for restrictionless */

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			//PacketByteBuf emptyBuf = PacketByteBufs.empty();
			//sender.sendPacket(DISABLE_ZOOM_PACKET_ID, emptyBuf);
			//sender.sendPacket(DISABLE_ZOOM_SCROLLING_PACKET_ID, emptyBuf);
			//sender.sendPacket(FORCE_CLASSIC_MODE_PACKET_ID, emptyBuf);
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeDouble(4.0D);
			sender.sendPacket(FORCE_ZOOM_DIVISOR_PACKET_ID, buf);
		}); 

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (ZoomPackets.disableZoom || ZoomPackets.disableZoomScrolling || ZoomPackets.forceClassicMode) {
				ZoomPackets.resetPacketSignals();	
			}
		});
	}

	public static boolean getDisableZoom() {
		return disableZoom;
	}
	
	public static boolean getDisableZoomScrolling() {
		return disableZoomScrolling;
	}

	public static boolean getForceClassicMode() {
		return forceClassicMode;
	}

	public static boolean getForceZoomDivisors() {
		return forceZoomDivisors;
	}

	public static double getMaximumZoomDivisor() {
		return maximumZoomDivisor;
	}

	public static double getMinimumZoomDivisor() {
		return minimumZoomDivisor;
	}
	
	//The method used to reset the signals once left the server.
	private static void resetPacketSignals() {
		ZoomPackets.disableZoom = false;
		ZoomPackets.disableZoomScrolling = false;
		ZoomPackets.forceZoomDivisors = false;
		ZoomPackets.maximumZoomDivisor = 0.0D;
		ZoomPackets.minimumZoomDivisor = 0.0D;
		if (ZoomPackets.forceClassicMode) {
			ZoomPackets.forceClassicMode = false;
			OkZoomerConfig.configureZoomInstance();
		}
	}
}
package net.dafarka.player_notify;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class PlayerNotify implements ClientModInitializer {
	public static final String MOD_ID = "player_notify";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public static TargetConfig targetConfig = new TargetConfig();

	private int tickCounter = 0;

	@Override
	public void onInitializeClient() {
		ModConfig.init();
		targetConfig = TargetConfig.load();

		if (ModConfig.INSTANCE.enableMod) {
			MinecraftClient client = MinecraftClient.getInstance();

			if (client != null) {
				ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
					dispatcher.register(ClientCommandManager.literal("pn_add")
						.then(ClientCommandManager.argument("player_name", StringArgumentType.string())
							.executes(context -> {
								String playerName = StringArgumentType.getString(context, "player_name");
								targetConfig.targetPlayersName.add(playerName);
								targetConfig.save();
								client.player.sendMessage(Text.of("Added " + playerName + "."), false);
								return 1;
							})))
				);

				ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
					dispatcher.register(ClientCommandManager.literal("pn_remove")
						.then(ClientCommandManager.argument("player_name", StringArgumentType.string())
							.executes(context -> {
								String playerName = StringArgumentType.getString(context, "player_name");
								if (targetConfig.targetPlayersName.contains(playerName)) {
									targetConfig.targetPlayersName.remove(playerName);
									targetConfig.save();
									client.player.sendMessage(Text.of("Removed " + playerName + "."), false);
								} else {
									client.player.sendMessage(Text.of(playerName + " is not in your list of targeted players."), true);
								}
								return 1;
							})))
				);

				ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
					dispatcher.register(ClientCommandManager.literal("pn_clear")
						.executes(context -> {
							targetConfig.targetPlayersName.clear();
							targetConfig.save();
							client.player.sendMessage(Text.of("Cleared all player names."), false);
							return 1;
						})));

				ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
					dispatcher.register(ClientCommandManager.literal("pn_list")
						.executes(context -> {
							for (String playerName : targetConfig.targetPlayersName) {
								client.player.sendMessage(Text.of(playerName), false);
							}
							return 1;
						})));

				ClientTickEvents.END_CLIENT_TICK.register(client_ -> {
					if (client_.world != null && client_.player != null) {
						tickCounter++;
						if (tickCounter >= ModConfig.INSTANCE.seconds * 20) {
							checkPlayersInRange(client_);
							tickCounter = 0;
						}
					}
				});
			}
		}
	}


	private void checkPlayersInRange(MinecraftClient client) {
		ClientPlayerEntity me = client.player;

		if (client.world != null && client.player != null) {
			for (String targetPlayerName : targetConfig.targetPlayersName) {
				for (PlayerEntity player : client.world.getPlayers()) {
					if (player.equals(me)) { continue; }
					if (targetPlayerName.equals((player.getName().getString()))) {
						// Calculate distance (x, z only)
						double dx = player.getX() - client.player.getX();
						double dz = player.getZ() - client.player.getZ();
						double distance = Math.sqrt(dx * dx + dz * dz);

						if (distance <= ModConfig.INSTANCE.range) {
							Text message = Text.literal(targetPlayerName + " is within range!")
								.setStyle(Style.EMPTY
									.withBold(true)
									.withColor(Formatting.RED)
								);
							client.player.sendMessage(message, true);
						}
					}
				}
			}
		}
	}
}
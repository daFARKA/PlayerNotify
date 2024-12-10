package net.dafarka.player_notify;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class PlayerNotify implements ClientModInitializer {
	public static final String MOD_ID = "player_notify";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public static TargetConfig targetConfig = new TargetConfig();

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
								targetConfig.targetPlayersName.add(StringArgumentType.getString(context, "player_name"));
								targetConfig.save();
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
								} else {
									client.player.sendMessage(Text.of("Player " + playerName + " is not in your list of targeted players."), true);
								}
								return 1;
							})))
				);

				ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
					dispatcher.register(ClientCommandManager.literal("pn_clear")
						.executes(context -> {
							targetConfig.targetPlayersName.clear();
							targetConfig.save();
							return 1;
						})));

				ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
					dispatcher.register(ClientCommandManager.literal("pn_list")
						.executes(context -> {
							for (String playerName : targetConfig.targetPlayersName) {
								client.player.sendMessage(Text.of(playerName ), false);
							}
							return 1;
						})));

				for (String targetPlayerName : targetConfig.targetPlayersName) {
					client.execute(() -> {
						if (client.world != null && client.player != null) {
							client.world.getPlayers().forEach(player -> {
								if (targetPlayerName.equals((player.getName().getString()))) {
									// Calculate distance (x, z only)
									double dx = player.getX() - client.player.getX();
									double dz = player.getZ() - client.player.getZ();
									double distance = Math.sqrt(dx * dx + dz * dz);

									if (distance <= ModConfig.INSTANCE.range) {
										Text message = Text.literal("Player " + targetPlayerName + " is within range!")
											.setStyle(Style.EMPTY
												.withBold(true)
												.withColor(Formatting.RED)
											);
										client.player.sendMessage(message, true);
									}
								}
							});
						}
					});
				}
			}
		}
	}
}
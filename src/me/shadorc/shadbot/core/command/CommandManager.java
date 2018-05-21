package me.shadorc.shadbot.core.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.stats.CommandStatsManager;
import me.shadorc.shadbot.data.stats.CommandStatsManager.CommandEnum;
import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;

public class CommandManager {

	private static final Map<String, AbstractCommand> COMMANDS_MAP = new HashMap<>();

	public static boolean init() {
		LogUtils.infof("Initializing commands...");

		Reflections reflections = new Reflections(Shadbot.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(Class<?> cmdClass : reflections.getTypesAnnotatedWith(Command.class)) {
			String cmdName = cmdClass.getSimpleName();
			if(!AbstractCommand.class.isAssignableFrom(cmdClass)) {
				LogUtils.error(String.format("An error occurred while generating command, %s cannot be casted to %s.",
						cmdName, AbstractCommand.class.getSimpleName()));
				continue;
			}

			try {
				AbstractCommand cmd = AbstractCommand.class.cast(cmdClass.getConstructor().newInstance());

				List<String> names = cmd.getNames();
				if(!cmd.getAlias().isEmpty()) {
					names.add(cmd.getAlias());
				}

				for(String name : names) {
					if(COMMANDS_MAP.putIfAbsent(name, cmd) != null) {
						LogUtils.error(String.format("Command name collision between %s and %s",
								cmdName, COMMANDS_MAP.get(name).getClass().getSimpleName()));
						continue;
					}
				}
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while initializing command %s.", cmdName));
				return false;
			}
		}

		LogUtils.infof("%s initialized.", StringUtils.pluralOf(COMMANDS_MAP.values().stream().distinct().count(), "command"));
		return true;
	}

	// TODO: Avoid to block everything
	public static void execute(Context context) {
		AbstractCommand cmd = COMMANDS_MAP.get(context.getCommandName());
		if(cmd == null) {
			return;
		}

		if(!BotUtils.isCommandAllowed(context.getGuildId().get(), cmd)) {
			return;
		}

		CommandPermission authorPermission = context.getAuthorPermission();
		if(cmd.getPermission().isSuperior(authorPermission)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " You do not have the permission to execute this command.", context.getChannel());
			return;
		}

		if(cmd.getRateLimiter().isPresent()
				&& cmd.getRateLimiter().get().isLimited(context.getGuildId().get(), context.getChannelId(), context.getAuthorId())) {
			CommandStatsManager.log(CommandEnum.COMMAND_LIMITED, cmd);
			return;
		}

		try {
			cmd.execute(context);
			CommandStatsManager.log(CommandEnum.COMMAND_USED, cmd);
			VariousStatsManager.log(VariousEnum.COMMANDS_EXECUTED);
		} catch (IllegalCmdArgumentException err) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + err.getMessage(), context.getChannel());
			CommandStatsManager.log(CommandEnum.COMMAND_ILLEGAL_ARG, cmd);
		} catch (MissingArgumentException err) {
			BotUtils.sendMessage(new MessageCreateSpec()
					.setContent(TextUtils.MISSING_ARG)
					.setEmbed(cmd.getHelp(context.getPrefix())), context.getChannel());
			CommandStatsManager.log(CommandEnum.COMMAND_MISSING_ARG, cmd);
		}
	}

	public static Map<String, AbstractCommand> getCommands() {
		return COMMANDS_MAP;
	}

	public static AbstractCommand getCommand(String name) {
		return COMMANDS_MAP.get(name);
	}
}

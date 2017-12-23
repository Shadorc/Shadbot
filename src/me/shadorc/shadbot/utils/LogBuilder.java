package me.shadorc.shadbot.utils;

import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.LogUtils.LogType;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class LogBuilder {

	private final LogType type;
	private final String message;
	private final Exception err;
	private final String input;
	private final AbstractCommand cmd;
	private final IChannel channel;

	public LogBuilder(LogType type, String message, Exception err, String input, AbstractCommand cmd, IChannel channel) {
		this.type = type;
		this.message = message;
		this.err = err;
		this.input = input;
		this.cmd = cmd;
		this.channel = channel;
	}

	public LogBuilder(LogType type, String message, Exception err) {
		this(type, message, err, null, null, null);
	}

	public LogBuilder(LogType type, String message) {
		this(type, message, null, null, null, null);
	}

	public EmbedObject build() {
		EmbedBuilder builder = new EmbedBuilder()
				.setLenient(true)
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withAuthorName(String.format("%s (Version: %s)", StringUtils.capitalize(type.toString()), Shadbot.version))
				.withDescription(message);

		if(cmd != null) {
			builder.appendField("Command", cmd.getName(), false);
		}

		if(err != null) {
			builder.appendField("Error type", err.getClass().getSimpleName(), false);
			builder.appendField("Error message", err.getMessage(), false);
		}

		if(input != null) {
			builder.appendField("Input", input, false);
		}

		builder.appendField("User warned", Boolean.toString(channel != null), false);

		return builder.build();
	}
}

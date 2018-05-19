package me.shadorc.shadbot.utils.embed;

import java.awt.Color;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.StringUtils;

public class LogBuilder {

	private final LogType type;
	private final String message;
	private final Throwable err;
	private final String input;

	public LogBuilder(LogType type, String message, Throwable err, String input) {
		this.type = type;
		this.message = message;
		this.err = err;
		this.input = input;
	}

	public LogBuilder(LogType type, String message, Throwable err) {
		this(type, message, err, null);
	}

	public LogBuilder(LogType type, String message) {
		this(type, message, null, null);
	}

	public EmbedCreateSpec build() {
		EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
				// .setLenient(true)
				.setAuthor(String.format("%s (Version: %s)", StringUtils.capitalize(type.toString()), Config.VERSION), null, null)
				.setDescription(message);

		switch (type) {
			case ERROR:
				embed.setColor(Color.RED.getRGB());
				break;
			case WARN:
				embed.setColor(Color.ORANGE.getRGB());
				break;
			case INFO:
				embed.setColor(Color.GREEN.getRGB());
				break;
		}

		if(err != null) {
			embed.addField("Error type", err.getClass().getSimpleName(), false);
			embed.addField("Error message", err.getMessage(), false);
		}

		if(input != null) {
			embed.addField("Input", input, false);
		}

		return embed;
	}
}

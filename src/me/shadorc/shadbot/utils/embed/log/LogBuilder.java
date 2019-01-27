package me.shadorc.shadbot.utils.embed.log;

import java.awt.Color;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;

public class LogBuilder {

	public enum LogType {
		INFO, WARN, ERROR;
	}

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

	public Consumer<? super EmbedCreateSpec> build() {
		return embed -> {
			EmbedUtils.getDefaultEmbed().accept(embed);
			embed.setAuthor(String.format("%s (Version: %s)", StringUtils.capitalizeEnum(this.type), Config.VERSION), null, null);
			embed.setDescription(this.message);

			switch (this.type) {
				case ERROR:
					embed.setColor(Color.RED);
					break;
				case WARN:
					embed.setColor(Color.ORANGE);
					break;
				case INFO:
					embed.setColor(Color.GREEN);
					break;
				default:
					embed.setColor(Color.BLUE);
					break;
			}
	
			if(this.err != null) {
				embed.addField("Error type", this.err.getClass().getSimpleName(), false);
				if(this.err.getMessage() != null) {
					embed.addField("Error message", this.err.getMessage(), false);
				}
			}
	
			if(this.input != null) {
				embed.addField("Input", this.input, false);
			}
		};
	}

}

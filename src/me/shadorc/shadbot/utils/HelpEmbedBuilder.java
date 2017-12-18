package me.shadorc.shadbot.utils;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.data.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.Context;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

public class HelpEmbedBuilder {

	private final Context context;
	private final AbstractCommand cmd;

	private String usage;
	private String example;
	private String description;

	public HelpEmbedBuilder(Context context, AbstractCommand cmd) {
		this.context = context;
		this.cmd = cmd;
	}

	public HelpEmbedBuilder setDescription(String description) {
		this.description = String.format("**%s**", description);
		return this;
	}

	public HelpEmbedBuilder setArgs(String... args) {
		this.usage = String.format("`%s%s %s`",
				context.getPrefix(),
				cmd.getName(),
				FormatUtils.formatArray(args, arg -> String.format("<%s>", arg), " "));
		return this;
	}

	public HelpEmbedBuilder setExample(String example) {
		this.example = example;
		return this;
	}

	public EmbedObject build() {
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setLenient(true)
				.withAuthorName(String.format("Help for %s command", cmd.getName()))
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.withDescription(description)
				.appendField("Usage", usage, false)
				.appendField("Example", example, false);

		if(cmd.getAlias() != null) {
			embedBuilder.withFooterText(String.format("Alias: %s", cmd.getAlias()));
		}

		return embedBuilder.build();
	}
}

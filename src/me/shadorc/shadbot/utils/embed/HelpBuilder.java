package me.shadorc.shadbot.utils.embed;

import java.util.ArrayList;
import java.util.List;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.FormatUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.obj.Embed.EmbedField;
import sx.blah.discord.util.EmbedBuilder;

public class HelpBuilder {

	private final String prefix;
	private final AbstractCommand cmd;
	private final List<Argument> args;
	private final List<EmbedField> fields;

	private String example;
	private String usage;
	private String description;

	public HelpBuilder(AbstractCommand cmd, String prefix) {
		this.prefix = prefix;
		this.cmd = cmd;
		this.args = new ArrayList<>();
		this.fields = new ArrayList<>();
	}

	public HelpBuilder setDescription(String description) {
		this.description = String.format("**%s**", description);
		return this;
	}

	public HelpBuilder setExample(String example) {
		this.example = example;
		return this;
	}

	public HelpBuilder setUsage(String usage) {
		this.usage = usage;
		return this;
	}

	public HelpBuilder addArg(String name, String desc, boolean isFacultative) {
		args.add(new Argument(name, desc, isFacultative));
		return this;
	}

	public HelpBuilder addArg(String name, boolean isFacultative) {
		return this.addArg(name, null, isFacultative);
	}

	public HelpBuilder addArg(List<?> options, boolean isFacultative) {
		return this.addArg(FormatUtils.formatList(options, opt -> opt.toString(), "|"), null, isFacultative);
	}

	public HelpBuilder appendField(String name, String value, boolean inline) {
		fields.add(new EmbedField(name, value, inline));
		return this;
	}

	public EmbedObject build() {
		EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(String.format("Help for %s command", cmd.getName()))
				.withDescription(description)
				.appendField("Usage", this.getUsage(), false)
				.appendField("Arguments", this.getArguments(), false)
				.appendField("Example", example, false);

		for(EmbedField field : fields) {
			embedBuilder.appendField(field);
		}

		if(!cmd.getAlias().isEmpty()) {
			embedBuilder.withFooterText(String.format("Alias: %s", cmd.getAlias()));
		}

		return embedBuilder.build();
	}

	private String getUsage() {
		StringBuilder usageBld = new StringBuilder(String.format("`%s%s ", prefix, cmd.getName()));
		if(usage == null) {
			usageBld.append(FormatUtils.formatList(args, arg -> String.format(arg.isFacultative() ? "[<%s>]" : "<%s>", arg.getName()), " "));
		} else {
			usageBld.append(usage);
		}
		usageBld.append('`');
		return usageBld.toString();
	}

	private String getArguments() {
		StringBuilder argBld = new StringBuilder();
		for(Argument arg : args) {
			if(arg.getDesc() == null) {
				continue;
			}

			argBld.append(String.format("\n**%s** - ", arg.getName()));
			if(arg.isFacultative()) {
				argBld.append("[OPTIONAL] ");
			}
			argBld.append(arg.getDesc());
		}

		return argBld.toString();
	}
}

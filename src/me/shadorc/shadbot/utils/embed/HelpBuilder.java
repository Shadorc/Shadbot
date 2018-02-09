package me.shadorc.shadbot.utils.embed;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

	private String thumbnail;
	private String description;
	private String usage;
	private String example;
	private String gains;
	private String source;

	public HelpBuilder(AbstractCommand cmd, String prefix) {
		this.prefix = prefix;
		this.cmd = cmd;
		this.args = new ArrayList<>();
		this.fields = new ArrayList<>();
	}

	public HelpBuilder setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
		return this;
	}

	public HelpBuilder setDescription(String description) {
		this.description = String.format("**%s**", description);
		return this;
	}

	public HelpBuilder setFullUsage(String usage) {
		this.usage = usage;
		return this;
	}

	public HelpBuilder setUsage(String usage) {
		return this.setFullUsage(String.format("%s%s %s", prefix, cmd.getName(), usage));
	}

	public HelpBuilder setExample(String example) {
		this.example = example;
		return this;
	}

	public HelpBuilder setGains(String format, Object... args) {
		this.gains = String.format(format, args);
		return this;
	}

	public HelpBuilder setSource(String source) {
		this.source = source;
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
		return this.addArg(FormatUtils.format(options, Object::toString, "|"), null, isFacultative);
	}

	public HelpBuilder addArg(Object[] options, boolean isFacultative) {
		return this.addArg(List.of(options), isFacultative);
	}

	public HelpBuilder appendField(String name, String value, boolean inline) {
		fields.add(new EmbedField(name, value, inline));
		return this;
	}

	public EmbedObject build() {
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(String.format("Help for %s command", cmd.getName()))
				.withDescription(description)
				.appendField("Usage", this.getUsage(), false)
				.appendField("Arguments", this.getArguments(), false)
				.appendField("Example", example, false)
				.appendField("Gains", gains, false)
				.appendField("Source", source, false);

		if(thumbnail != null) {
			embed.withThumbnail(thumbnail);
		}

		for(EmbedField field : fields) {
			embed.appendField(field);
		}

		if(!cmd.getAlias().isEmpty()) {
			embed.withFooterText(String.format("Alias: %s", cmd.getAlias()));
		}

		return embed.build();
	}

	private String getUsage() {
		if(usage != null) {
			return String.format("`%s`", usage);
		}

		return String.format("`%s%s %s`",
				prefix, cmd.getName(),
				FormatUtils.format(args, arg -> String.format(arg.isFacultative() ? "[<%s>]" : "<%s>", arg.getName()), " "));
	}

	private String getArguments() {
		return args.stream()
				.filter(arg -> arg.getDesc() != null)
				.map(arg -> String.format("%n**%s** %s - %s", arg.getName(), arg.isFacultative() ? "[optional] " : "", arg.getDesc()))
				.collect(Collectors.joining());
	}
}

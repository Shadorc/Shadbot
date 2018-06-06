package me.shadorc.shadbot.utils.embed;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.FormatUtils;

public class HelpBuilder {

	private final String prefix;
	private final AbstractCommand cmd;
	private final List<Argument> args;
	private final List<EmbedFieldEntity> fields;

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

	public <T> HelpBuilder addArg(List<T> options, boolean isFacultative) {
		return this.addArg(FormatUtils.format(options, Object::toString, "|"), null, isFacultative);
	}

	public <T> HelpBuilder addArg(T[] options, boolean isFacultative) {
		return this.addArg(List.of(options), isFacultative);
	}

	public HelpBuilder addField(String name, String value, boolean inline) {
		fields.add(new EmbedFieldEntity(name, value, inline));
		return this;
	}

	public EmbedCreateSpec build() {
		EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed(String.format("Help for %s command", cmd.getName()))
				.setDescription(description)
				.addField("Usage", this.getUsage(), false)
				.addField("Arguments", this.getArguments(), false)
				.addField("Example", example, false)
				.addField("Gains", gains, false)
				.addField("Source", source, false);

		if(thumbnail != null) {
			embed.setThumbnail(thumbnail);
		}

		for(EmbedFieldEntity field : fields) {
			embed.addField(field.getName(), field.getValue(), field.isInline());
		}

		if(!cmd.getAlias().isEmpty()) {
			embed.setFooter(String.format("Alias: %s", cmd.getAlias()), null);
		}

		return embed;
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

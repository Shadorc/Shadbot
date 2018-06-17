package me.shadorc.shadbot.utils.embed;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.FormatUtils;
import reactor.core.publisher.Mono;

public class HelpBuilder {

	private final Context context;
	private final AbstractCommand cmd;
	private final List<Argument> args;
	private final List<EmbedFieldEntity> fields;

	private String thumbnail;
	private String description;
	private String usage;
	private String example;
	private String gains;
	private String source;

	public HelpBuilder(AbstractCommand cmd, Context context) {
		this.context = context;
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
		return this.setFullUsage(String.format("%s%s %s", context.getPrefix(), cmd.getName(), usage));
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

	public Mono<EmbedCreateSpec> build() {
		return context.getAuthorAvatarUrl()
				.map(avatarUrl -> {
					EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Help for %s command", cmd.getName()), null, avatarUrl)
							.addField("Usage", this.getUsage(), false);

					if(!this.getArguments().isEmpty()) {
						embed.addField("Arguments", this.getArguments(), false);
					}

					if(example != null) {
						embed.addField("Example", example, false);
					}

					if(gains != null) {
						embed.addField("Gains", gains, false);
					}

					if(source != null) {
						embed.addField("Source", source, false);
					}

					if(description != null) {
						embed.setDescription(description);
					}

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
				});
	}

	private String getUsage() {
		if(usage != null) {
			return String.format("`%s`", usage);
		}

		return String.format("`%s%s %s`",
				context.getPrefix(), cmd.getName(),
				FormatUtils.format(args, arg -> String.format(arg.isFacultative() ? "[<%s>]" : "<%s>", arg.getName()), " "));
	}

	private String getArguments() {
		return args.stream()
				.filter(arg -> arg.getDesc() != null)
				.map(arg -> String.format("%n**%s** %s - %s", arg.getName(), arg.isFacultative() ? "[optional] " : "", arg.getDesc()))
				.collect(Collectors.joining());
	}
}

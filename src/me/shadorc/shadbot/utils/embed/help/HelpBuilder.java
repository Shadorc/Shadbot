package me.shadorc.shadbot.utils.embed.help;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
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
	private String delimiter;

	public HelpBuilder(AbstractCommand cmd, Context context) {
		this.context = context;
		this.cmd = cmd;
		this.args = new ArrayList<>();
		this.fields = new ArrayList<>();
		this.delimiter = Config.DEFAULT_COMMAND_DELIMITER;
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
		return this.setFullUsage(String.format("%s%s %s", this.context.getPrefix(), this.cmd.getName(), usage));
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

	public HelpBuilder setDelimiter(String delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	public HelpBuilder addArg(String name, String desc, boolean isFacultative) {
		this.args.add(new Argument(name, desc, isFacultative));
		return this;
	}

	public HelpBuilder addArg(String name, boolean isFacultative) {
		return this.addArg(name, null, isFacultative);
	}

	public <T> HelpBuilder addArg(Collection<T> options, boolean isFacultative) {
		return this.addArg(FormatUtils.format(options, Object::toString, "|"), null, isFacultative);
	}

	public <T> HelpBuilder addArg(T[] options, boolean isFacultative) {
		return this.addArg(List.of(options), isFacultative);
	}

	public HelpBuilder addField(String name, String value, boolean inline) {
		this.fields.add(new EmbedFieldEntity(name, value, inline));
		return this;
	}

	public Mono<Consumer<? super EmbedCreateSpec>> build() {
		return this.context.getAvatarUrl()
				.map(avatarUrl -> {
					final Consumer<? super EmbedCreateSpec> embedConsumer = embed -> {
						EmbedUtils.getDefaultEmbed().accept(embed);
						embed.setAuthor(String.format("Help for %s command", this.cmd.getName()), null, avatarUrl);
						embed.addField("Usage", this.getUsage(), false);
	
						if(this.description != null) {
							embed.setDescription(this.description);
						}
	
						if(this.thumbnail != null) {
							embed.setThumbnail(this.thumbnail);
						}
	
						if(!this.getArguments().isEmpty()) {
							embed.addField("Arguments", this.getArguments(), false);
						}
	
						if(this.example != null) {
							embed.addField("Example", this.example, false);
						}
	
						if(this.gains != null) {
							embed.addField("Gains", this.gains, false);
						}
	
						if(this.source != null) {
							embed.addField("Source", this.source, false);
						}
	
						for(final EmbedFieldEntity field : this.fields) {
							embed.addField(field.getName(), field.getValue(), field.isInline());
						}
	
						if(!this.cmd.getAlias().isEmpty()) {
							embed.setFooter(String.format("Alias: %s", this.cmd.getAlias()), null);
						}
					};
					
					return embedConsumer;
			});
	}

	private String getUsage() {
		if(this.usage != null) {
			return String.format("`%s`", this.usage);
		}

		return String.format("`%s%s %s`",
				this.context.getPrefix(), this.cmd.getName(),
				FormatUtils.format(this.args, arg -> String.format(arg.isFacultative() ? "[<%s>]" : "<%s>", arg.getName()), this.delimiter));
	}

	private String getArguments() {
		return this.args.stream()
				.filter(arg -> Objects.nonNull(arg.getDesc()))
				.map(arg -> String.format("%n**%s** %s - %s", arg.getName(), arg.isFacultative() ? "[optional] " : "", arg.getDesc()))
				.collect(Collectors.joining());
	}
}

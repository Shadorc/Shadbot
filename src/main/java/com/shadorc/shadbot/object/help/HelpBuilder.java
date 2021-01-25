package com.shadorc.shadbot.object.help;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.command.Option;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class HelpBuilder {

    protected final Context context;

    protected final List<Option> options;
    protected final List<ImmutableEmbedFieldData> fields;

    @Nullable
    private String authorName;
    @Nullable
    private String authorUrl;
    @Nullable
    private String thumbnailUrl;
    @Nullable
    private String description;
    @Nullable
    private String usage;
    @Nullable
    private String example;
    @Nullable
    private String source;
    @Nullable
    private String footer;

    protected HelpBuilder(Context context) {
        this.context = context;
        this.options = new ArrayList<>();
        this.fields = new ArrayList<>();
    }

    public HelpBuilder setAuthor(String authorName, String authorUrl) {
        this.authorName = authorName;
        this.authorUrl = authorUrl;
        return this;
    }

    public HelpBuilder setThumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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

    public HelpBuilder setExample(String example) {
        this.example = example;
        return this;
    }

    public HelpBuilder setSource(String source) {
        this.source = source;
        return this;
    }

    public HelpBuilder setFooter(String footer) {
        this.footer = footer;
        return this;
    }

    public HelpBuilder addField(String name, String value, boolean inline) {
        this.fields.add(ImmutableEmbedFieldData.of(name, value, Possible.of(inline)));
        return this;
    }

    public Consumer<EmbedCreateSpec> build() {
        return ShadbotUtil.getDefaultEmbed(embed -> {
            if (this.authorName != null && !this.authorName.isBlank()) {
                embed.setAuthor(this.authorName, this.authorUrl, this.context.getAuthorAvatarUrl());
            }
            embed.addField("Usage", this.getUsage(), false);

            if (this.description != null && !this.description.isBlank()) {
                embed.setDescription(this.description);
            }

            if (this.thumbnailUrl != null && !this.thumbnailUrl.isBlank()) {
                embed.setThumbnail(this.thumbnailUrl);
            }

            if (!this.getArguments().isEmpty()) {
                embed.addField("Arguments", this.getArguments(), false);
            }

            if (this.example != null && !this.example.isBlank()) {
                embed.addField("Example", this.example, false);
            }

            if (this.source != null && !this.source.isBlank()) {
                embed.addField("Source", this.source, false);
            }

            for (final ImmutableEmbedFieldData field : this.fields) {
                embed.addField(field.name(), field.value(), field.inline().get());
            }

            if (this.footer != null && !this.footer.isBlank()) {
                embed.setFooter(this.footer, null);
            }
        });
    }

    protected abstract String getCommandName();

    private String getUsage() {
        if (this.usage != null && !this.usage.isBlank()) {
            return String.format("`%s`", this.usage);
        }

        if (this.options.isEmpty()) {
            return String.format("`/%s`", this.getCommandName());
        }

        return String.format("`/%s %s`", this.getCommandName(),
                FormatUtil.format(this.options,
                        option -> String.format(option.isRequired() ? "<%s>" : "[<%s>]", option.getName()), " "));
    }

    private String getArguments() {
        return this.options.stream()
                .map(option -> String.format("%n**%s** %s - %s",
                        option.getName(), option.isRequired() ? "" : "[optional] ", option.getDescription()))
                .collect(Collectors.joining());
    }
}

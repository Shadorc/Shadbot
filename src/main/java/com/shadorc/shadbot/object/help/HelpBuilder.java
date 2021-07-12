package com.shadorc.shadbot.object.help;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class HelpBuilder {

    protected final Context context;
    protected final List<ApplicationCommandOptionData> options;

    @Nullable
    private String authorName;
    @Nullable
    private String authorUrl;
    @Nullable
    private String description;

    protected HelpBuilder(Context context) {
        this.context = context;
        this.options = new ArrayList<>();
    }

    public HelpBuilder setAuthor(String authorName, String authorUrl) {
        this.authorName = authorName;
        this.authorUrl = authorUrl;
        return this;
    }

    public HelpBuilder setDescription(String description) {
        this.description = "**%s**".formatted(description);
        return this;
    }

    public EmbedCreateSpec build() {
        final EmbedCreateSpec.Builder embed = ShadbotUtil.createEmbedBuilder();
        if (!StringUtil.isBlank(this.authorName)) {
            embed.author(this.authorName, this.authorUrl, this.context.getAuthorAvatar());
        }
        embed.addField(this.context.localize("help.usage"), this.getUsage(), false);

        if (!StringUtil.isBlank(this.description)) {
            embed.description(this.description);
        }

        final String args = this.getArguments();
        if (!StringUtil.isBlank(args)) {
            embed.addField(this.context.localize("help.arguments"), args, false);
        }

        return embed.build();
    }

    protected abstract String getCommandName();

    private String getUsage() {
        if (this.options.isEmpty()) {
            return "`/%s`".formatted(this.getCommandName());
        }

        return "`/%s %s`".formatted(this.getCommandName(),
                FormatUtil.format(this.options,
                        option -> String.format(option.required().toOptional().orElse(false) ? "<%s>" : "[<%s>]",
                                option.name()), " "));
    }

    private String getArguments() {
        return this.options.stream()
                .map(option -> "%n**%s** %s - %s"
                        .formatted(option.name(), option.required().toOptional().orElse(false) ?
                                        "" : this.context.localize("help.optional"),
                                option.description()))
                .collect(Collectors.joining());
    }
}

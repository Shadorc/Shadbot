package com.shadorc.shadbot.command.hidden;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.guild.entity.DBGuild;
import com.shadorc.shadbot.db.guild.GuildManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class PrefixCmd extends BaseCmd {

    public PrefixCmd() {
        super(CommandCategory.HIDDEN, List.of("prefix"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final DBGuild dbGuild = GuildManager.getInstance().getDBGuild(context.getGuildId());
        final String prefix = dbGuild.getSettings().getPrefix();

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.INFO + " The prefix for this server is `%s`. For example: `%shelp`",
                                prefix, prefix), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show the current prefix for this server.")
                .setFullUsage(String.format("%s%s", Config.DEFAULT_PREFIX, this.getName()))
                .build();
    }
}

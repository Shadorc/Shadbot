package com.shadorc.shadbot.command.hidden;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.database.DBGuild;
import com.shadorc.shadbot.db.database.DatabaseManager;
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
        final DBGuild dbGuild = DatabaseManager.getInstance().getDBGuild(context.getGuildId());

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.INFO + " The prefix for this server is `%s`. For example: `%shelp`",
                                dbGuild.getPrefix(), dbGuild.getPrefix()), channel))
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

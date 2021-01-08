/*
package com.shadorc.shadbot.command.hidden;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.function.Consumer;

public class PrefixCmd extends BaseCmd {

    public PrefixCmd() {
        super(CommandCategory.HIDDEN, List.of("prefix"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .map(DBGuild::getSettings)
                .map(Settings::getPrefix)
                .zipWith(context.getChannel())
                .flatMap(TupleUtils.function((prefix, channel) -> DiscordUtils.sendMessage(
                        String.format(Emoji.INFO + " The prefix for this server is `%s`. For example: `%shelp`",
                                prefix, prefix), channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show the current prefix for this server.")
                .setFullUsage(String.format("%s%s", Config.DEFAULT_PREFIX, this.getName()))
                .build();
    }
}
*/

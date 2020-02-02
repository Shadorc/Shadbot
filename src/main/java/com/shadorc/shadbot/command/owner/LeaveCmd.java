package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class LeaveCmd extends BaseCmd {

    public LeaveCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("leave"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final Long guildId = NumberUtils.toPositiveLongOrNull(arg);
        if (guildId == null) {
            return Mono.error(new CommandException(String.format("`%s` is not a valid guild ID.", arg)));
        }

        return context.getClient().getGuildById(Snowflake.of(guildId))
                .onErrorMap(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                        err -> new CommandException("Guild not found."))
                .flatMap(Guild::leave)
                .then(context.getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.CHECK_MARK + " Guild with ID **%d** left.", guildId), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Leave a guild.")
                .addArg("guildID", false)
                .build();
    }

}

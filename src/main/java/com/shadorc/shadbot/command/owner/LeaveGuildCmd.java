package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.NumberUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.ApplicationCommandOptionType;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

public class LeaveGuildCmd extends BaseCmd {

    public LeaveGuildCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, "leave_guild", "Leave a guild");
        this.addOption("guildId", "The ID of the guild to leave", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String arg = context.getOptionAsString("guildId").orElseThrow();

        final Long guildId = NumberUtil.toPositiveLongOrNull(arg);
        if (guildId == null) {
            return Mono.error(new CommandException("`%s` is not a valid guild ID.".formatted(arg)));
        }

        return context.getClient()
                .getGuildById(Snowflake.of(guildId))
                .onErrorMap(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                        err -> new CommandException("Guild not found."))
                .flatMap(Guild::leave)
                .then(context.reply(Emoji.CHECK_MARK,"Guild (ID: **%d**) left.".formatted(guildId)));
    }

}

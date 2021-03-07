package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.ApplicationCommandOptionType;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

public class SendMessageCmd extends BaseCmd {

    public SendMessageCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, "send_message", "Send a private message to a user");

        this.addOption("user", "The user to send a message to", true, ApplicationCommandOptionType.USER);
        this.addOption("message", "The message to send", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Snowflake userId = Snowflake.of(context.getOptionAsString("user").orElseThrow());
        if (userId.equals(context.getClient().getSelfId())) {
            return Mono.error(new CommandException("I can't send a private message to myself."));
        }

        return context.getClient()
                .getUserById(userId)
                .switchIfEmpty(Mono.error(new CommandException("User not found.")))
                .flatMap(user -> {
                    if (user.isBot()) {
                        return Mono.error(new CommandException("I can't send private message to other bots."));
                    }

                    final String message = context.getOptionAsString("messahe").orElseThrow();
                    return user.getPrivateChannel()
                            .cast(MessageChannel.class)
                            .flatMap(privateChannel -> DiscordUtil.sendMessage(message, privateChannel))
                            .onErrorMap(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                                    err -> new CommandException("I'm not allowed to send a private message to this user."))
                            .then(context.createFollowupMessage(Emoji.CHECK_MARK + " Message \"%s\" sent to **%s** (%s).",
                                    message, user.getUsername(), user.getId().asString()));
                });
    }

}

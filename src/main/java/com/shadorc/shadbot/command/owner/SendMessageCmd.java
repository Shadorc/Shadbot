package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Snowflake;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class SendMessageCmd extends BaseCmd {

    public SendMessageCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("send_message", "send-message", "sendmessage"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final Long userId = NumberUtils.toPositiveLongOrNull(args.get(0));
        if (userId == null) {
            return Mono.error(new CommandException(String.format("`%s` is not a valid user ID.",
                    args.get(0))));
        }

        if (Snowflake.of(userId).equals(Shadbot.getSelfId())) {
            return Mono.error(new CommandException("I can't send a private message to myself."));
        }

        return context.getClient()
                .getUserById(Snowflake.of(userId))
                .switchIfEmpty(Mono.error(new CommandException("User not found.")))
                .flatMap(user -> {
                    if (user.isBot()) {
                        return Mono.error(new CommandException("I can't send private message to other bots."));
                    }

                    return user.getPrivateChannel()
                            .cast(MessageChannel.class)
                            .flatMap(privateChannel -> DiscordUtils.sendMessage(args.get(1), privateChannel))
                            .onErrorMap(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                                    err -> new CommandException("I'm not allowed to send a private message to this user."))
                            .then(context.getChannel())
                            .flatMap(channel -> DiscordUtils.sendMessage(
                                    String.format(Emoji.CHECK_MARK + " Message \"%s\" sent to **%s** (%s).",
                                            args.get(1), user.getUsername(), user.getId().asLong()), channel));
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Send a private message to a user.")
                .addArg("userID", false)
                .addArg("message", false)
                .build();
    }

}

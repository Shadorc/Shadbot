package com.shadorc.shadbot.command.moderation;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.NumberUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class PruneCmd extends BaseCmd {

    private static final int MAX_MESSAGES = 100;
    private static final int MESSAGES_OFFSET = 2;

    public PruneCmd() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, "prune", "Delete messages (include embeds)");
        this.addOption("authors", "Authors of the messages, comma separated", false,
                ApplicationCommandOptionType.USER);
        this.addOption("words", "Words contained in the messages, comma separated", false,
                ApplicationCommandOptionType.STRING);
        this.addOption("limit", "Maximum number of messages to delete (max: %d)".formatted(MAX_MESSAGES),
                false, ApplicationCommandOptionType.INTEGER);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String[] authors = context.getOptionAsString("authors").orElse("").split(",");
        final String[] words = context.getOptionAsString("words").orElse("").split(",");
        final Optional<String> limitOpt = context.getOptionAsString("limit");

        return context.reply(Emoji.HOURGLASS, context.localize("prune.loading"))
                .then(context.getChannel())
                .cast(GuildMessageChannel.class)
                .flatMap(channel -> DiscordUtil.requirePermissions(channel,
                        Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY)
                        .thenMany(Flux.defer(() -> {
                            if (limitOpt.isPresent() && NumberUtil.toPositiveIntOrNull(limitOpt.orElseThrow()) == null) {
                                return Flux.error(new CommandException(context.localize("prune.invalid.limit")
                                        .formatted(limitOpt.orElseThrow())));
                            }

                            // The count is incremented by MESSAGES_OFFSET to take into account the command
                            int limit = limitOpt.map(NumberUtil::toPositiveIntOrNull).orElse(MAX_MESSAGES);
                            limit = Math.min(MAX_MESSAGES, limit + MESSAGES_OFFSET);

                            final List<Snowflake> authorIds = Arrays.stream(authors).map(Snowflake::of)
                                    .collect(Collectors.toUnmodifiableList());

                            return channel.getMessagesBefore(Snowflake.of(Instant.now()))
                                    .take(limit)
                                    .filter(message -> authors.length == 0
                                            || message.getAuthor().map(User::getId).map(authorIds::contains).orElse(false))
                                    .filter(message -> words.length == 0
                                            || Arrays.stream(words).anyMatch(word -> message.getContent().contains(word))
                                            || Arrays.stream(words).anyMatch(word -> PruneCmd.getEmbedContent(message).contains(word)));
                        }))
                        .map(Message::getId)
                        .collectList()
                        .flatMap(messageIds -> channel.bulkDelete(Flux.fromIterable(messageIds))
                                .count()
                                .map(messagesNotDeleted -> messageIds.size() - messagesNotDeleted - MESSAGES_OFFSET)))
                .flatMap(messagesDeleted -> context.reply(Emoji.CHECK_MARK, context.localize("prune.messages.deleted")
                        .formatted(messagesDeleted)));
    }

    private static String getEmbedContent(Message message) {
        final StringBuilder strBuilder = new StringBuilder();
        for (final Embed embed : message.getEmbeds()) {
            embed.getTitle().ifPresent(title -> strBuilder.append(title).append('\n'));
            embed.getDescription().ifPresent(desc -> strBuilder.append(desc).append('\n'));
            for (final Field field : embed.getFields()) {
                strBuilder.append(field.getName())
                        .append('\n')
                        .append(field.getValue())
                        .append('\n');
            }
        }
        return strBuilder.toString();
    }

}

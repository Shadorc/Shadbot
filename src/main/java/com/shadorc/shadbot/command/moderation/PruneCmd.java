package com.shadorc.shadbot.command.moderation;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.NumberUtil;
import com.shadorc.shadbot.utils.StringUtil;
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
import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class PruneCmd extends BaseCmd {

    private static final long MIN_MESSAGES = 1;
    private static final long MAX_MESSAGES = 100;
    private static final int MESSAGES_OFFSET = 1;

    public PruneCmd() {
        super(CommandCategory.MODERATION, CommandPermission.ADMIN,
                "prune", "Delete messages (include embeds)");

        this.addOption(option -> option.name("author")
                .description("Author of the messages")
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("words")
                .description("Words contained in the messages, separated by comma")
                .required(false)
                .type(ApplicationCommandOptionType.STRING.getValue()));
        this.addOption(option -> option.name("limit")
                .description("Maximum number of messages to delete (max: %d)".formatted(MAX_MESSAGES))
                .required(false)
                .type(ApplicationCommandOptionType.INTEGER.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("prune.loading"))
                .then(context.getChannel())
                .cast(GuildMessageChannel.class)
                .flatMap(channel -> DiscordUtil.requirePermissions(channel,
                        Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY)
                        .then(PruneCmd.getLimit(context))
                        .flatMapMany(limit -> Flux.defer(() -> {
                            final List<String> words = StringUtil.split(
                                    context.getOptionAsString("words").orElse(""), ",");

                            final Mono<User> getAuthor = context.getOptionAsUser("authors");
                            final Mono<Optional<Snowflake>> getAuthorId = getAuthor
                                    .map(User::getId)
                                    .map(Optional::of)
                                    .defaultIfEmpty(Optional.empty());

                            return getAuthorId.flatMapMany(authorOpt ->
                                    channel.getMessagesBefore(Snowflake.of(Instant.now()))
                                            .take(limit)
                                            .filter(PruneCmd.filterMessage(authorOpt.orElse(null), words)));
                        }))
                        .map(Message::getId)
                        .collectList()
                        .flatMap(messageIds -> channel.bulkDelete(Flux.fromIterable(messageIds))
                                .count()
                                .map(messagesNotDeleted -> Math.max(0, messageIds.size() - messagesNotDeleted - MESSAGES_OFFSET))))
                .flatMap(messagesDeleted -> context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("prune.messages.deleted")
                        .formatted(messagesDeleted)));
    }

    private static Predicate<Message> filterMessage(@Nullable Snowflake authorId, List<String> words) {
        return message -> (authorId == null
                || message.getAuthor().map(User::getId).map(authorId::equals).orElse(false))
                && (words.isEmpty()
                || words.stream().anyMatch(word -> message.getContent().contains(word) || PruneCmd.getEmbedContent(message).contains(word)));
    }

    private static Mono<Long> getLimit(Context context) {
        final Optional<Long> limitOpt = context.getOptionAsLong("limit");
        final long limit = limitOpt.orElse(MAX_MESSAGES);
        if (!NumberUtil.isBetween(limit, MIN_MESSAGES, MAX_MESSAGES)) {
            return Mono.error(new CommandException(context.localize("prune.limit.out.of.range")
                    .formatted(MIN_MESSAGES, MAX_MESSAGES)));
        }

        // The count is incremented by MESSAGES_OFFSET to take into account the command
        return Mono.just(Math.min(MAX_MESSAGES, limit + MESSAGES_OFFSET));
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

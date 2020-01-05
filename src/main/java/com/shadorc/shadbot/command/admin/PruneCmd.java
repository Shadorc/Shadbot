package com.shadorc.shadbot.command.admin;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PruneCmd extends BaseCmd {

    private static final int MAX_MESSAGES = 100;

    public PruneCmd() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of("prune"));
        this.setRateLimiter(new RateLimiter(2, Duration.ofSeconds(3)));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading messages to prune...", context.getUsername()))
                .send()
                .then(context.getChannel())
                .flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY)
                        .then(context.getMessage().getUserMentions().collectList())
                        .flatMapMany(mentions -> {
                            final String arg = context.getArg().orElse("");
                            final List<String> quotedElements = StringUtils.getQuotedElements(arg);

                            if (arg.contains("\"") && quotedElements.isEmpty() || quotedElements.size() > 1) {
                                return Flux.error(new CommandException("You have forgotten a quote or have specified several quotes in quotation marks."));
                            }

                            final String words = quotedElements.isEmpty() ? null : quotedElements.get(0);

                            // Remove everything from argument (users mentioned and quoted words) to keep only count if specified
                            final String argCleaned = StringUtils.remove(arg,
                                    FormatUtils.format(mentions, User::getMention, " "),
                                    String.format("\"%s\"", words))
                                    .trim();

                            Integer count = NumberUtils.toPositiveIntOrNull(argCleaned);
                            if (!argCleaned.isEmpty() && count == null) {
                                return Flux.error(new CommandException(String.format("`%s` is not a valid number. If you want to specify a word or a sentence, "
                                                + "please include them in quotation marks. See `%shelp %s` for more information.",
                                        argCleaned, context.getPrefix(), this.getName())));
                            }

                            count = count == null ? MAX_MESSAGES : Math.min(MAX_MESSAGES, count);

                            final List<Snowflake> mentionIds = mentions.stream().map(User::getId).collect(Collectors.toList());

                            return channel.getMessagesBefore(Snowflake.of(Instant.now()))
                                    .take(count)
                                    .filter(message -> mentions.isEmpty()
                                            || message.getAuthor().map(User::getId).map(mentionIds::contains).orElse(false))
                                    .filter(message -> words == null
                                            || message.getContent().map(content -> content.contains(words)).orElse(false)
                                            || this.getEmbedContent(message).contains(words));
                        })
                        .map(Message::getId)
                        .collectList()
                        .flatMap(messageIds -> ((GuildMessageChannel) channel).bulkDelete(Flux.fromIterable(messageIds))
                                .count()
                                .map(messagesNotDeleted -> messageIds.size() - messagesNotDeleted))
                        .map(deletedMessages -> String.format(Emoji.CHECK_MARK + " (Requested by **%s**) %s deleted.",
                                context.getUsername(), StringUtils.pluralOf(deletedMessages, "message"))))
                .map(updatableMsg::setContent)
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private String getEmbedContent(Message message) {
        final StringBuilder strBuilder = new StringBuilder();
        for (final Embed embed : message.getEmbeds()) {
            embed.getTitle().ifPresent(title -> strBuilder.append(title + "\n"));
            embed.getDescription().ifPresent(desc -> strBuilder.append(desc + "\n"));
            for (final Field field : embed.getFields()) {
                strBuilder.append(field.getName() + "\n" + field.getValue() + "\n");
            }
        }
        return strBuilder.toString();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Delete messages (include embeds).")
                .addArg("@user(s)", "from these users", true)
                .addArg("\"words\"", "containing these words", true)
                .addArg("number", String.format("number of messages to delete (max: %d)", MAX_MESSAGES), true)
                .setExample(String.format("Delete **15** messages from user **@Shadbot** containing **hi guys**:"
                        + "%n`%s%s @Shadbot \"hi guys\" 15`", context.getPrefix(), this.getName()))
                .addField("Info", "Messages older than 2 weeks cannot be deleted.", false)
                .build();
    }

}

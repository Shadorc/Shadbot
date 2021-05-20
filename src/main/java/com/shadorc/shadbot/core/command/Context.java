package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.i18n.I18nContext;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.database.guilds.entity.DBGuild;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.music.NoMusicException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.EnumUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableWebhookMessageEditRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.MultipartRequest;
import discord4j.rest.util.Permission;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class Context implements InteractionContext, I18nContext {

    private final InteractionCreateEvent event;
    private final DBGuild dbGuild;
    private final AtomicLong replyId;

    public Context(InteractionCreateEvent event, DBGuild dbGuild) {
        this.event = event;
        this.dbGuild = dbGuild;
        this.replyId = new AtomicLong();
    }

    public InteractionCreateEvent getEvent() {
        return this.event;
    }

    public DBGuild getDbGuild() {
        return this.dbGuild;
    }

    public GatewayDiscordClient getClient() {
        return this.event.getClient();
    }

    public String getFullCommandName() {
        final List<String> cmds = new ArrayList<>();
        cmds.add(this.getCommandName());
        this.getSubCommandGroupName().ifPresent(cmds::add);
        this.getSubCommandName().ifPresent(cmds::add);
        return String.join(" ", cmds);
    }

    public Optional<String> getSubCommandGroupName() {
        return DiscordUtil.flattenOptions(this.event.getInteraction().getCommandInteraction())
                .stream()
                .filter(option -> option.getType() == ApplicationCommandOptionType.SUB_COMMAND_GROUP)
                .map(ApplicationCommandInteractionOption::getName)
                .findFirst();
    }

    public Optional<String> getSubCommandName() {
        return DiscordUtil.flattenOptions(this.event.getInteraction().getCommandInteraction())
                .stream()
                .filter(option -> option.getType() == ApplicationCommandOptionType.SUB_COMMAND)
                .map(ApplicationCommandInteractionOption::getName)
                .findFirst();
    }

    public String getCommandName() {
        return this.event.getCommandName();
    }

    public String getLastCommandName() {
        return this.getSubCommandGroupName()
                .orElse(this.getSubCommandName()
                        .orElse(this.getCommandName()));
    }

    public Mono<Guild> getGuild() {
        return this.getEvent().getInteraction().getGuild();
    }

    public Snowflake getGuildId() {
        return this.getEvent().getInteraction().getGuildId().orElseThrow();
    }

    public Mono<TextChannel> getChannel() {
        return this.getEvent().getInteraction().getChannel().cast(TextChannel.class);
    }

    public Snowflake getChannelId() {
        return this.getEvent().getInteraction().getChannelId();
    }

    public Mono<Boolean> isChannelNsfw() {
        return this.getChannel().map(TextChannel::isNsfw);
    }

    public Member getAuthor() {
        return this.getEvent().getInteraction().getMember().orElseThrow();
    }

    public Snowflake getAuthorId() {
        return this.getEvent().getInteraction().getUser().getId();
    }

    public String getAuthorName() {
        return this.getAuthor().getUsername();
    }

    public String getAuthorAvatar() {
        return this.getAuthor().getAvatarUrl();
    }

    public Optional<ApplicationCommandInteractionOptionValue> getOption(String name) {
        final List<ApplicationCommandInteractionOption> options =
                DiscordUtil.flattenOptions(this.getEvent().getInteraction().getCommandInteraction());
        return options.stream()
                .filter(option -> option.getName().equals(name))
                .filter(option -> option.getValue().isPresent())
                .findFirst()
                .flatMap(ApplicationCommandInteractionOption::getValue);
    }

    public <T extends Enum<T>> Optional<T> getOptionAsEnum(Class<T> enumClass, String name) {
        return this.getOptionAsString(name).map(it -> EnumUtil.parseEnum(enumClass, it));
    }

    public Optional<String> getOptionAsString(String name) {
        return this.getOption(name).map(ApplicationCommandInteractionOptionValue::asString);
    }

    public Optional<Snowflake> getOptionAsSnowflake(String name) {
        return this.getOption(name).map(ApplicationCommandInteractionOptionValue::asSnowflake);
    }

    public Optional<Long> getOptionAsLong(String name) {
        return this.getOption(name).map(ApplicationCommandInteractionOptionValue::asLong);
    }

    public Optional<Boolean> getOptionAsBool(String name) {
        return this.getOption(name).map(ApplicationCommandInteractionOptionValue::asBoolean);
    }

    public Mono<User> getOptionAsUser(String name) {
        return Mono.justOrEmpty(this.getOption(name))
                .flatMap(ApplicationCommandInteractionOptionValue::asUser);
    }

    public Mono<Member> getOptionAsMember(String name) {
        return Mono.justOrEmpty(this.getOption(name))
                .flatMap(ApplicationCommandInteractionOptionValue::asUser)
                .flatMap(user -> user.asMember(this.getGuildId()));
    }

    public Mono<Role> getOptionAsRole(String name) {
        return Mono.justOrEmpty(this.getOption(name)).flatMap(ApplicationCommandInteractionOptionValue::asRole);
    }

    public Mono<Channel> getOptionAsChannel(String name) {
        return Mono.justOrEmpty(this.getOption(name)).flatMap(ApplicationCommandInteractionOptionValue::asChannel);
    }

    public Flux<CommandPermission> getPermissions() {
        // The author is a bot's owner
        final Mono<CommandPermission> ownerPerm = Mono.just(this.getAuthorId())
                .filter(Shadbot.getOwnerId()::equals)
                .map(__ -> CommandPermission.OWNER);

        // The member is an administrator or it's a private message
        final Mono<CommandPermission> adminPerm = this.getChannel()
                .filterWhen(channel -> BooleanUtils.or(
                        DiscordUtil.hasPermission(channel, this.getAuthorId(), Permission.ADMINISTRATOR),
                        Mono.just(channel.getType() == Channel.Type.DM)))
                .map(__ -> CommandPermission.ADMIN);

        return Flux.merge(ownerPerm, adminPerm, Mono.just(CommandPermission.USER));
    }

    public GuildMusic requireGuildMusic() {
        return MusicManager.getGuildMusic(this.getGuildId())
                .filter(guildMusic -> !guildMusic.getTrackScheduler().isStopped())
                .orElseThrow(NoMusicException::new);
    }

    /////////////////////////////////////////////
    ///////////// InteractionContext
    /////////////////////////////////////////////

    @Override
    public Locale getLocale() {
        return this.getDbGuild().getLocale();
    }

    @Override
    public String localize(String key) {
        return I18nManager.localize(this.getLocale(), key);
    }

    @Override
    public String localize(double number) {
        return I18nManager.localize(this.getLocale(), number);
    }

    /////////////////////////////////////////////
    ///////////// InteractionContext
    /////////////////////////////////////////////

    @Override
    public Mono<Void> replyEphemeral(Emoji emoji, String message) {
        return this.event.replyEphemeral("%s %s".formatted(emoji, message))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
    }

    @Override
    public Mono<Message> createFollowupMessage(String str) {
        return this.event.getInteractionResponse()
                .createFollowupMessage(str)
                .map(data -> new Message(this.getClient(), data))
                .doOnNext(message -> this.replyId.set(message.getId().asLong()))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
    }

    @Override
    public Mono<Message> createFollowupMessage(Emoji emoji, String str) {
        return this.createFollowupMessage("%s %s".formatted(emoji, str));
    }

    @Override
    public Mono<Message> createFollowupMessage(Consumer<EmbedCreateSpec> embed) {
        final EmbedCreateSpec mutatedSpec = new EmbedCreateSpec();
        embed.accept(mutatedSpec);
        return this.event.getInteractionResponse().createFollowupMessage(MultipartRequest.ofRequest(
                WebhookExecuteRequest.builder()
                        .addEmbed(mutatedSpec.asRequest())
                        .build()))
                .map(data -> new Message(this.getClient(), data))
                .doOnNext(message -> this.replyId.set(message.getId().asLong()))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
    }

    @Override
    public Mono<Message> editFollowupMessage(String message) {
        return Mono.fromCallable(this.replyId::get)
                .filter(messageId -> messageId > 0)
                .flatMap(messageId -> this.event.getInteractionResponse()
                        .editFollowupMessage(messageId, ImmutableWebhookMessageEditRequest.builder()
                                .content(message)
                                .build(), true))
                .map(data -> new Message(this.getClient(), data))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc())
                .switchIfEmpty(this.createFollowupMessage(message));
    }

    @Override
    public Mono<Message> editFollowupMessage(Emoji emoji, String message) {
        return this.editFollowupMessage("%s %s".formatted(emoji, message));
    }

    @Override
    public Mono<Message> editFollowupMessage(Consumer<EmbedCreateSpec> embed) {
        return Mono.fromCallable(this.replyId::get)
                .filter(messageId -> messageId > 0)
                .flatMap(messageId -> {
                    final EmbedCreateSpec mutatedSpec = new EmbedCreateSpec();
                    embed.accept(mutatedSpec);
                    return this.event.getInteractionResponse()
                            .editFollowupMessage(messageId, ImmutableWebhookMessageEditRequest.builder()
                                    .content("")
                                    .embeds(List.of(mutatedSpec.asRequest()))
                                    .build(), true);
                })
                .map(data -> new Message(this.getClient(), data))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc())
                .switchIfEmpty(this.createFollowupMessage(embed));
    }

    @Override
    public Mono<Message> editInitialFollowupMessage(Consumer<EmbedCreateSpec> embed) {
        return Mono.defer(() -> {
            final EmbedCreateSpec mutatedSpec = new EmbedCreateSpec();
            embed.accept(mutatedSpec);
            return this.event.getInteractionResponse()
                    .editInitialResponse(ImmutableWebhookMessageEditRequest.builder()
                            .content("")
                            .embeds(List.of(mutatedSpec.asRequest()))
                            .build())
                    .map(data -> new Message(this.getClient(), data))
                    .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc())
                    .switchIfEmpty(this.createFollowupMessage(embed));
        });
    }

}

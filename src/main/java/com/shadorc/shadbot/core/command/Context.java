package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.music.NoMusicException;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableWebhookMessageEditRequest;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.Permission;
import discord4j.rest.util.WebhookMultipartRequest;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class Context {

    private final InteractionCreateEvent event;
    private final DBGuild dbGuild;

    public Context(InteractionCreateEvent event, DBGuild dbGuild) {
        this.event = event;
        this.dbGuild = dbGuild;
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

    public String getCommandName() {
        return this.event.getCommandName();
    }

    public Snowflake getGuildId() {
        return this.event.getGuildId().orElseThrow();
    }

    public Snowflake getChannelId() {
        return this.event.getChannelId();
    }

    public Member getAuthor() {
        return this.event.getMember().orElseThrow();
    }

    public Snowflake getAuthorId() {
        return this.event.getUser().getId();
    }

    public String getAuthorName() {
        return this.getAuthor().getUsername();
    }

    public String getAuthorAvatarUrl() {
        return this.getAuthor().getAvatarUrl();
    }

    public Mono<Guild> getGuild() {
        return this.event.getGuild();
    }

    public Mono<TextChannel> getChannel() {
        return this.event.getChannel();
    }

    public Optional<ApplicationCommandInteractionOptionValue> getOption(String name) {
        return this.event.getCommandInteraction().getOption(name).flatMap(ApplicationCommandInteractionOption::getValue);
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

    public Mono<Member> getOptionAsMember(String name) {
        return Mono.justOrEmpty(this.getOption(name)).flatMap(ApplicationCommandInteractionOptionValue::asUser)
                .flatMap(user -> user.asMember(getGuildId()));
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

    public int getShardCount() {
        return this.event.getShardInfo().getCount();
    }

    public int getShardIndex() {
        return this.event.getShardInfo().getIndex();
    }

    public Mono<Boolean> isChannelNsfw() {
        return this.getChannel().map(TextChannel::isNsfw);
    }

    public Mono<Snowflake> createFollowupMessage(String format, Object... args) {
        return this.event.getInteractionResponse().createFollowupMessage(String.format(format, args))
                .map(MessageData::id)
                .map(Snowflake::of);
    }

    public Mono<Snowflake> createFollowupMessage(Consumer<EmbedCreateSpec> embed) {
        final EmbedCreateSpec mutatedSpec = new EmbedCreateSpec();
        embed.accept(mutatedSpec);
        return this.event.getInteractionResponse().createFollowupMessage(new WebhookMultipartRequest(
                WebhookExecuteRequest.builder()
                        .addEmbed(mutatedSpec.asRequest())
                        .build()), true)
                .map(MessageData::id)
                .map(Snowflake::of);
    }

    public Mono<MessageData> editFollowupMessage(Snowflake messageId, String format, Object... args) {
        return this.editFollowupMessage(messageId, String.format(format, args));
    }

    public Mono<MessageData> editFollowupMessage(Snowflake messageId, String content) {
        return this.event.getInteractionResponse()
                .editFollowupMessage(messageId.asLong(), ImmutableWebhookMessageEditRequest.builder()
                        .content(content)
                        .build(), true);
    }

    public Mono<MessageData> editFollowupMessage(Snowflake messageId, Consumer<EmbedCreateSpec> embed) {
        final EmbedCreateSpec mutatedSpec = new EmbedCreateSpec();
        embed.accept(mutatedSpec);
        return this.event.getInteractionResponse()
                .editFollowupMessage(messageId.asLong(), ImmutableWebhookMessageEditRequest.builder()
                        .content("Done!") // TODO: Remove content
                        .embeds(List.of(mutatedSpec.asRequest()))
                        .build(), true);
    }

    public GuildMusic requireGuildMusic() {
        return MusicManager.getInstance()
                .getGuildMusic(this.getGuildId())
                .filter(guildMusic -> !guildMusic.getTrackScheduler().isStopped())
                .orElseThrow(NoMusicException::new);
    }

}

package com.shadorc.shadbot.core.retriever;

import com.shadorc.shadbot.data.Telemetry;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.retriever.RestEntityRetriever;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SpyRestEntityRetriever extends RestEntityRetriever {

    public SpyRestEntityRetriever(GatewayDiscordClient gateway) {
        super(gateway);
    }

    @Override
    public Mono<Channel> getChannelById(Snowflake channelId) {
        return super.getChannelById(channelId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Mono<Guild> getGuildById(Snowflake guildId) {
        return super.getGuildById(guildId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Mono<GuildEmoji> getGuildEmojiById(Snowflake guildId, Snowflake emojiId) {
        return super.getGuildEmojiById(guildId, emojiId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Mono<Member> getMemberById(Snowflake guildId, Snowflake userId) {
        return super.getMemberById(guildId, userId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Mono<Message> getMessageById(Snowflake channelId, Snowflake messageId) {
        return super.getMessageById(channelId, messageId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Mono<Role> getRoleById(Snowflake guildId, Snowflake roleId) {
        return super.getRoleById(guildId, roleId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Mono<User> getUserById(Snowflake userId) {
        return super.getUserById(userId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Flux<Guild> getGuilds() {
        return super.getGuilds()
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Mono<User> getSelf() {
        return super.getSelf()
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Flux<Member> getGuildMembers(Snowflake guildId) {
        return super.getGuildMembers(guildId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Flux<GuildChannel> getGuildChannels(Snowflake guildId) {
        return super.getGuildChannels(guildId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Flux<Role> getGuildRoles(Snowflake guildId) {
        return super.getGuildRoles(guildId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

    @Override
    public Flux<GuildEmoji> getGuildEmojis(Snowflake guildId) {
        return super.getGuildEmojis(guildId)
                .doOnTerminate(Telemetry.REST_REQUEST_COUNTER::inc);
    }

}

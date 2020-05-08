package com.shadorc.shadbot.core.retriever;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.retriever.RestEntityRetriever;
import discord4j.rest.util.Snowflake;
import io.prometheus.client.Counter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SpyRestEntityRetriever extends RestEntityRetriever {

    private static final Counter REST_REQUEST_COUNTER = Counter.build().namespace("shard")
            .name("rest_request").help("Rest request count").register();

    public SpyRestEntityRetriever(GatewayDiscordClient gateway) {
        super(gateway);
    }

    public Mono<Channel> getChannelById(Snowflake channelId) {
        return super.getChannelById(channelId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Mono<Guild> getGuildById(Snowflake guildId) {
        return super.getGuildById(guildId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Mono<GuildEmoji> getGuildEmojiById(Snowflake guildId, Snowflake emojiId) {
        return super.getGuildEmojiById(guildId, emojiId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Mono<Member> getMemberById(Snowflake guildId, Snowflake userId) {
        return super.getMemberById(guildId, userId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Mono<Message> getMessageById(Snowflake channelId, Snowflake messageId) {
        return super.getMessageById(channelId, messageId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Mono<Role> getRoleById(Snowflake guildId, Snowflake roleId) {
        return super.getRoleById(guildId, roleId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Mono<User> getUserById(Snowflake userId) {
        return super.getUserById(userId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Flux<Guild> getGuilds() {
        return super.getGuilds()
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Mono<User> getSelf() {
        return super.getSelf()
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Flux<Member> getGuildMembers(Snowflake guildId) {
        return super.getGuildMembers(guildId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Flux<GuildChannel> getGuildChannels(Snowflake guildId) {
        return super.getGuildChannels(guildId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Flux<Role> getGuildRoles(Snowflake guildId) {
        return super.getGuildRoles(guildId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

    public Flux<GuildEmoji> getGuildEmojis(Snowflake guildId) {
        return super.getGuildEmojis(guildId)
                .doOnTerminate(REST_REQUEST_COUNTER::inc);
    }

}

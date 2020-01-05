package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.exception.MissingArgumentException;
import com.shadorc.shadbot.exception.NoMusicException;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Optional;

public class Context {

    private final MessageCreateEvent event;
    private final String prefix;
    private final String cmdName;
    @Nullable
    private final String arg;

    public Context(MessageCreateEvent event, String prefix) {
        this.event = event;
        this.prefix = prefix;

        final List<String> splittedMsg = StringUtils.split(this.getContent(), 2);
        this.cmdName = splittedMsg.get(0).substring(prefix.length()).toLowerCase();
        this.arg = splittedMsg.size() > 1 ? splittedMsg.get(1).trim() : null;
    }

    public MessageCreateEvent getEvent() {
        return this.event;
    }

    public Optional<String> getArg() {
        return Optional.ofNullable(this.arg);
    }

    public User getAuthor() {
        return this.getMessage().getAuthor().orElseThrow();
    }

    public Snowflake getAuthorId() {
        return this.getAuthor().getId();
    }

    public String getAvatarUrl() {
        return this.getAuthor().getAvatarUrl();
    }

    public Mono<MessageChannel> getChannel() {
        return this.getMessage().getChannel();
    }

    public Snowflake getChannelId() {
        return this.getMessage().getChannelId();
    }

    public GatewayDiscordClient getClient() {
        return this.event.getClient();
    }

    public String getCommandName() {
        return this.cmdName;
    }

    public String getContent() {
        return this.getMessage().getContent().orElseThrow();
    }

    public Mono<Guild> getGuild() {
        return this.event.getGuild();
    }

    public Snowflake getGuildId() {
        return this.event.getGuildId().orElseThrow();
    }

    public Member getMember() {
        return this.event.getMember().orElseThrow();
    }

    public Message getMessage() {
        return this.event.getMessage();
    }

    public Flux<CommandPermission> getPermissions() {
        // The author is a bot's owner
        final Mono<CommandPermission> ownerPerm = Mono.just(this.getAuthorId())
                .filter(Shadbot.getOwnerId()::equals)
                .map(ignored -> CommandPermission.OWNER);

        // The member is an administrator or it's a private message
        final Mono<CommandPermission> adminPerm = this.getChannel()
                .filterWhen(channel -> DiscordUtils.hasPermission(channel, this.getAuthorId(), Permission.ADMINISTRATOR)
                        .map(isAdmin -> isAdmin || channel.getType() == Channel.Type.DM))
                .map(ignored -> CommandPermission.ADMIN);

        return Flux.merge(ownerPerm, adminPerm, Mono.just(CommandPermission.USER));
    }

    public String getPrefix() {
        return this.prefix;
    }

    public Mono<User> getSelf() {
        return this.getClient().getSelf();
    }

    public Snowflake getSelfId() {
        return Shadbot.getSelfId();
    }

    public Mono<Member> getSelfAsMember() {
        return this.getSelf().flatMap(self -> self.asMember(this.getGuildId()));
    }

    public int getShardCount() {
        return this.getEvent().getShardInfo().getCount();
    }

    public int getShardIndex() {
        return this.getEvent().getShardInfo().getIndex();
    }

    public String getUsername() {
        return this.getAuthor().getUsername();
    }

    public Mono<Boolean> isChannelNsfw() {
        return this.getChannel()
                .ofType(TextChannel.class)
                .map(TextChannel::isNsfw);
    }

    public String requireArg() {
        return this.getArg()
                .map(StringUtils::normalizeSpace)
                .orElseThrow(MissingArgumentException::new);
    }

    public List<String> requireArgs(int count) {
        return this.requireArgs(count, count);
    }

    public List<String> requireArgs(int min, int max) {
        return this.requireArgs(min, max, Config.COMMAND_DELIMITER);
    }

    public List<String> requireArgs(int min, int max, String delimiter) {
        final List<String> args = StringUtils.split(this.requireArg(), max, delimiter);
        if (!NumberUtils.isBetween(args.size(), min, max)) {
            throw new MissingArgumentException();
        }
        return args;
    }

    public GuildMusic requireGuildMusic() {
        final GuildMusic guildMusic = MusicManager.getInstance().getMusic(this.getGuildId());
        if (guildMusic == null || guildMusic.getTrackScheduler().isStopped()) {
            throw new NoMusicException();
        }
        return guildMusic;
    }

}

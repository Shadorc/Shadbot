package me.shadorc.shadbot.core.command;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.NoMusicException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import reactor.core.publisher.Mono;

public class Context {

	private final Optional<String> arg;
	private final String cmdName;
	private final MessageCreateEvent event;
	private final String prefix;

	public Context(MessageCreateEvent event, String prefix) {
		this.event = event;
		this.prefix = prefix;

		final List<String> splittedMsg = StringUtils.split(this.getContent(), 2);
		this.cmdName = splittedMsg.get(0).substring(prefix.length()).toLowerCase();
		this.arg = Optional.ofNullable(splittedMsg.size() > 1 ? splittedMsg.get(1).trim() : null);
	}

	public Optional<String> getArg() {
		return this.arg;
	}

	public Mono<User> getAuthor() {
		return this.getMessage().getAuthor();
	}

	@Nullable
	public Snowflake getAuthorId() {
		return this.getMessage().getAuthorId().orElse(null);
	}

	public Mono<String> getAvatarUrl() {
		return this.getAuthor().map(User::getAvatarUrl);
	}

	public Mono<MessageChannel> getChannel() {
		return this.getMessage().getChannel();
	}

	public Snowflake getChannelId() {
		return this.getMessage().getChannelId();
	}

	public DiscordClient getClient() {
		return this.event.getClient();
	}

	public String getCommandName() {
		return this.cmdName;
	}

	@Nullable
	public String getContent() {
		return this.getMessage().getContent().orElse(null);
	}

	public Mono<Guild> getGuild() {
		return this.event.getGuild();
	}

	@Nullable
	public Snowflake getGuildId() {
		return this.event.getGuildId().orElse(null);
	}

	@Nullable
	public Member getMember() {
		return this.event.getMember().orElse(null);
	}

	public Message getMessage() {
		return this.event.getMessage();
	}

	public Mono<CommandPermission> getPermission() {
		// The author is the bot's owner
		final Mono<CommandPermission> ownerPerm = this.getClient().getApplicationInfo()
				.map(ApplicationInfo::getOwnerId)
				.filter(this.getAuthorId()::equals)
				.map(ignored -> CommandPermission.OWNER);

		// Private message, the author is considered as an administrator
		final Mono<CommandPermission> dmPerm = Mono.just(this.event.getGuildId())
				.filter(guildId -> !guildId.isPresent())
				.map(ignored -> CommandPermission.ADMIN);

		// The member is an administrator
		final Mono<CommandPermission> adminPerm = this.getChannel()
				.filterWhen(channel -> DiscordUtils.hasPermission(channel, this.getAuthorId(), Permission.ADMINISTRATOR))
				.map(ignored -> CommandPermission.ADMIN);

		return ownerPerm
				.switchIfEmpty(dmPerm)
				.switchIfEmpty(adminPerm)
				.defaultIfEmpty(CommandPermission.USER);
	}

	public String getPrefix() {
		return this.prefix;
	}

	public Mono<User> getSelf() {
		return this.getClient().getSelf();
	}

	public Mono<Member> getSelfAsMember() {
		return this.getSelf().flatMap(self -> self.asMember(this.getGuildId()));
	}

	@Nullable
	public Snowflake getSelfId() {
		return this.getClient().getSelfId().orElse(null);
	}

	public int getShardCount() {
		return this.getClient().getConfig().getShardCount();
	}

	public int getShardIndex() {
		return this.getClient().getConfig().getShardIndex();
	}

	public String getUsername() {
		return this.getMember().getUsername();
	}

	public Mono<Boolean> isChannelNsfw() {
		return this.getChannel()
				.ofType(TextChannel.class)
				.map(TextChannel::isNsfw);
	}

	public boolean isDm() {
		return !this.event.getGuildId().isPresent();
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
		return this.requireArgs(min, max, Config.DEFAULT_COMMAND_DELIMITER);
	}

	public List<String> requireArgs(int min, int max, String delimiter) {
		final List<String> args = StringUtils.split(this.requireArg(), max, delimiter);
		if(!NumberUtils.isInRange(args.size(), min, max)) {
			throw new MissingArgumentException();
		}
		return args;
	}

	public List<String> requireArgs(int count, String delimiter) {
		return this.requireArgs(count, count, delimiter);
	}

	public GuildMusic requireGuildMusic() {
		final GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(this.getGuildId());
		if(guildMusic == null || guildMusic.getTrackScheduler().isStopped()) {
			throw new NoMusicException();
		}
		return guildMusic;
	}

}

package me.shadorc.shadbot.core.command;

import java.util.List;
import java.util.Optional;

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
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.NoMusicException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import reactor.core.publisher.Mono;

public class Context {

	private final MessageCreateEvent event;
	private final String prefix;
	private final String cmdName;
	private final Optional<String> arg;

	// Note: The message is considered to be from a member, so all optional values should be accessible
	public Context(MessageCreateEvent event, String prefix) {
		this.event = event;
		this.prefix = prefix;

		List<String> splittedMsg = StringUtils.split(this.getContent(), 2);
		this.cmdName = splittedMsg.get(0).substring(prefix.length()).toLowerCase();
		this.arg = Optional.ofNullable(splittedMsg.size() > 1 ? splittedMsg.get(1).trim() : null);
	}

	public MessageCreateEvent getEvent() {
		return event;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getCommandName() {
		return cmdName;
	}

	// TODO: Comment this to see everywhere it's used to check that it use correctly optional
	public Optional<String> getArg() {
		return arg;
	}

	public DiscordClient getClient() {
		return event.getClient();
	}

	public Mono<Guild> getGuild() {
		return event.getGuild();
	}

	public Snowflake getGuildId() {
		return event.getGuildId().get();
	}

	public Member getMember() {
		return event.getMember().get();
	}

	public Message getMessage() {
		return event.getMessage();
	}

	public int getShardIndex() {
		return this.getClient().getConfig().getShardIndex();
	}

	public int getShardCount() {
		return this.getClient().getConfig().getShardCount();
	}

	public Mono<User> getSelf() {
		return this.getClient().getSelf();
	}

	public Mono<Member> getSelfAsMember() {
		return this.getSelf().flatMap(self -> self.asMember(this.getGuildId()));
	}

	public Snowflake getSelfId() {
		return this.getClient().getSelfId().get();
	}

	public String getContent() {
		return this.getMessage().getContent().get();
	}

	public Mono<MessageChannel> getChannel() {
		return this.getMessage().getChannel();
	}

	public Snowflake getChannelId() {
		return this.getMessage().getChannelId();
	}

	public Mono<User> getAuthor() {
		return this.getMessage().getAuthor();
	}

	public Snowflake getAuthorId() {
		return this.getMessage().getAuthorId().get();
	}

	public String getUsername() {
		return this.getMember().getUsername();
	}

	public Mono<String> getAvatarUrl() {
		return DiscordUtils.getAuthorAvatarUrl(this.getAuthor());
	}

	public Mono<CommandPermission> getPermission() {
		// The author is the bot's owner
		Mono<CommandPermission> ownerPerm = this.getClient().getApplicationInfo()
				.map(ApplicationInfo::getOwnerId)
				.filter(this.getAuthorId()::equals)
				.map(bool -> CommandPermission.OWNER);

		// Private message, the author is considered as an administrator
		Mono<CommandPermission> dmPerm = Mono.just(event.getGuildId())
				.filter(guildId -> !guildId.isPresent())
				.map(guildId -> CommandPermission.ADMIN);

		// The member is an administrator
		Mono<CommandPermission> adminPerm = DiscordUtils.hasPermissions(this.getAuthor(), this.getGuildId(), Permission.ADMINISTRATOR)
				.map(bool -> CommandPermission.ADMIN);

		return ownerPerm
				.switchIfEmpty(dmPerm)
				.switchIfEmpty(adminPerm)
				.defaultIfEmpty(CommandPermission.USER);
	}

	public Mono<Boolean> isChannelNsfw() {
		return this.getChannel()
				.map(TextChannel.class::cast)
				.map(TextChannel::isNsfw);
	}

	public String requireArg() {
		return this.getArg()
				.map(StringUtils::normalizeSpace)
				.orElseThrow(() -> new MissingArgumentException());
	}

	public List<String> requireArgs(int count) {
		return this.requireArgs(count, count);
	}

	public List<String> requireArgs(int min, int max) {
		List<String> args = StringUtils.split(this.requireArg(), max);
		if(!NumberUtils.isInRange(args.size(), min, max)) {
			throw new MissingArgumentException();
		}
		return args;
	}

	public GuildMusic requireGuildMusic() {
		final GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(this.getGuildId());
		if(guildMusic == null || guildMusic.getScheduler().isStopped()) {
			throw new NoMusicException();
		}
		return guildMusic;
	}

}

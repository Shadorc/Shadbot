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
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Image.Format;
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

	private final MessageCreateEvent event; // The event is stored because it does not need to be reactive
	private final String prefix;
	private final String cmdName;
	private final Optional<String> arg;

	public Context(MessageCreateEvent event, String prefix) {
		this.event = event;
		this.prefix = prefix;

		List<String> splittedMsg = StringUtils.split(this.getContent(), 2);
		this.cmdName = splittedMsg.get(0).substring(prefix.length()).toLowerCase();
		this.arg = Optional.ofNullable(splittedMsg.size() > 1 ? splittedMsg.get(1) : null);
	}

	public String getPrefix() {
		return prefix;
	}

	public String getCommandName() {
		return cmdName;
	}

	public Optional<String> getArg() {
		return arg;
	}

	public DiscordClient getClient() {
		return event.getClient();
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

	public Snowflake getSelfId() {
		return this.getClient().getSelfId().get();
	}

	public Message getMessage() {
		return event.getMessage();
	}

	public String getContent() {
		return this.getMessage().getContent().get();
	}

	public Mono<Guild> getGuild() {
		return event.getGuild();
	}

	public Optional<Snowflake> getGuildId() {
		return event.getGuildId();
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

	public Mono<String> getAuthorName() {
		return this.getAuthor().map(User::getUsername);
	}

	public Mono<String> getAuthorAvatarUrl() {
		return this.getAuthor()
				.map(user -> user.getAvatar(Format.JPEG))
				.map(image -> image.map(Image::getUrl))
				.map(url -> url.orElse("https://avatars0.githubusercontent.com/u/6373756?s=460&v=4"));

	}

	public Optional<Member> getMember() {
		return event.getMember();
	}

	public Mono<CommandPermission> getAuthorPermission() {
		// The author is the bot's owner
		Mono<CommandPermission> ownerPerm = this.getClient().getApplicationInfo()
				.map(ApplicationInfo::getOwnerId)
				.filter(this.getAuthorId()::equals)
				.map(bool -> {
					return CommandPermission.OWNER;
				});

		// Private message, the author is considered as an administrator
		Mono<CommandPermission> dmPerm = Mono.just(this.getGuildId())
				.filter(guildId -> !guildId.isPresent())
				.map(guildId -> {
					return CommandPermission.ADMIN;
				});

		// The member is an administrator
		Mono<CommandPermission> adminPerm = DiscordUtils.hasPermissions(this.getAuthor(), this.getGuildId().get(), Permission.ADMINISTRATOR)
				.map(bool -> {
					return CommandPermission.ADMIN;
				});

		return ownerPerm
				.switchIfEmpty(dmPerm)
				.switchIfEmpty(adminPerm)
				.defaultIfEmpty(CommandPermission.USER);
	}

	public Mono<Boolean> isChannelNsfw() {
		return this.getChannel().map(TextChannel.class::cast).map(TextChannel::isNsfw);
	}

	public String requireArg() {
		if(!this.getArg().isPresent()) {
			throw new MissingArgumentException();
		}
		return this.getArg().get();
	}

	public List<String> requireArgs(int count) {
		return this.requireArgs(count, count);
	}

	public List<String> requireArgs(int min, int max) {
		this.requireArg();
		List<String> args = StringUtils.split(this.getArg().get(), max);
		if(!NumberUtils.isInRange(args.size(), min, max)) {
			throw new MissingArgumentException();
		}
		return args;
	}

	public GuildMusic requireGuildMusic() {
		GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(this.getGuildId().get());
		if(guildMusic == null || guildMusic.getScheduler().isStopped()) {
			throw new NoMusicException();
		}
		return guildMusic;
	}

}

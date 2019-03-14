package me.shadorc.shadbot.command.music;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.listener.music.AudioLoadResultListener;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

public class PlayCmd extends BaseCmd {

	public PlayCmd() {
		super(CommandCategory.MUSIC, List.of("play", "add", "queue", "playfirst", "addfirst", "queuefirst"));
		this.setDefaultRateLimiter();
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();
		final Snowflake guildId = context.getGuildId();

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.CONNECT, Permission.SPEAK)
						.then(DiscordUtils.requireSameVoiceChannel(context))
						.flatMap(voiceChannelId -> {
							String identifier;
							// If this is a SoundCloud search...
							if(arg.startsWith("soundcloud ")) {
								identifier = AudioLoadResultListener.SC_SEARCH + StringUtils.remove(arg, "soundcloud ");
							}
							// ... else if the argument is an URL...
							else if(NetUtils.isValidUrl(arg)) {
								identifier = arg;
							}
							// ...else, search on YouTube
							else {
								identifier = AudioLoadResultListener.YT_SEARCH + arg;
							}

							final GuildMusic guildMusic = GuildMusicManager.getOrCreate(context.getClient(), guildId, voiceChannelId);
							if(guildMusic.isWaitingForChoice()) {
								if(guildMusic.getDjId().equals(context.getAuthorId())) {
									return Mono.error(new CommandException(String.format("You're already selecting a music. "
											+ "Enter a number or use `%scancel` to cancel the selection.", context.getPrefix())));
								}

								if(identifier.startsWith(AudioLoadResultListener.SC_SEARCH) || identifier.startsWith(AudioLoadResultListener.YT_SEARCH)) {
									return context.getClient().getUserById(guildMusic.getDjId())
											.map(User::getUsername)
											.flatMap(djName -> DiscordUtils.sendMessage(String.format(Emoji.HOURGLASS + " (**%s**) **%s** is "
													+ "already selecting a music, please wait for him to finish.",
													context.getUsername(), djName), channel))
											.then();
								}
							}

							if(guildMusic.getTrackScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE - 1
									&& !Shadbot.getPremium().isPremium(guildId, context.getAuthorId())) {
								return DiscordUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, channel).then();
							}

							guildMusic.setMessageChannel(context.getChannelId());

							final boolean putFirst = context.getCommandName().endsWith("first");
							final AudioLoadResultListener resultListener = new AudioLoadResultListener(
									guildId, context.getAuthorId(), identifier, putFirst);

							guildMusic.addAudioLoadResultListener(resultListener, identifier);

							return Mono.empty();
						}));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Play the music(s) from the url, search terms or playlist.")
				.setFullUsage(String.format("%s%s[first] [soundcloud] <url>", context.getPrefix(), this.getName()))
				.addArg("first", "add the song at the top of the playlist", true)
				.addArg("soundcloud", "search on SoundCloud instead of YouTube", true)
				.setExample(String.format("`%splayfirst soundcloud At Doom's gate`"
						+ "%n`%splay E1M8`", context.getPrefix(), context.getPrefix()))
				.build();
	}
}

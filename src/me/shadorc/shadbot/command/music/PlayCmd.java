package me.shadorc.shadbot.command.music;

import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.listener.music.AudioLoadResultListener;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "play", "add", "queue", "playfirst", "addfirst", "queuefirst" }, permissions = { Permission.CONNECT, Permission.SPEAK })
public class PlayCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();
		final Snowflake guildId = context.getGuildId();

		return DiscordUtils.requireSameVoiceChannel(context)
				.flatMap(voiceChannelId -> {
					String identifier;
					if(arg.startsWith("soundcloud ")) {
						identifier = AudioLoadResultListener.SC_SEARCH + StringUtils.remove(arg, "soundcloud ");
					} else if(NetUtils.isValidUrl(arg)) {
						identifier = arg;
					} else {
						identifier = AudioLoadResultListener.YT_SEARCH + arg;
					}

					GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guildId);

					if(guildMusic == null) {
						guildMusic = GuildMusicManager.createGuildMusic(context.getClient(), guildId);
					} else if(guildMusic.isWaitingForChoice()) {
						if(guildMusic.getDjId().equals(context.getAuthorId())) {
							throw new CommandException(String.format("You're already selecting a music. "
									+ "Enter a number or use `%scancel` to cancel the selection.", context.getPrefix()));
						}

						if(identifier.startsWith(AudioLoadResultListener.SC_SEARCH) || identifier.startsWith(AudioLoadResultListener.YT_SEARCH)) {
							return context.getClient().getUserById(guildMusic.getDjId())
									.map(User::getUsername)
									.flatMap(djName -> BotUtils.sendMessage(String.format(Emoji.HOURGLASS + " (**%s**) **%s** is "
											+ "already selecting a music, please wait for him to finish.",
											context.getUsername(), djName), context.getChannel()))
									.then();
						}
					}

					if(guildMusic.getScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE - 1
							&& !PremiumManager.isPremium(guildId, context.getAuthorId())) {
						return BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, context.getChannel()).then();
					}

					guildMusic.setMessageChannel(context.getChannelId());

					final boolean putFirst = context.getCommandName().endsWith("first");
					final AudioLoadResultListener resultListener = new AudioLoadResultListener(
							guildMusic, context.getAuthorId(), voiceChannelId, identifier, putFirst);
					GuildMusicManager.AUDIO_PLAYER_MANAGER.loadItemOrdered(guildMusic, identifier, resultListener);

					return Mono.empty();
				});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
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

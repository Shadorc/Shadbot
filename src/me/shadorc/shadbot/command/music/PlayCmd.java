package me.shadorc.shadbot.command.music;

import java.util.Optional;

import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.listener.music.AudioLoadResultListener;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "play", "add", "queue", "playfirst", "addfirst", "queuefirst" })
public class PlayCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		final String arg = context.requireArg();

		Mono<Optional<Snowflake>> monoAuthorChannelId = context.getGuild()
				.flatMapMany(Guild::getVoiceStates)
				.filter(voiceState -> voiceState.getUserId().equals(context.getAuthorId()))
				.map(VoiceState::getChannelId)
				.single();

		Mono<Optional<Snowflake>> monoSelfChannelId = context.getGuild()
				.flatMapMany(Guild::getVoiceStates)
				.filter(voiceState -> voiceState.getUserId().equals(context.getSelfId()))
				.map(VoiceState::getChannelId)
				.single();

		Mono.zip(monoAuthorChannelId, monoSelfChannelId).subscribe(tuple -> {

			Optional<Snowflake> authorChannelId = tuple.getT1();
			Optional<Snowflake> botChannelId = tuple.getT2();

			// If the bot is in a voice channel and the user is not in a channel or not in the same
			if(botChannelId.isPresent() && !authorChannelId.map(botChannelId.get()::equals).orElse(false)) {
				context.getClient().getVoiceChannelById(botChannelId.get()).subscribe(voiceChannel -> {
					throw new IllegalCmdArgumentException(String.format("I'm currently playing music in voice channel %s"
							+ ", join me before using this command.", voiceChannel.getName())); // TODO: change getName to getMention
				});
			}

			if(!authorChannelId.isPresent()) {
				throw new IllegalCmdArgumentException("Join a voice channel before using this command.");
			}

			/*
			 * TODO: This need to be managed when joining a voice channel if(!botChannelId.isPresent() && !DiscordU.hasPermissions(userVoiceChannel,
			 * Permissions.VOICE_CONNECT, Permissions.VOICE_SPEAK)) { BotUtils.sendMessage(TextUtils.missingPerm(Permissions.VOICE_CONNECT,
			 * Permissions.VOICE_SPEAK), context.getChannel()); LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to connect/speak in a voice channel.",
			 * context.getGuild().getLongID()); return; }
			 */

			final

			String identifier;
			if(arg.startsWith("soundcloud ")) {
				identifier = AudioLoadResultListener.SC_SEARCH + StringUtils.remove(arg, "soundcloud ");
			} else if(NetUtils.isValidURL(arg)) {
				identifier = arg;
			} else {
				identifier = AudioLoadResultListener.YT_SEARCH + arg;
			}

			final Snowflake guildId = context.getGuildId().get();

			GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guildId);

			if(guildMusic == null) {
				guildMusic = GuildMusicManager.createGuildMusic(context.getClient(), guildId);
			} else if(guildMusic.isWaiting()) {
				if(guildMusic.getDjId().equals(context.getAuthorId())) {
					context.getAuthor().map(User::getUsername).subscribe(username -> {
						throw new IllegalCmdArgumentException(String.format("(**%s**) You're already selecting a music. "
								+ "Enter a number or use `%scancel` to cancel the selection.",
								username, context.getPrefix()));
					});
				}

				if(identifier.startsWith(AudioLoadResultListener.SC_SEARCH) || identifier.startsWith(AudioLoadResultListener.YT_SEARCH)) {
					context.getClient().getUserById(guildMusic.getDjId()).map(User::getUsername).subscribe(username -> {
						BotUtils.sendMessage(String.format(Emoji.HOURGLASS + " **%s** is already selecting a music, please wait for him to finish.",
								username), context.getChannel());
					});
					return;
				}
			}

			if(guildMusic.getScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE - 1
					&& !PremiumManager.isPremium(guildId, context.getAuthorId())) {
				BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, context.getChannel());
				return;
			}

			guildMusic.setChannel(context.getChannelId());

			final boolean putFirst = context.getCommandName().endsWith("first");
			AudioLoadResultListener resultListener = new AudioLoadResultListener(
					guildMusic, context.getAuthorId(), authorChannelId.get(), identifier, putFirst);
			GuildMusicManager.AUDIO_PLAYER_MANAGER.loadItemOrdered(guildMusic, identifier, resultListener);
		});
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Play the music(s) from the url, search terms or playlist.")
				.setFullUsage(String.format("%s%s[first] [soundcloud] <url>", prefix, this.getName()))
				.addArg("first", "add the song at the top of the playlist", true)
				.addArg("soundcloud", "search on SoundCloud instead of YouTube", true)
				.setExample(String.format("`%splayfirst soundcloud At Doom's gate`"
						+ "%n`%splay E1M8`", prefix, prefix))
				.build();
	}
}

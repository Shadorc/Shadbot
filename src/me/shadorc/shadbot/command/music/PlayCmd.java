package me.shadorc.shadbot.command.music;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.listener.music.AudioLoadResultListener;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "play", "add", "queue", "playfirst", "addfirst", "queuefirst" })
public class PlayCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		IVoiceChannel botVoiceChannel = context.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		IVoiceChannel userVoiceChannel = context.getAuthor().getVoiceStateForGuild(context.getGuild()).getChannel();

		if(botVoiceChannel != null && !botVoiceChannel.equals(userVoiceChannel)) {
			throw new IllegalArgumentException(String.format("I'm currently playing music in voice channel %s"
					+ ", join me before using this command.", botVoiceChannel.mention()));
		}

		if(userVoiceChannel == null) {
			throw new IllegalArgumentException("Join a voice channel before using this command.");
		}

		if(botVoiceChannel == null && !BotUtils.hasPermissions(userVoiceChannel, Permissions.VOICE_CONNECT, Permissions.VOICE_SPEAK)) {
			BotUtils.sendMessage(TextUtils.missingPerm(Permissions.VOICE_CONNECT, Permissions.VOICE_SPEAK), context.getChannel());
			LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to connect/speak in a voice channel.", context.getGuild().getLongID());
			return;
		}

		String identifier;
		if(context.getArg().startsWith("soundcloud ")) {
			identifier = AudioLoadResultListener.SC_SEARCH + StringUtils.remove(context.getArg(), "soundcloud ");
		} else if(NetUtils.isValidURL(context.getArg())) {
			identifier = context.getArg();
		} else {
			identifier = AudioLoadResultListener.YT_SEARCH + context.getArg();
		}

		GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(context.getGuild().getLongID());

		if(guildMusic != null && guildMusic.isWaiting()) {
			if(guildMusic.getDj().equals(context.getAuthor())) {
				throw new IllegalArgumentException(String.format("(**%s**) You're already selecting a music. "
						+ "Enter a number or use `%scancel` to cancel the selection.", context.getAuthorName(), context.getPrefix()));
			}

			if(identifier.startsWith(AudioLoadResultListener.SC_SEARCH) || identifier.startsWith(AudioLoadResultListener.YT_SEARCH)) {
				BotUtils.sendMessage(String.format(Emoji.HOURGLASS + " **%s** is already selecting a music, please wait for him to finish.",
						guildMusic.getDj().getName()), context.getChannel());
				return;
			}
		}

		if(guildMusic == null) {
			guildMusic = GuildMusicManager.createGuildMusic(context.getGuild());
		}

		if(guildMusic.getScheduler().getPlaylist().size() >= Config.MAX_PLAYLIST_SIZE - 1
				&& !PremiumManager.isGuildPremium(context.getGuild())
				&& !PremiumManager.isUserPremium(context.getAuthor())) {
			BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, context.getChannel());
			return;
		}

		guildMusic.setChannel(context.getChannel());

		AudioLoadResultListener resultListener = new AudioLoadResultListener(
				guildMusic, context.getAuthor(), userVoiceChannel, identifier, context.getCommandName().endsWith("first"));
		GuildMusicManager.PLAYER_MANAGER.loadItemOrdered(guildMusic, identifier, resultListener);
	}

	@Override
	public EmbedObject getHelp(String prefix) {
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

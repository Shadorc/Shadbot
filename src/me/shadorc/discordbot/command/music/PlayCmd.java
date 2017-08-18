package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.events.AudioLoadResultListener;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.NetUtils;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;

public class PlayCmd extends AbstractCommand {

	public PlayCmd() {
		super(Role.USER, "play", "add");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		IVoiceChannel botVoiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		IVoiceChannel userVoiceChannel = context.getAuthor().getVoiceStateForGuild(context.getGuild()).getChannel();
		if(userVoiceChannel == null) {
			if(botVoiceChannel == null) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Join a vocal channel before using this command.", context.getChannel());
			} else {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Shadbot is currently playing music in voice channel " + botVoiceChannel.mention()
						+ ", join him before using this command.", context.getChannel());
			}
			return;
		}

		if(botVoiceChannel != null && !botVoiceChannel.equals(userVoiceChannel)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Shadbot is currently playing music in voice channel " + botVoiceChannel.mention()
					+ ", join him before using this command.", context.getChannel());
			return;
		}

		final StringBuilder identifier = new StringBuilder();
		if(context.getArg().startsWith("soundcloud ")) {
			identifier.append(AudioLoadResultListener.SC_SEARCH);
			identifier.append(context.getArg().replace("soundcloud ", ""));
		} else if(NetUtils.isValidURL(context.getArg())) {
			identifier.append(context.getArg());
		} else {
			identifier.append(AudioLoadResultListener.YT_SEARCH);
			identifier.append(context.getArg());
		}

		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		musicManager.setChannel(context.getChannel());
		AudioLoadResultListener resultListener = new AudioLoadResultListener(identifier.toString(), botVoiceChannel, userVoiceChannel, musicManager);
		GuildMusicManager.PLAYER_MANAGER.loadItemOrdered(musicManager, identifier.toString(), resultListener);
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Play the music(s) from the url, search terms or playlist.\nYou can also search on SoundCloud by using /play soundcloud <search>**")
				.appendField("Usage", context.getPrefix() + "play <url>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}

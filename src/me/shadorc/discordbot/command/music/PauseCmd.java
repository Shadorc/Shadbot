package me.shadorc.discordbot.command.music;

import java.awt.Color;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class PauseCmd extends Command {

	public PauseCmd() {
		super(false, "pause");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(Emoji.WARNING + " Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		scheduler.setPaused(!scheduler.isPaused());
		if(scheduler.isPaused()) {
			BotUtils.sendMessage(Emoji.PAUSE + " Musique mise en pause par " + context.getAuthorName() + ".", context.getChannel());
		} else {
			BotUtils.sendMessage(Emoji.PLAY + " Reprise de la musique à la demande de " + context.getAuthorName() + ".", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Met en pause la musique en cours. Réutilisez cette commande pour désactiver la pause.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
package me.shadorc.discordbot.command.music;

import java.awt.Color;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class VolumeCmd extends Command {

	public VolumeCmd() {
		super(false, "volume");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(Emoji.WARNING + " Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		if(context.getArg() == null) {
			BotUtils.sendMessage(Emoji.WARNING + " Merci d'indiquer un volume compris entre 0 et 100.", context.getChannel());
			return;
		}

		try {
			scheduler.setVolume(Integer.parseInt(context.getArg()));
			BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Volume de la musique réglé sur " + scheduler.getVolume() + "%", context.getChannel());
		} catch (NumberFormatException e) {
			BotUtils.sendMessage(Emoji.WARNING + " Merci d'indiquer un volume compris entre 0 et 100.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Change le volume des musiques.**")
				.appendField("Utilisation", "/volume <0-100>", false);

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}

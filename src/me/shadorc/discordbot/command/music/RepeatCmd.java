package me.shadorc.discordbot.command.music;

import java.awt.Color;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class RepeatCmd extends Command {

	public RepeatCmd() {
		super(false, "repeat");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(Emoji.WARNING + " Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		scheduler.setRepeatEnabled(!scheduler.isRepeating());
		if(scheduler.isRepeating()) {
			BotUtils.sendMessage(Emoji.REPEAT + " Répétition de la musique activée.", context.getChannel());
		} else {
			BotUtils.sendMessage(Emoji.PLAY + " Répétition de la musique désactivée.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Active la répétition de la musique en cours. Réutilisez cette commande pour désactiver la répétition.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}

package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class RepeatCmd extends AbstractCommand {

	public RepeatCmd() {
		super(Role.USER, "repeat");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null) {
			BotUtils.sendMessage(Emoji.MUTE + " No currently playing music.", context.getChannel());
			return;
		}

		TrackScheduler scheduler = musicManager.getScheduler();
		scheduler.setRepeatEnabled(!scheduler.isRepeating());
		if(scheduler.isRepeating()) {
			BotUtils.sendMessage(Emoji.REPEAT + " Repetition enabled.", context.getChannel());
		} else {
			BotUtils.sendMessage(Emoji.PLAY + " Repetition disabled.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Enable/disable music repetition.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}

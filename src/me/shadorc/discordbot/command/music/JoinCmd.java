package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

public class JoinCmd extends AbstractCommand {

	public JoinCmd() {
		super(Role.USER, "join");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null || musicManager.getScheduler().isStopped()) {
			BotUtils.sendMessage(Emoji.MUTE + " No currently playing music.", context.getChannel());
			return;
		}

		if(!BotUtils.hasPermission(context.getChannel(), Permissions.VOICE_MOVE_MEMBERS)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I can't move you to the voice channel due to the lack of permission."
					+ "\nPlease, check my permissions and channel-specific ones to verify that **Move members** is checked.", context.getChannel());
			LogUtils.info("{Guild: " + context.getChannel().getGuild().getName() + " (ID: " + context.getChannel().getGuild().getStringID() + ")} "
					+ "Shadbot wasn't allowed to move member in a voice channel.");
			return;
		}

		context.getAuthor().moveToVoiceChannel(Shadbot.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel());
		BotUtils.sendMessage(Emoji.CHECK_MARK + " You've been moved to the music voice channel.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Join the music voice channel if Shadbot is currently playing music.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}

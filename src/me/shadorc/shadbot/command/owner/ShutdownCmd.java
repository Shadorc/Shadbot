package me.shadorc.shadbot.command.owner;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.ThreadPoolUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IGuild;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "shutdown" })
public class ShutdownCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		ScheduledExecutorService executor =
				Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.getThreadFactoryNamed("Shadbot-ShutdownCmd"));

		if(!context.hasArg()) {
			// TODO: Ask for confirmation
			executor.submit(() -> System.exit(0));
			return;
		}

		List<String> splitArgs = StringUtils.split(context.getArg(), 2);
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Integer delay = CastUtils.asPositiveInt(splitArgs.get(0));
		if(delay == null) {
			throw new IllegalArgumentException("Invalid time.");
		}

		String message = splitArgs.get(1);
		for(IGuild guild : context.getClient().getGuilds()) {
			GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(guild);
			if(musicManager != null && musicManager.getChannel() != null) {
				BotUtils.sendMessage(Emoji.INFO + " " + message, musicManager.getChannel());
			}
		}

		executor.schedule(() -> System.exit(0), delay, TimeUnit.SECONDS);

		LogUtils.warnf("Shadbot will restart in %d seconds. (Message: %s)", delay, message);
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Schedule a shutdown after a fixed amount of seconds and send a message to all guilds playing musics.")
				.addArg("seconds", true)
				.addArg("message", true)
				.build();
	}

}

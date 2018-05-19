package me.shadorc.shadbot.command.owner;

import java.util.List;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.message.MessageListener;
import me.shadorc.shadbot.message.MessageManager;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "shutdown" })
public class ShutdownCmd extends AbstractCommand implements MessageListener {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			MessageManager.addListener(context.getChannel(), this);
			BotUtils.sendMessage(String.format(Emoji.QUESTION + " Do you really want to shutdown %s ? Yes/No",
					context.getClient().getSelf().mention()), context.getChannel());
			return;
		}

		List<String> splitArgs = StringUtils.split(context.getArg(), 2);
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Integer delay = NumberUtils.asPositiveInt(splitArgs.get(0));
		if(delay == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid time.", splitArgs.get(0)));
		}

		String message = splitArgs.get(1);
		for(IGuild guild : context.getClient().getGuilds()) {
			GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guild.getLongID());
			if(guildMusic != null && guildMusic.getChannel() != null) {
				BotUtils.sendMessage(Emoji.INFO + " " + message, guildMusic.getChannel());
			}
		}

		Shadbot.getScheduler().schedule(() -> System.exit(0), delay, TimeUnit.SECONDS);

		LogUtils.warnf("Shadbot will restart in %d seconds. (Message: %s)", delay, message);
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Schedule a shutdown after a fixed amount of seconds and send a message to all guilds playing musics.")
				.addArg("seconds", true)
				.addArg("message", true)
				.build();
	}

	@Override
	public boolean intercept(IMessage message) {
		if(!message.getAuthor().equals(message.getClient().getApplicationOwner())) {
			return false;
		}

		String content = message.getContent().toLowerCase();
		if("yes".equalsIgnoreCase(content) || "y".equalsIgnoreCase(content)) {
			Shadbot.getScheduler().submit(() -> System.exit(0));
			return true;
		} else if("no".equalsIgnoreCase(content) || "n".equalsIgnoreCase(content)) {
			MessageManager.removeListener(message.getChannel(), this);
			BotUtils.sendMessage(Emoji.INFO + " Shutdown cancelled.", message.getChannel());
			return true;
		}

		return false;
	}

}

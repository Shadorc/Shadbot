package me.shadorc.shadbot.command.owner;

import java.util.List;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "shutdown" })
public class ShutdownCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		if(!context.getArg().isPresent()) {
			Shadbot.logout();
			return Mono.empty();
		}

		List<String> args = context.requireArgs(2);

		final Integer delay = NumberUtils.asPositiveInt(args.get(0));
		if(delay == null) {
			throw new CommandException(String.format("`%s` is not a valid time.", args.get(0)));
		}

		final String message = args.get(1);

		return Flux.fromIterable(GuildMusicManager.GUILD_MUSIC_MAP.values())
				.flatMap(guildMusic -> BotUtils.sendMessage(Emoji.INFO + " " + message, guildMusic.getMessageChannel()))
				.then(context.getSelf())
				.map(User::getMention)
				.doOnSuccess(botName -> LogUtils.warn(context.getClient(),
						String.format("%s will restart in %d seconds. (Message: %s)", botName, delay, message)))
				.doOnTerminate(() -> Shadbot.getScheduler().schedule(Shadbot::logout, delay, TimeUnit.SECONDS))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Schedule a shutdown after a fixed amount of seconds and send a message to all guilds playing musics.")
				.addArg("seconds", true)
				.addArg("message", true)
				.build();
	}

}

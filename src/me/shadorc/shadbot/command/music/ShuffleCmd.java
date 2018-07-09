package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "shuffle" })
public class ShuffleCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final GuildMusic guildMusic = context.requireGuildMusic();
		guildMusic.getScheduler().shufflePlaylist();

		return context.getAuthorName()
				.flatMap(username -> BotUtils.sendMessage(
						String.format(Emoji.CHECK_MARK + " Playlist shuffled by **%s**.", username),
						context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Shuffle current playlist.")
				.build();
	}

}

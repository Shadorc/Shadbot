package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "clear" })
public class ClearCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		GuildMusic guildMusic = context.requireGuildMusic();
		guildMusic.getScheduler().clearPlaylist();
		BotUtils.sendMessage(Emoji.CHECK_MARK + " Playlist cleared.", context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Clear current playlist.")
				.build();
	}

}

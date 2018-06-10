package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "name", "current", "np" })
public class NameCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		GuildMusic guildMusic = context.requireGuildMusic();
		BotUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " Currently playing: **%s**",
				FormatUtils.formatTrackName(guildMusic.getScheduler().getAudioPlayer().getPlayingTrack().getInfo())), context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show current music name.")
				.build();
	}
}
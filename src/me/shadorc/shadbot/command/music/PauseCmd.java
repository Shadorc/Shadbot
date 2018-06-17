package me.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import discord4j.core.object.entity.User;
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
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "pause", "unpause", "resume" })
public class PauseCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		GuildMusic guildMusic = context.requireGuildMusic();
		AudioPlayer audioPlayer = guildMusic.getScheduler().getAudioPlayer();
		audioPlayer.setPaused(!audioPlayer.isPaused());
		context.getAuthor().map(User::getUsername).subscribe(username -> {
			if(audioPlayer.isPaused()) {
				BotUtils.sendMessage(String.format(Emoji.PAUSE + " Music paused by **%s**.",
						username), context.getChannel());
			} else {
				BotUtils.sendMessage(String.format(Emoji.PLAY + " Music resumed by **%s**.",
						username), context.getChannel());
			}
		});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Pause current music. Use this command again to resume.")
				.build();
	}
}
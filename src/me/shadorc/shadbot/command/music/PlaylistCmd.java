// TODO: Implement
// package me.shadorc.shadbot.command.music;
//
// import java.util.concurrent.BlockingQueue;
//
// import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
//
// import discord4j.core.spec.EmbedCreateSpec;
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.core.command.annotation.RateLimited;
// import me.shadorc.shadbot.music.GuildMusic;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.FormatUtils;
// import me.shadorc.shadbot.utils.StringUtils;
// import me.shadorc.shadbot.utils.embed.EmbedUtils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
// import reactor.core.publisher.Mono;
//
// @RateLimited
// @Command(category = CommandCategory.MUSIC, names = { "playlist" })
// public class PlaylistCmd extends AbstractCommand {
//
// @Override
// public Mono<Void> execute(Context context) {
// final GuildMusic guildMusic = context.requireGuildMusic();
//
// return context.getAvatarUrl()
// .map(avatarUrl -> EmbedUtils.getDefaultEmbed()
// .setAuthor("Playlist", null, avatarUrl)
// .setThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
// .setDescription(this.formatPlaylist(guildMusic.getScheduler().getPlaylist())))
// .flatMap(embed -> BotUtils.sendMessage(embed, context.getChannel()))
// .then();
// }
//
// private String formatPlaylist(BlockingQueue<AudioTrack> queue) {
// if(queue.isEmpty()) {
// return "**The playlist is empty.**";
// }
//
// final StringBuilder playlist = new StringBuilder(String.format("**%s in the playlist:**\n", StringUtils.pluralOf(queue.size(), "music")));
//
// int count = 1;
// for(AudioTrack track : queue) {
// final String name = String.format("%n\t**%d.** %s", count, FormatUtils.trackName(track.getInfo()));
// if(playlist.length() + name.length() < 1800) {
// playlist.append(name);
// } else {
// playlist.append("\n\t...");
// break;
// }
// count++;
// }
// return playlist.toString();
// }
//
// @Override
// public Mono<EmbedCreateSpec> getHelp(Context context) {
// return new HelpBuilder(this, context)
// .setDescription("Show current playlist.")
// .build();
// }
// }
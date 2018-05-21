// TODO
// package me.shadorc.shadbot.command.music;
//
// import java.util.concurrent.BlockingQueue;
//
// import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
//
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.core.command.annotation.RateLimited;
// import me.shadorc.shadbot.exception.MissingArgumentException;
// import me.shadorc.shadbot.music.GuildMusic;
// import me.shadorc.shadbot.music.GuildMusicManager;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.FormatUtils;
// import me.shadorc.shadbot.utils.StringUtils;
// import me.shadorc.shadbot.utils.TextUtils;
// import me.shadorc.shadbot.utils.embed.EmbedUtils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
//
// @RateLimited
// @Command(category = CommandCategory.MUSIC, names = { "playlist" })
// public class PlaylistCmd extends AbstractCommand {
//
// @Override
// public void execute(Context context) throws MissingArgumentException {
// GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(context.getGuild().getLongID());
//
// if(guildMusic == null || guildMusic.getScheduler().isStopped()) {
// BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
// return;
// }
//
// EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
// .withAuthorName("Playlist")
// .withThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
// .appendDescription(this.formatPlaylist(guildMusic.getScheduler().getPlaylist()));
// BotUtils.sendMessage(embed.build(), context.getChannel());
// }
//
// private String formatPlaylist(BlockingQueue<AudioTrack> queue) {
// if(queue.isEmpty()) {
// return "**The playlist is empty.**";
// }
//
// StringBuilder playlist = new StringBuilder(String.format("**%s in the playlist:**\n", StringUtils.pluralOf(queue.size(), "music")));
//
// int count = 1;
// for(AudioTrack track : queue) {
// String name = String.format("%n\t**%d.** %s", count, FormatUtils.formatTrackName(track.getInfo()));
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
// public EmbedObject getHelp(String prefix) {
// return new HelpBuilder(this, prefix)
// .setDescription("Show current playlist.")
// .build();
// }
// }
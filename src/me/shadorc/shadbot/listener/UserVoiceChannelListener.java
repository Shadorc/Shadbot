// TODO
// package me.shadorc.shadbot.listener;
//
// import me.shadorc.shadbot.music.GuildMusic;
// import me.shadorc.shadbot.music.GuildMusicManager;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.object.Emoji;
//
// public class UserVoiceChannelListener {
//
// public static void onUserVoiceChannelJoinEvent(UserVoiceChannelJoinEvent event) {
// this.check(event.getGuild());
// }
//
// private void onUserVoiceChannelLeaveEvent(UserVoiceChannelLeaveEvent event) {
// this.check(event.getGuild());
// }
//
// private void onUserVoiceChannelMoveEvent(UserVoiceChannelMoveEvent event) {
// this.check(event.getGuild());
// }
//
// private synchronized void check(IGuild guild) {
// IVoiceChannel botVoiceChannel = guild.getClient().getSelf().getVoiceStateForGuild(guild).getChannel();
// if(botVoiceChannel == null) {
// return;
// }
//
// GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guild.getLongID());
// if(guildMusic == null) {
// return;
// }
//
// if(this.isAlone(botVoiceChannel) && !guildMusic.isLeavingScheduled()) {
// BotUtils.sendMessage(Emoji.INFO + " Nobody is listening anymore, music paused. I will leave the voice channel in 1 minute.",
// guildMusic.getChannel());
// guildMusic.getScheduler().getAudioPlayer().setPaused(true);
// guildMusic.scheduleLeave();
//
// } else if(!this.isAlone(botVoiceChannel) && guildMusic.isLeavingScheduled()) {
// BotUtils.sendMessage(Emoji.INFO + " Somebody joined me, music resumed.", guildMusic.getChannel());
// guildMusic.getScheduler().getAudioPlayer().setPaused(false);
// guildMusic.cancelLeave();
// }
// }
//
// private boolean isAlone(IVoiceChannel voiceChannel) {
// return voiceChannel.getConnectedUsers().stream().filter(user -> !user.isBot()).count() == 0;
// }
//
// }

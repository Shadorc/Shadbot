// TODO
// package me.shadorc.shadbot.command.owner;
//
// import java.util.List;
// import java.util.Optional;
// import java.util.concurrent.TimeUnit;
//
// import discord4j.core.event.domain.message.MessageCreateEvent;
// import discord4j.core.object.entity.Guild;
// import discord4j.core.object.entity.User;
// import discord4j.core.spec.EmbedCreateSpec;
// import me.shadorc.shadbot.Shadbot;
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.CommandPermission;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.exception.CommandException;
// import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
// import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
// import me.shadorc.shadbot.music.GuildMusic;
// import me.shadorc.shadbot.music.GuildMusicManager;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.DiscordUtils;
// import me.shadorc.shadbot.utils.NumberUtils;
// import me.shadorc.shadbot.utils.SchedulerUtils;
// import me.shadorc.shadbot.utils.command.Emoji;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
// import me.shadorc.shadbot.utils.embed.log.LogUtils;
// import reactor.core.publisher.Flux;
// import reactor.core.publisher.Mono;
//
// @Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "shutdown" })
// public class ShutdownCmd extends AbstractCommand implements MessageInterceptor {
//
// @Override
// public Mono<Void> execute(Context context) {
// if(!context.getArg().isPresent()) {
// MessageInterceptorManager.addInterceptor(context.getChannelId(), this);
// return context.getSelf()
// .map(User::getMention)
// .flatMap(botName -> BotUtils.sendMessage(String.format(Emoji.QUESTION + " Do you really want to shutdown %s ? Yes/No", botName),
// context.getChannel()))
// .then();
// }
//
// List<String> args = context.requireArgs(2);
//
// final Integer delay = NumberUtils.asPositiveInt(args.get(0));
// if(delay == null) {
// throw new CommandException(String.format("`%s` is not a valid time.", args.get(0)));
// }
//
// final String message = args.get(1);
//
// return Flux.fromIterable(GuildMusicManager.GUILD_MUSIC_MAP.values())
// .flatMap(guildMusic -> BotUtils.sendMessage(Emoji.INFO + " " + message, guildMusic.getMessageChannel()))
// .then(context.getSelf())
// .map(User::getMention)
// .doOnSuccess(botName -> LogUtils.warn(context.getClient(),
// String.format("%s will restart in %d seconds. (Message: %s)", botName, delay, message)))
// .doOnTerminate(() -> SchedulerUtils.schedule(() -> Shadbot.logout(), delay, TimeUnit.SECONDS))
// .then();
// }
//
// @Override
// public Mono<EmbedCreateSpec> getHelp(Context context) {
// return new HelpBuilder(this, context)
// .setDescription("Schedule a shutdown after a fixed amount of seconds and send a message to all guilds playing musics.")
// .addArg("seconds", true)
// .addArg("message", true)
// .build();
// }
//
// @Override
// public boolean isIntercepted(MessageCreateEvent event) {
// if(!event.getAuthor().equals(message.getClient().getApplicationOwner())) {
// return false;
// }
//
// String content = message.getContent().toLowerCase();
// if("yes".equalsIgnoreCase(content) || "y".equalsIgnoreCase(content)) {
// Shadbot.getScheduler().submit(() -> System.exit(0));
// return true;
// } else if("no".equalsIgnoreCase(content) || "n".equalsIgnoreCase(content)) {
// MessageInterceptorManager.removeInterceptor(message.getChannel(), this);
// BotUtils.sendMessage(Emoji.INFO + " Shutdown cancelled.", message.getChannel());
// return true;
// }
//
// return false;
// }
//
// }

 package me.shadorc.shadbot.command.hidden;

 import java.util.stream.Collectors;

 import discord4j.core.spec.EmbedCreateSpec;
 import me.shadorc.shadbot.Config;
 import me.shadorc.shadbot.core.command.AbstractCommand;
 import me.shadorc.shadbot.core.command.CommandCategory;
 import me.shadorc.shadbot.core.command.CommandManager;
 import me.shadorc.shadbot.core.command.Context;
 import me.shadorc.shadbot.core.command.annotation.Command;
 import me.shadorc.shadbot.core.command.annotation.RateLimited;
 import me.shadorc.shadbot.data.stats.CommandStatsManager;
 import me.shadorc.shadbot.data.stats.CommandStatsManager.CommandEnum;
 import me.shadorc.shadbot.utils.BotUtils;
 import me.shadorc.shadbot.utils.embed.EmbedUtils;
 import me.shadorc.shadbot.utils.embed.HelpBuilder;
 import reactor.core.publisher.Flux;
 import reactor.core.publisher.Mono;

 @RateLimited
 @Command(category = CommandCategory.HIDDEN, names = { "help" })
 public class HelpCmd extends AbstractCommand {

 @Override
 public Mono<Void> execute(Context context) {
 if(context.getArg().isPresent()) {
 AbstractCommand cmd = CommandManager.getCommand(context.getArg().get());
 if(cmd == null) {
 return Mono.empty();
 }

 CommandStatsManager.log(CommandEnum.COMMAND_HELPED, cmd);
 return cmd.getHelp(context)
 .flatMap(embed -> BotUtils.sendMessage(embed, context.getChannel()))
 .then();
 }

 return context.getAuthorAvatarUrl()
 .map(avatarUrl -> EmbedUtils.getDefaultEmbed()
 .setAuthor("Shadbot Help", null, avatarUrl)
 .setDescription(String.format("Any issues, questions or suggestions ?"
 + " Join the [support server.](%s)"
 + "%nGet more information by using `%s%s <command>`.",
 Config.SUPPORT_SERVER_URL, context.getPrefix(), this.getName())))
 .flatMap(embed -> {
 return context.getAuthorPermission()
 .flatMap(authorPerm -> {
 for(CommandCategory category : CommandCategory.values()) {
 if(category.equals(CommandCategory.HIDDEN)) {
 continue;
 }

 Flux.fromIterable(CommandManager.getCommands().values())
 .distinct()
 .filter(cmd -> cmd.getCategory().equals(category))
 .filter(cmd -> !cmd.getPermission().isSuperior(authorPerm))
 .filter(cmd -> !context.getGuildId().isPresent() || BotUtils.isCommandAllowed(context.getGuildId().get(), cmd))
 .map(AbstractCommand::getName)
 .map(cmdName -> String.format("`%s%s`", context.getPrefix(), cmdName))
 .collect(Collectors.joining(" "))
 .filter(commands -> !commands.isEmpty())
 .subscribe(commands -> embed.addField(String.format("%s Commands", category.toString()), commands, false));
 }

 return BotUtils.sendMessage(embed, context.getChannel());
 });
 })
 .then();
 }

 @Override
 public Mono<EmbedCreateSpec> getHelp(Context context) {
 return new HelpBuilder(this, context)
 .setDescription("Show the list of available commands.")
 .build();
 }

 }

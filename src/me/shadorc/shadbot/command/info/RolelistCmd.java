// package me.shadorc.shadbot.command.info;
//
// import java.util.List;
// import java.util.stream.Collectors;
//
// import discord4j.core.object.entity.Role;
// import discord4j.core.object.entity.User;
// import discord4j.core.spec.EmbedCreateSpec;
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.core.command.annotation.RateLimited;
// import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
// import me.shadorc.shadbot.exception.MissingArgumentException;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.FormatUtils;
// import me.shadorc.shadbot.utils.embed.EmbedUtils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
// import reactor.core.publisher.Flux;
//
// @RateLimited
// @Command(category = CommandCategory.INFO, names = { "rolelist" })
// public class RolelistCmd extends AbstractCommand {
//
// @Override
// public void execute(Context context) {
// Flux<Role> roles = context.getMessage().getRoleMentions();
// if(!roles.hasElements().block()) {
// throw new MissingArgumentException();
// }
//
// List<IUser> users = roles.stream()
// .flatMap(role -> context.getGuild().getUsersByRole(role).stream())
// .distinct()
// .collect(Collectors.toList());
//
// // Only keep elements common to all users list
// roles.stream().forEach(role -> users.retainAll(context.getGuild().getUsersByRole(role)));
//
// EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed("Role list")
// .withDescription(String.format("Members with role(s) **%s**", FormatUtils.format(roles, IRole::getName, ", ")));
//
// if(users.isEmpty()) {
// embed.appendDescription(String.format("There is nobody with %s.", roles.size() == 1 ? "this role" : "these roles"));
// } else {
// FormatUtils.createColumns(users.stream().map(User::getUsername).collect(Collectors.toList()), 25)
// .stream()
// .forEach(embed::appendField);
// }
//
// BotUtils.sendMessage(embed.build(), context.getChannel());
// }
//
// @Override
// public EmbedObject getHelp(String prefix) {
// return new HelpBuilder(this, context)
// .setDescription("Show a list of members with specific role(s).")
// .addArg("@role(s)", false)
// .build();
// }
//
// }

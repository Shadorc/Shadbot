package me.shadorc.shadbot.command.info;

import java.util.List;
import java.util.stream.Collectors;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "rolelist" })
public class RolelistCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		List<IRole> roles = context.getMessage().getRoleMentions();
		if(roles.isEmpty()) {
			throw new MissingArgumentException();
		}

		List<IUser> users = roles.stream()
				.flatMap(role -> context.getGuild().getUsersByRole(role).stream())
				.distinct()
				.collect(Collectors.toList());

		// Only keep elements common to all users list
		roles.stream().forEach(role -> users.retainAll(context.getGuild().getUsersByRole(role)));

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Role list")
				.withDescription(String.format("Members with role(s) **%s**", FormatUtils.format(roles, IRole::getName, ", ")));

		if(users.isEmpty()) {
			embed.appendDescription(String.format("There is nobody with %s.", roles.size() == 1 ? "this role" : "these roles"));
		} else {
			FormatUtils.createColumns(users.stream().map(IUser::getName).collect(Collectors.toList()), 25)
					.stream()
					.forEach(embed::appendField);
		}

		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show a list of members with specific role(s).")
				.addArg("@role(s)", false)
				.build();
	}

}

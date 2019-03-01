package me.shadorc.shadbot.command.hidden;

import java.util.function.Consumer;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.HIDDEN, names = { "help" })
public class HelpCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		if(context.getArg().isPresent()) {
			final AbstractCommand cmd = CommandInitializer.getCommand(context.getArg().get());
			if(cmd == null) {
				return Mono.empty();
			}

			StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_HELPED, cmd);
			return context.getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(cmd.getHelp(context), channel))
					.then();
		}

		return context.getPermission()
				.flatMap(authorPerm -> Flux.fromIterable(CommandInitializer.getCommands().values())
						.distinct()
						.filter(cmd -> !cmd.getPermission().isSuperior(authorPerm))
						.filterWhen(cmd -> context.getChannel().map(Channel::getType)
								.map(type -> type.equals(Type.DM) || Shadbot.getDatabase().getDBGuild(context.getGuildId()).isCommandAllowed(cmd)))
						.collectMultimap(AbstractCommand::getCategory, cmd -> String.format("`%s%s`", context.getPrefix(), cmd.getName())))
				.map(map -> EmbedUtils.getDefaultEmbed()
						.andThen(embed -> {
							embed.setAuthor("Shadbot Help", null, context.getAvatarUrl())
									.setDescription(String.format("Any issues, questions or suggestions ?"
											+ " Join the [support server.](%s)"
											+ "%nGet more information by using `%s%s <command>`.",
											Config.SUPPORT_SERVER_URL, context.getPrefix(), this.getName()));

							for(final CommandCategory category : CommandCategory.values()) {
								if(map.get(category) != null && !map.get(category).isEmpty() && !category.equals(CommandCategory.HIDDEN)) {
									embed.addField(String.format("%s Commands", category.toString()), String.join(" ", map.get(category)), false);
								}
							}
						}))
				.flatMap(embedConsumer -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel)))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show the list of available commands.")
				.build();
	}

}
